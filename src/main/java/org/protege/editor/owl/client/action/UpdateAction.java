package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.api.exception.OWLServerException;
import org.protege.editor.owl.server.versioning.CollectingChangeVisitor;
import org.protege.editor.owl.server.versioning.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AnnotationChange;
import org.semanticweb.owlapi.model.ImportChange;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.UnloadableImportException;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UpdateAction extends AbstractClientAction {

    private static final long serialVersionUID = 2694484296709954780L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            final VersionedOWLOntology vont = getActiveVersionedOntology();
            Future<?> task = submit(new DoUpdate(vont));
            @SuppressWarnings("unchecked")
            List<OWLOntologyChange> incomingChanges = (List<OWLOntologyChange>) task.get();
            if (incomingChanges.isEmpty()) {
                showSynchronizationInfoDialog("Local copy is already up-to-date");
            }
            else {
                String template = "Local copy is succesfully updated by %d changes";
                showSynchronizationInfoDialog(String.format(template, incomingChanges.size()));
            }
        }
        catch (SynchronizationException e) {
            showSynchronizationErrorDialog(e.getMessage(), e);
            // TODO: Implement conflict resolution module
        }
        catch (InterruptedException | ExecutionException e) {
            showSynchronizationErrorDialog("Internal error while updating changes", e);
        }
    }

    private class DoUpdate implements Callable<List<OWLOntologyChange>> {
        
        private VersionedOWLOntology vont;
        private OWLOntology ontology;

        public DoUpdate(VersionedOWLOntology vont) {
            this.vont = vont;
            ontology = vont.getOntology();
        }

        @Override
        public List<OWLOntologyChange> call() throws SynchronizationException {
            List<OWLOntologyChange> incomingChanges = new ArrayList<>();
            if (!isUpdated()) {
                List<OWLOntologyChange> localChanges = getLatestChangesFromClient();
                List<OWLOntologyChange> remoteChanges = getLatestChangesFromServer();
                List<OWLOntologyChange> conflictChanges = getConflicts(localChanges, remoteChanges);
                if (conflictChanges.isEmpty()) {
                    performUpdate(remoteChanges);
                    incomingChanges = remoteChanges;
                }
                throw new SynchronizationException("Conflict was detected and unable to merge the remote changes");
            }
            return incomingChanges;
        }

        private void performUpdate(List<OWLOntologyChange> updates) {
            ontology.getOWLOntologyManager().applyChanges(updates);
            adjustImports(updates);
        }

        private boolean isUpdated() {
            try {
                DocumentRevision remoteHead = ChangeUtils.getRemoteHeadRevision(vont);
                DocumentRevision localHead = vont.getRevision();
                return remoteHead.getRevisionDifferenceFrom(localHead) == 0;
            }
            catch (OWLServerException e) {
                showSynchronizationErrorDialog("Error while computing the remote head revision", e);
                return false;
            }
        }

        public List<OWLOntologyChange> getLatestChangesFromClient() {
            List<OWLOntologyChange> changes = new ArrayList<>();
            try {
                changes = ChangeUtils.getUncommittedChanges(vont);
            }
            catch (OWLServerException e) {
                showSynchronizationErrorDialog("Error while fetching the latest changes from client", e);
            }
            return changes;
        }

        private List<OWLOntologyChange> getLatestChangesFromServer() {
            List<OWLOntologyChange> changes = new ArrayList<>();
            try {
                OWLOntology placeholder = OWLManager.createOWLOntologyManager().createOntology();
                ChangeHistory ch = ChangeUtils.getLatestChanges(vont);
                changes = ch.getChanges(placeholder);
            }
            catch (OWLServerException e) {
                showSynchronizationErrorDialog("Error while fetching the latest changes from server", e);
            }
            catch (OWLOntologyCreationException e) {
                showSynchronizationErrorDialog("Internal error while computing changes", e);
            }
            return changes;
        }

        private List<OWLOntologyChange> getConflicts(List<OWLOntologyChange> localChanges, List<OWLOntologyChange> remoteChanges) {
            List<OWLOntologyChange> conflictChanges = new ArrayList<>();

            CollectingChangeVisitor clientChanges = CollectingChangeVisitor.collectChanges(localChanges);
            CollectingChangeVisitor serverChanges = CollectingChangeVisitor.collectChanges(remoteChanges);

            /*
             * Compute the conflicts by comparing the change signature between the client and server changes. The
             * change signature is defined by the OWL object that becomes the focus of the change (i.e.,
             * OWLImportDeclaration, OWLAnnotation, OWLAxiom) or the ontology ID.
             */
            if (clientChanges.getLastOntologyIDChange() != null && serverChanges.getLastOntologyIDChange() != null) {
                conflictChanges.add(clientChanges.getLastOntologyIDChange());
            }
            final Map<OWLImportsDeclaration, ImportChange> importChanges = clientChanges.getLastImportChangeMap();
            for (Entry<OWLImportsDeclaration, ImportChange> entry : importChanges.entrySet()) {
                OWLImportsDeclaration decl = entry.getKey();
                if (importChanges.containsKey(decl)) {
                    conflictChanges.add(entry.getValue());
                }
            }
            final Map<OWLAnnotation, AnnotationChange> annotationChanges = clientChanges.getLastOntologyAnnotationChangeMap();
            for (Entry<OWLAnnotation, AnnotationChange> entry : annotationChanges.entrySet()) {
                OWLAnnotation annotation = entry.getKey();
                if (annotationChanges.containsKey(annotation)) {
                    conflictChanges.add(entry.getValue());
                }
            }
            final Map<OWLAxiom, OWLAxiomChange> axiomChanges = clientChanges.getLastAxiomChangeMap();
            for (Entry<OWLAxiom, OWLAxiomChange> entry : axiomChanges.entrySet()) {
                OWLAxiom axiom = entry.getKey();
                if (axiomChanges.containsKey(axiom)) {
                    conflictChanges.add(entry.getValue());
                }
            }
            return conflictChanges;
        }

        private void adjustImports(List<OWLOntologyChange> changes) {
            OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
            configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
            configuration = configuration.setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH);
            try {
                adjustImports(changes, configuration);
            }
            catch (UnloadableImportException e) {
                showSynchronizationErrorDialog("Unexpected error when adjusting import declarations", e);
            }
        }
        
        private void adjustImports(List<OWLOntologyChange> changes, OWLOntologyLoaderConfiguration configuration)
                throws UnloadableImportException {
            Set<OWLImportsDeclaration> finalDeclaredImports = ontology.getImportsDeclarations();
            Set<OWLImportsDeclaration> missingImports = new TreeSet<OWLImportsDeclaration>();
            for (OWLOntologyChange change : changes) {
                if (change instanceof AddImport) {
                    OWLImportsDeclaration importDecl = ((AddImport) change).getImportDeclaration();
                    if (finalDeclaredImports.contains(importDecl)
                            && ontology.getOWLOntologyManager().getImportedOntology(importDecl) == null) {
                        missingImports.add(importDecl);
                    }
                }
            }
            for (OWLImportsDeclaration missingImport : missingImports) {
                ontology.getOWLOntologyManager().makeLoadImportRequest(missingImport, configuration);
            }
        }
    }
}
