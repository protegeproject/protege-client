package org.protege.editor.owl.client.util;

import org.protege.editor.owl.server.api.exception.OWLServerException;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ClientUtils {

    private static OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();

    public static OWLOntology buildOntology(ServerDocument serverDocument)
            throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLMutableOntology targetOntology = createEmptyMutableOntology();
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(remoteChangeHistory, targetOntology);
        targetOntology.applyChanges(changes);
        fixMissingImports(targetOntology, changes);
        return targetOntology;
    }

    public static OWLOntology rebuildOntology(ServerDocument serverDocument, DocumentRevision localHead,
            final OWLOntology currentOntology) throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLMutableOntology copyOntology = (OWLMutableOntology) owlManager.copyOntology(currentOntology, OntologyCopy.DEEP);
        ChangeHistory remoteChangeHistory = ChangeUtils.getLatestChanges(serverDocument, localHead);
        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(remoteChangeHistory, copyOntology);
        copyOntology.applyChanges(changes);
        fixMissingImports(copyOntology, changes);
        return copyOntology;
    }

    public static VersionedOWLOntology buildVersionedOntology(ServerDocument serverDocument)
            throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLOntology targetOntology = buildOntology(serverDocument);
        VersionedOWLOntology versionedOntology = new VersionedOWLOntologyImpl(serverDocument, targetOntology);
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        versionedOntology.update(remoteChangeHistory);
        return versionedOntology;
    }

    public static VersionedOWLOntology rebuildVersionedOntology(ServerDocument serverDocument, DocumentRevision localHead,
            final OWLOntology currentOntology) throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLOntology targetOntology = rebuildOntology(serverDocument, localHead, currentOntology);
        VersionedOWLOntology versionedOntology = new VersionedOWLOntologyImpl(serverDocument, targetOntology);
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        versionedOntology.update(remoteChangeHistory);
        return versionedOntology;
    }

    /*
     * Private utility methods
     */

    private static OWLMutableOntology createEmptyMutableOntology() throws OWLOntologyCreationException {
        return (OWLMutableOntology) OWLManager.createOWLOntologyManager().createOntology();
    }

    private static void fixMissingImports(OWLMutableOntology targetOntology, List<OWLOntologyChange> changes) {
        OWLOntologyManager manager = targetOntology.getOWLOntologyManager();
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
        configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        configuration = configuration.setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH);
        
        final Set<OWLImportsDeclaration> declaredImports = targetOntology.getImportsDeclarations();
        Set<OWLImportsDeclaration> missingImports = new TreeSet<OWLImportsDeclaration>();
        for (OWLOntologyChange change : changes) {
            if (change instanceof AddImport) {
                OWLImportsDeclaration importDecl = ((AddImport) change).getImportDeclaration();
                if (declaredImports.contains(importDecl) && manager.getImportedOntology(importDecl) == null) {
                    missingImports.add(importDecl);
                }
            }
        }
        for (OWLImportsDeclaration importDecl : missingImports) {
            manager.makeLoadImportRequest(importDecl, configuration);
        }
    }
}
