package org.protege.editor.owl.client.util;

import org.protege.owl.server.changes.ServerDocument;
import org.protege.owl.server.changes.VersionedOntologyDocumentImpl;
import org.protege.owl.server.changes.api.ChangeHistory;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ClientUtils {

    public static OWLOntology buildOntology(ServerDocument serverDocument, OWLMutableOntology targetOntology) {
        if (serverDocument.getChangeHistory().isPresent()) {
            ChangeHistory remoteChangeHistory = serverDocument.getChangeHistory().get();
            List<OWLOntologyChange> changes = remoteChangeHistory.getChanges(targetOntology);
            targetOntology.applyChanges(changes);
            fixMissingImports(targetOntology, changes);
        }
        return targetOntology;
    }

    public static VersionedOntologyDocument constructVersionedOntology(ServerDocument serverDocument, OWLOntology targetOntology) {
        VersionedOntologyDocument versionedOntology = new VersionedOntologyDocumentImpl(serverDocument, targetOntology);
        if (serverDocument.getChangeHistory().isPresent()) {
            versionedOntology.appendChangeHistory(serverDocument.getChangeHistory().get());
        }
        return versionedOntology;
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
