package org.protege.editor.owl.client.util;

import org.protege.editor.owl.server.api.exception.OWLServerException;
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

import java.rmi.RemoteException;
import java.util.List;

public class ChangeUtils {

    public static List<OWLOntologyChange> getUncommittedChanges(VersionedOWLOntology versionedOntology) throws OWLServerException {
        final ChangeHistory localHistory = versionedOntology.getChangeHistory();
        final OWLOntology ontology = versionedOntology.getOntology();
        List<OWLOntologyChange> baselineHistory = ChangeHistoryUtils.getOntologyChanges(localHistory, ontology);
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        for (OWLOntologyChange change : baselineHistory) {
            change.accept(visitor);
        }
        return visitor.getChanges();
    }

    public static ChangeHistory getAllChanges(VersionedOWLOntology versionedOntology) throws OWLServerException {
        return getAllChanges(versionedOntology.getServerDocument());
    }

    public static ChangeHistory getAllChanges(ServerDocument serverDocument) throws OWLServerException {
        try {
            RemoteChangeService changeService = (RemoteChangeService) ServerUtils.getRemoteService(
                serverDocument.getServerAddress(), serverDocument.getRegistryPort(), RmiChangeService.CHANGE_SERVICE);
            ChangeHistory allChanges = changeService.getAllChanges(serverDocument.getHistoryFile());
            return allChanges;
        }
        catch (RemoteException e) {
            throw new OWLServerException(e);
        }
        catch (Exception e) { // TODO Make as OWLServerServiceException
            throw new OWLServerException(e);
        }
    }

    public static ChangeHistory getLatestChanges(VersionedOWLOntology versionedOntology) throws OWLServerException {
        return getLatestChanges(versionedOntology.getServerDocument(), versionedOntology.getChangeHistory().getHeadRevision());
    }

    public static ChangeHistory getLatestChanges(ServerDocument serverDocument, DocumentRevision headRevision) throws OWLServerException {
        try {
            RemoteChangeService changeService = (RemoteChangeService) ServerUtils.getRemoteService(
                serverDocument.getServerAddress(), serverDocument.getRegistryPort(), RmiChangeService.CHANGE_SERVICE);
            ChangeHistory latestChanges = changeService.getLatestChanges(serverDocument.getHistoryFile(), headRevision);
            return latestChanges;
        }
        catch (RemoteException e) {
            throw new OWLServerException(e);
        }
        catch (Exception e) { // TODO Make as OWLServerServiceException
            throw new OWLServerException(e);
        }
    }

    public static DocumentRevision getRemoteHeadRevision(VersionedOWLOntology versionedOntology) throws OWLServerException {
        return getLatestChanges(versionedOntology).getHeadRevision();
    }
}
