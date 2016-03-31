package org.protege.editor.owl.client.util;

import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.changes.OntologyDocumentRevision;
import org.protege.owl.server.changes.api.ChangeHistory;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;
import org.protege.owl.server.connect.RemoteChangeService;
import org.protege.owl.server.connect.RmiChangeService;
import org.protege.owl.server.util.GetUncommittedChangesVisitor;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import edu.stanford.protege.metaproject.api.Address;
import edu.stanford.protege.metaproject.api.Host;

public class ChangeUtils {

    private static RemoteChangeService getChangeService(Host remoteHost) throws OWLServerException {
        String host = remoteHost.getAddress().get();
        int port = remoteHost.getRegistryPort().get();
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            return (RemoteChangeService) registry.lookup(RmiChangeService.CHANGE_SERVICE);
        }
        catch (RemoteException | NotBoundException e) {
            throw new OWLServerException(e);
        }
    }

    public static List<OWLOntologyChange> getUncommittedChanges(VersionedOntologyDocument versionedOntology)
            throws OWLServerException {
        ChangeHistory localChanges = versionedOntology.getLocalHistory();
//        OntologyDocumentRevision localHeadRevision = versionedOntology.getRevision();
        OntologyDocumentRevision localHeadRevision = localChanges.getEndRevision();
        try {
            Host remoteHost = versionedOntology.getRemoteHost();
            RemoteChangeService changeService = getChangeService(remoteHost);
            
            Address remoteAddress = versionedOntology.getRemoteAddress();
            ChangeHistory remoteChanges = changeService.getLatestChanges(remoteAddress, localHeadRevision);
            if (!remoteChanges.isEmpty()) {
                versionedOntology.appendLocalHistory(remoteChanges);
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
        catch (Exception e) {
            throw new OWLServerException(e);
        }
    }
}
