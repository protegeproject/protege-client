package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionListener;

import org.protege.editor.owl.client.LocalHttpClient;

import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.api.exception.ClientRequestException;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.CollectingChangeVisitor;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
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
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.UnloadableImportException;

public class UpdateAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 2694484296709954780L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is disabled
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
            setEnabled(activeVersionOntology.isPresent());
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            Future<?> task = submit(new DoUpdate(activeVersionOntology.get()));
            @SuppressWarnings("unchecked")
            List<OWLOntologyChange> incomingChanges = (List<OWLOntologyChange>) task.get();
            if (incomingChanges.isEmpty()) {
                showInfoDialog("Update", "Local copy is already up-to-date");
            }
            else {
                String template = "Local copy is succesfully updated by %d changes";
                showInfoDialog("Update", String.format(template, incomingChanges.size()));
            }
        }
        catch (InterruptedException | ExecutionException e) {
            showErrorDialog("Update error", "Internal error: " + e.getMessage(), e);
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
                else {
                    throw new SynchronizationException("Conflict was detected and unable to merge changes from the server");
                }
            }
            return incomingChanges;
        }

        private void performUpdate(List<OWLOntologyChange> updates) {
            ontology.getOWLOntologyManager().applyChanges(updates);
            adjustImports(updates);
        }

        private boolean isUpdated() {
            try {
                DocumentRevision remoteHead = LocalHttpClient.current_user().getRemoteHeadRevision(vont);
                DocumentRevision localHead = vont.getHeadRevision();
                return localHead.sameAs(remoteHead);
            }
            catch (Exception e) {
                showErrorDialog("Update error", "Error while fetching the remote head revision", e);
                return false;
            }
        }

        public List<OWLOntologyChange> getLatestChangesFromClient() {
            return ClientUtils.getUncommittedChanges(vont.getOntology(), vont.getChangeHistory());
        }

        private List<OWLOntologyChange> getLatestChangesFromServer() {
            List<OWLOntologyChange> changes = new ArrayList<>();
            try {
                ChangeHistory remoteChangeHistory = LocalHttpClient.current_user().getLatestChanges(vont);
                changes = ChangeHistoryUtils.getOntologyChanges(remoteChangeHistory, ontology);
            }
            catch (Exception e) {
                showErrorDialog("Update error", "Error while fetching the latest changes from server", e);
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
                showErrorDialog("Update error", "Unexpected error when adjusting import declarations", e);
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
