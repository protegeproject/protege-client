package org.protege.editor.owl.client.util;

import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.ChangeHistoryImpl;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ClientUtils {

    /**
     * Perform logout from the Protege client-server application.
     *
     * @param clientSession
     *          The existing client session
     * @param client
     *          The client to log out
     */
    public static void performLogout(ClientSession clientSession, Client client) throws Exception
    {
        if (client instanceof ClientSessionListener) {
            clientSession.removeListener((ClientSessionListener) client);
        }
        clientSession.clear();
    }

    /**
     * Compute the uncommitted changes given the input <code>ontology</code>. This method will use an
     * empty baseline for comparison and therefore will return all add operations that construct
     * this ontology.
     *
     * @param ontology
     *          The target ontology to check for uncommitted changes.
     * @param baseline
     *          The existing change history as the baseline for comparison.
     * @return A list of ontology changes
     */
    public static List<OWLOntologyChange> getUncommittedChanges(OWLOntology ontology) {
        return getUncommittedChanges(ontology, ChangeHistoryImpl.createEmptyChangeHistory());
    }

    /**
     * Compute the uncommitted changes given the input <code>ontology</code> and the initial change history
     * <code>baseline</code>. The baseline contains changes that are already committed. By comparing the
     * baseline and the new changes in the ontology, the method will be able to determine the uncommitted
     * changes.
     *
     * @param ontology
     *          The target ontology to check for uncommitted changes.
     * @param baseline
     *          The existing change history as the baseline for comparison.
     * @return A list of ontology changes
     */
    public static List<OWLOntologyChange> getUncommittedChanges(OWLOntology ontology, ChangeHistory baseline) {
        List<OWLOntologyChange> baselineHistory = ChangeHistoryUtils.getOntologyChanges(baseline, ontology);
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        for (OWLOntologyChange change : baselineHistory) {
            change.accept(visitor);
        }
        return visitor.getChanges();
    }

    /**
     * Create a commit object by specifying the <code>author</code>, <code>comment</code> string and
     * the list of <code>changes</code>.
     *
     * @param author
     *          The committer
     * @param comment
     *          The commit comment
     * @param changes
     *          The list of changes inside a commit
     * @return A commit object
     */
    public static Commit createCommit(Client author, String comment, List<OWLOntologyChange> changes) {
        RevisionMetadata metadata = new RevisionMetadata(
                author.getUserInfo().getId(),
                author.getUserInfo().getName(),
                author.getUserInfo().getEmailAddress(), comment);
        return new Commit(metadata, changes);
    }

    /**
     * Create a new OWL ontology instance by applying all the changes from the remote change history
     * specified in the <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *          The input server document.
     * @return A new OWL ontology instance.
     * @throws ClientRequestException
     * @throws OWLOntologyCreationException
     */
    public static OWLOntology buildOntology(ServerDocument serverDocument, OWLOntologyManager owlManager)
            throws ClientRequestException, OWLOntologyCreationException {
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        OWLOntology targetOntology = owlManager.createOntology();
        updateOntology(targetOntology, remoteChangeHistory, owlManager);
        return targetOntology;
    }

    /**
     * Update an existing <code>targetOntology</code> with recent changes from the remote change
     * history specified by the last local revision <code>localHead</code> and the
     * <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *          The input server document.
     * @param localHead
     *          The local HEAD reference.
     * @param targetOntology
     *          The target ontology to be updated
     * @return An updated OWL ontology instance.
     * @throws ClientRequestException
     * @throws OWLOntologyCreationException
     */
    public static OWLOntology updateOntology(ServerDocument serverDocument, DocumentRevision localHead,
            final OWLOntology targetOntology) throws ClientRequestException, OWLOntologyCreationException {
        ChangeHistory remoteChangeHistory = ChangeUtils.getLatestChanges(serverDocument, localHead);
        updateOntology(targetOntology, remoteChangeHistory, targetOntology.getOWLOntologyManager());
        return targetOntology;
    }

    /**
     * Create a new versioned ontology by restoring all the changes from the remote change history
     * specified in the <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *          The input server document
     * @return A new version ontology instance.
     * @throws ClientRequestException
     * @throws OWLOntologyCreationException
     */
    public static VersionedOWLOntology buildVersionedOntology(ServerDocument serverDocument, OWLOntologyManager owlManager)
            throws ClientRequestException, OWLOntologyCreationException {
        ChangeHistory remoteChangeHistory = ChangeUtils.getAllChanges(serverDocument);
        OWLOntology targetOntology = owlManager.createOntology();
        updateOntology(targetOntology, remoteChangeHistory, owlManager);
        return new VersionedOWLOntologyImpl(serverDocument, targetOntology, remoteChangeHistory);
    }

    /**
     * Create a new versioned ontology by restoring all recent changes from the
     * remote change history specified by the last local revision
     * <code>localHead</code> and the <code>serverDocument</code> reference.
     *
     * @param serverDocument
     *            The input server document.
     * @param localHead
     *            The local HEAD reference.
     * @param targetOntology
     *            The target ontology that will be updated
     * @return A new version ontology instance
     * @throws ClientRequestException
     * @throws OWLOntologyCreationException
     */
    public static VersionedOWLOntology buildVersionedOntology(ServerDocument serverDocument, DocumentRevision localHead,
            ChangeHistory localHistory, final OWLOntology targetOntology)
                    throws ClientRequestException, OWLOntologyCreationException {
        ChangeHistory remoteChangeHistory = ChangeUtils.getLatestChanges(serverDocument, localHead);
        updateOntology(targetOntology, remoteChangeHistory, targetOntology.getOWLOntologyManager());
        updateChangeHistory(localHistory, remoteChangeHistory);
        return new VersionedOWLOntologyImpl(serverDocument, targetOntology, localHistory);
    }

    /*
     * Private utility methods
     */

    private static void updateOntology(OWLOntology placeholder, ChangeHistory changeHistory, OWLOntologyManager manager) {
        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(changeHistory, placeholder);
        manager.applyChanges(changes);
        fixMissingImports(placeholder, changes, manager);
    }

    private static void updateChangeHistory(ChangeHistory changeHistory, ChangeHistory incomingChangeHistory) {
        final DocumentRevision base = incomingChangeHistory.getBaseRevision();
        final DocumentRevision end = incomingChangeHistory.getHeadRevision();
        for (DocumentRevision current = base.next(); current.behindOrSameAs(end); current = current.next()) {
            RevisionMetadata metadata = incomingChangeHistory.getMetadataForRevision(current);
            List<OWLOntologyChange> changes = incomingChangeHistory.getChangesForRevision(current);
            changeHistory.addRevision(metadata, changes);
        }
    }

    private static void fixMissingImports(OWLOntology ontology, List<OWLOntologyChange> changes, OWLOntologyManager manager) {
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
        configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        configuration = configuration.setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH);
        
        final Set<OWLImportsDeclaration> declaredImports = ontology.getImportsDeclarations();
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
