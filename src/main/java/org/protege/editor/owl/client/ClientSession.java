package org.protege.editor.owl.client;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.model.OWLEditorKitHook;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Map;
import java.util.TreeMap;

public class ClientSession extends OWLEditorKitHook {

    public static String ID = "org.protege.editor.owl.client.ClientSession";

    private Client activeClient;

    private Map<OWLOntologyID, VersionedOntologyDocument> ontologyMap = new TreeMap<>();

    public static ClientSession getInstance(OWLEditorKit editorKit) {
        return (ClientSession) editorKit.get(ID);
    }

    @Override
    public void initialise() throws Exception {
        // NO-OP
    }

    public void setActiveClient(Client client) {
        activeClient = client;
        changeActiveClient();
    }

    public Client getActiveClient() {
        return activeClient;
    }

    public VersionedOntologyDocument getVersionedOntology(OWLOntologyID ontologyId) {
        return ontologyMap.get(ontologyId);
    }

    public void addVersionedOntology(VersionedOntologyDocument versionOntology) {
        OWLOntologyID ontologyId = versionOntology.getOntology().getOntologyID();
        ontologyMap.put(ontologyId, versionOntology);
    }

    private void changeActiveClient() {
        for (VersionedOntologyDocument vont : ontologyMap.values()) {
            getEditorKit().getOWLModelManager().removeOntology(vont.getOntology()); // TODO How to close ontology properly?
        }
        ontologyMap.clear();
    }

    @Override
    public void dispose() throws Exception {
        activeClient = null;
        ontologyMap.clear();
    }
}
