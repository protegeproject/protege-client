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

    /**
     * Create a new OWL ontology instance by applying all the changes from the remote change history
     * specified in the <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *          The input server document.
     * @return A new OWL ontology instance.
     * @throws IOException
     * @throws OWLServerException
     * @throws OWLOntologyCreationException
     */
    public static OWLOntology buildOntology(ServerDocument serverDocument)
            throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLMutableOntology targetOntology = createEmptyMutableOntology();
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(remoteChangeHistory, targetOntology);
        targetOntology.applyChanges(changes);
        fixMissingImports(targetOntology, changes);
        return targetOntology;
    }

    /**
     * Update an existing <code>currentOntology</code> with recent changes from the remote change
     * history specified by the last local revision <code>localHead</code> and the
     * <code>serverDocument</code> reference. The method will return a new copy rather than
     * overriding the input <code>currentOntology</code>.
     *
     * @param serverDocument
     *          The input server document.
     * @param localHead
     *          The local HEAD reference.
     * @param currentOntology
     *          The target ontology to be updated
     * @return An updated OWL ontology instance.
     * @throws IOException
     * @throws OWLServerException
     * @throws OWLOntologyCreationException
     */
    public static OWLOntology refreshOntology(ServerDocument serverDocument, DocumentRevision localHead,
            final OWLOntology currentOntology) throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLMutableOntology copyOntology = (OWLMutableOntology) owlManager.copyOntology(currentOntology, OntologyCopy.DEEP);
        ChangeHistory remoteChangeHistory = ChangeUtils.getLatestChanges(serverDocument, localHead);
        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(remoteChangeHistory, copyOntology);
        copyOntology.applyChanges(changes);
        fixMissingImports(copyOntology, changes);
        return copyOntology;
    }

    /**
     * Create a new versioned ontology by restoring all the changes from the remote change history
     * specified in the <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *          The input server document
     * @return A new version ontology instance.
     * @throws IOException
     * @throws OWLServerException
     * @throws OWLOntologyCreationException
     */
    public static VersionedOWLOntology buildVersionedOntology(ServerDocument serverDocument)
            throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLOntology targetOntology = buildOntology(serverDocument);
        VersionedOWLOntology versionedOntology = new VersionedOWLOntologyImpl(serverDocument, targetOntology);
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        versionedOntology.update(remoteChangeHistory);
        return versionedOntology;
    }

    /**
     * Create a new versioned ontology by restoring all recent changes from the remote change history
     * specified by the last local revision <code>localHead</code> and the <code>serverDocument</code>
     * reference.
     *
     * @param serverDocument
     *          The input server document.
     * @param localHead
     *          The local HEAD reference.
     * @param currentOntology
     *          The target ontology that will be updated
     * @return A new version ontology instance
     * @throws IOException
     * @throws OWLServerException
     * @throws OWLOntologyCreationException
     */
    public static VersionedOWLOntology buildVersionedOntology(ServerDocument serverDocument, DocumentRevision localHead,
            final OWLOntology currentOntology) throws IOException, OWLServerException, OWLOntologyCreationException {
        OWLOntology targetOntology = refreshOntology(serverDocument, localHead, currentOntology);
        VersionedOWLOntology versionedOntology = new VersionedOWLOntologyImpl(serverDocument, targetOntology);
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        versionedOntology.update(remoteChangeHistory);
        return versionedOntology;
    }

    /*
     * Private utility methods
     */

    private static OWLMutableOntology createEmptyMutableOntology() throws OWLOntologyCreationException {
        return (OWLMutableOntology) owlManager.createOntology();
    }

    private static void fixMissingImports(OWLMutableOntology targetOntology, List<OWLOntologyChange> changes) {
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
        configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        configuration = configuration.setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH);
        
        final Set<OWLImportsDeclaration> declaredImports = targetOntology.getImportsDeclarations();
        Set<OWLImportsDeclaration> missingImports = new TreeSet<OWLImportsDeclaration>();
        for (OWLOntologyChange change : changes) {
            if (change instanceof AddImport) {
                OWLImportsDeclaration importDecl = ((AddImport) change).getImportDeclaration();
                if (declaredImports.contains(importDecl) && owlManager.getImportedOntology(importDecl) == null) {
                    missingImports.add(importDecl);
                }
            }
        }
        for (OWLImportsDeclaration importDecl : missingImports) {
            owlManager.makeLoadImportRequest(importDecl, configuration);
        }
    }
}
