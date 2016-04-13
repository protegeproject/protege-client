package org.protege.editor.owl.client.util;

import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.changes.OntologyDocumentRevision;
import org.protege.owl.server.changes.ServerDocument;
import org.protege.owl.server.changes.api.ChangeHistory;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;
import org.protege.owl.server.connect.RmiChangeService;
import org.protege.owl.server.util.GetUncommittedChangesVisitor;

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

    public static List<OWLOntologyChange> getUncommittedChanges(VersionedOntologyDocument versionedOntology) throws OWLServerException {
        ChangeHistory remoteChanges = getLatestChanges(versionedOntology);
        if (!remoteChanges.isEmpty()) {
            versionedOntology.appendChangeHistory(remoteChanges);
        }

        final OntologyDocumentRevision startRevision = OntologyDocumentRevision.START_REVISION;
        final OntologyDocumentRevision endRevision = remoteChanges.getEndRevision();
        ChangeHistory uncommittedChanges = versionedOntology.getLocalHistory().cropChanges(startRevision, endRevision);

        final OWLOntology ontology = versionedOntology.getOntology();
        List<OWLOntologyChange> baselineHistory = uncommittedChanges.getChanges(ontology);
        GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ontology);
        for (OWLOntologyChange change : baselineHistory) {
            change.accept(visitor);
        }
        return visitor.getChanges();
    }

    public static ChangeHistory getAllChanges(VersionedOntologyDocument versionedOntology) throws OWLServerException {
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

    public static ChangeHistory getLatestChanges(VersionedOntologyDocument versionedOntology) throws OWLServerException {
        ServerDocument serverDocument = versionedOntology.getServerDocument();
        OntologyDocumentRevision localHeadRevision = versionedOntology.getLocalHistory().getEndRevision();
        RmiChangeService changeService = getChangeService(serverDocument.getHost());
        try {
            ChangeHistory latestChanges = changeService.getLatestChanges(serverDocument, localHeadRevision);
            return latestChanges;
        }
        catch (RemoteException e) {
            throw new OWLServerException(e);
        }
    }

    public static OntologyDocumentRevision getRemoteHeadRevision(VersionedOntologyDocument versionedOntology) throws OWLServerException {
        return getLatestChanges(versionedOntology).getEndRevision();
    }
}
