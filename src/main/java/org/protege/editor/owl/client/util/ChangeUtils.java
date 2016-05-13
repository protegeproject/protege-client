package org.protege.editor.owl.client.util;

import org.protege.editor.owl.server.api.exception.OWLServerException;
import org.protege.editor.owl.server.transport.rmi.RmiChangeService;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.DocumentRevision;
import org.protege.editor.owl.server.versioning.ServerDocument;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import edu.stanford.protege.metaproject.api.Host;

public class ChangeUtils {

    private static RmiChangeService getChangeService(Host remoteHost) throws OWLServerException {
        URI remoteUri = remoteHost.getUri();
        String host = remoteUri.getHost();
        int port = remoteHost.getSecondaryPort().isPresent() ? remoteHost.getSecondaryPort().get().get() : remoteUri.getPort();
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            return (RmiChangeService) registry.lookup(RmiChangeService.CHANGE_SERVICE);
        }
        catch (RemoteException | NotBoundException e) {
            throw new OWLServerException(e);
        }
    }

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
        ServerDocument serverDocument = versionedOntology.getServerDocument();
        RmiChangeService changeService = getChangeService(serverDocument.getHost());
        try {
            ChangeHistory allChanges = changeService.getAllChanges(serverDocument);
            return allChanges;
        }
        catch (RemoteException e) {
            throw new OWLServerException(e);
        }
    }

    public static ChangeHistory getLatestChanges(VersionedOWLOntology versionedOntology) throws OWLServerException {
        ServerDocument serverDocument = versionedOntology.getServerDocument();
        DocumentRevision localHeadRevision = versionedOntology.getChangeHistory().getHeadRevision();
        RmiChangeService changeService = getChangeService(serverDocument.getHost());
        try {
            ChangeHistory latestChanges = changeService.getLatestChanges(serverDocument, localHeadRevision);
            return latestChanges;
        }
        catch (RemoteException e) {
            throw new OWLServerException(e);
        }
    }

    public static DocumentRevision getRemoteHeadRevision(VersionedOWLOntology versionedOntology) throws OWLServerException {
        return getLatestChanges(versionedOntology).getHeadRevision();
    }
}
