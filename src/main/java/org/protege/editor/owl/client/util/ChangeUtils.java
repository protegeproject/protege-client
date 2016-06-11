package org.protege.editor.owl.client.util;

import java.rmi.RemoteException;
import java.util.List;

import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.transport.rmi.RemoteChangeService;
import org.protege.editor.owl.server.transport.rmi.RmiChangeService;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeUtils {

    /**
     * @deprecated Use ClientUtils.getUncommittedChanges(OWLOntology, ChangeHistory) instead.
     */
    @Deprecated
    public static List<OWLOntologyChange> getUncommittedChanges(VersionedOWLOntology versionedOntology) {
        final ChangeHistory localHistory = versionedOntology.getChangeHistory();
        final OWLOntology ontology = versionedOntology.getOntology();
        List<OWLOntologyChange> baselineHistory = ChangeHistoryUtils.getOntologyChanges(localHistory, ontology);
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        for (OWLOntologyChange change : baselineHistory) {
            change.accept(visitor);
        }
        return visitor.getChanges();
    }

    public static ChangeHistory getAllChanges(VersionedOWLOntology versionedOntology) throws ClientRequestException {
        return getAllChanges(versionedOntology.getServerDocument());
    }

    public static ChangeHistory getAllChanges(ServerDocument serverDocument) throws ClientRequestException {
        try {
            RemoteChangeService changeService = (RemoteChangeService) ServerUtils.getRemoteService(
                serverDocument.getServerAddress(), serverDocument.getRegistryPort(), RmiChangeService.CHANGE_SERVICE);
            ChangeHistory allChanges = changeService.getAllChanges(serverDocument.getHistoryFile());
            return allChanges;
        }
        catch (RemoteException e) {
            throw new ClientRequestException(e.getCause());
        }
    }

    public static ChangeHistory getLatestChanges(VersionedOWLOntology versionedOntology) throws ClientRequestException {
        DocumentRevision startRevision = versionedOntology.getChangeHistory().getHeadRevision();
        return getLatestChanges(versionedOntology.getServerDocument(), startRevision);
    }

    public static ChangeHistory getLatestChanges(ServerDocument serverDocument, DocumentRevision startRevision)
            throws ClientRequestException {
        try {
            RemoteChangeService changeService = (RemoteChangeService) ServerUtils.getRemoteService(
                serverDocument.getServerAddress(), serverDocument.getRegistryPort(), RmiChangeService.CHANGE_SERVICE);
            ChangeHistory latestChanges = changeService.getLatestChanges(serverDocument.getHistoryFile(), startRevision);
            return latestChanges;
        }
        catch (RemoteException e) {
            throw new ClientRequestException(e.getCause());
        }
    }

    public static DocumentRevision getRemoteHeadRevision(VersionedOWLOntology versionedOntology) throws ClientRequestException {
        return getLatestChanges(versionedOntology).getHeadRevision();
    }
}
