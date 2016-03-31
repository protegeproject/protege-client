package org.protege.editor.owl.client;

import org.protege.editor.core.editorkit.plugin.EditorKitHook;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.api.Client;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Map;
import java.util.TreeMap;

public class ClientRegistry extends EditorKitHook {

    public static String ID = "org.protege.editor.owl.client.ClientHook";

    private Client activeClient;

    private Map<OWLOntologyID, VersionedOntologyDocument> ontologyMap = new TreeMap<>();

    public static ClientRegistry getInstance(OWLEditorKit editorKit) {
        return (ClientRegistry) editorKit.get(ID);
    }

    @Override
    public void initialise() throws Exception {
        // TODO Auto-generated method stub
    }

    public void setActiveClient(Client client) {
        activeClient = client;
    }

    public Client getActiveClient() {
        return activeClient;
    }

    public VersionedOntologyDocument getVersionedOntology(OWLOntology ontology) {
        return ontologyMap.get(ontology.getOntologyID());
    }

    public void addVersionedOntology(VersionedOntologyDocument versionOntology) {
        OWLOntologyID id = versionOntology.getOntology().getOntologyID();
        ontologyMap.put(id, versionOntology);
    }

    @Override
    public void dispose() throws Exception {
        // TODO Auto-generated method stub
    }
}
