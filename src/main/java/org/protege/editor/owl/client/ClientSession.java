package org.protege.editor.owl.client;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.model.OWLEditorKitHook;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Map;
import java.util.TreeMap;

import edu.stanford.protege.metaproject.api.ProjectId;

public class ClientSession extends OWLEditorKitHook {

    public static String ID = "org.protege.editor.owl.client.ClientSession";

    private Client activeClient;

    private Map<OWLOntologyID, VersionedOWLOntology> ontologyMap = new TreeMap<>();

    private Map<OWLOntologyID, ProjectId> projectMap = new TreeMap<>();

    private OWLModelManagerListener changeActiveProject = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            OWLOntologyID ontologyId = event.getSource().getActiveOntology().getOntologyID();
            ProjectId projectId = projectMap.get(ontologyId);
            activeClient.setActiveProject(projectId);
        }
    };

    public static ClientSession getInstance(OWLEditorKit editorKit) {
        return (ClientSession) editorKit.get(ID);
    }

    @Override
    public void initialise() throws Exception {
        getEditorKit().getOWLModelManager().addListener(changeActiveProject);
    }

    public void setActiveClient(Client client) {
        activeClient = client;
        changeActiveClient();
    }

    public Client getActiveClient() {
        return activeClient;
    }

    public void registerProject(ProjectId projectId, VersionedOWLOntology versionOntology) {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        projectMap.put(ontologyId, projectId);
        ontologyMap.put(ontologyId, versionOntology);
    }

    public void unregisterProject(ProjectId projectId) {
        OWLOntologyID ontologyToRemove = null;
        for (OWLOntologyID ontologyId : projectMap.keySet()) {
            if (projectMap.get(ontologyId).equals(projectId)) {
                ontologyToRemove = ontologyId;
                break;
            }
        }
        if (ontologyToRemove != null) {
            projectMap.remove(ontologyToRemove);
            ontologyMap.remove(ontologyToRemove);
        }
    }

    public ProjectId getActiveProject() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return projectMap.get(ontologyId);
    }

    public VersionedOWLOntology getActiveVersionOntology() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return ontologyMap.get(ontologyId);
    }

    private void changeActiveClient() {
        // TODO How to close ontology properly?
        projectMap.clear();
        ontologyMap.clear();
    }

    @Override
    public void dispose() throws Exception {
        getEditorKit().getOWLModelManager().removeListener(changeActiveProject);
        activeClient = null;
        projectMap.clear();
        ontologyMap.clear();
    }
}
