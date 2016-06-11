package org.protege.editor.owl.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.stanford.protege.metaproject.api.ProjectId;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.model.OWLEditorKitHook;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientSession extends OWLEditorKitHook {

    public static String ID = "org.protege.editor.owl.client.ClientSession";

    private Client activeClient;

    private Map<OWLOntologyID, VersionedOWLOntology> ontologyMap = new TreeMap<>();

    private Map<OWLOntologyID, ProjectId> projectMap = new TreeMap<>();

    private Set<ClientSessionListener> clientSessionListeners = new HashSet<>();

    private OWLModelManagerListener changeActiveProject = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            if (event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED)) {
                fireChangeEvent(EventCategory.SWITCH_ONTOLOGY);
            }
        }
    };

    public static ClientSession getInstance(OWLEditorKit editorKit) {
        return (ClientSession) editorKit.get(ID);
    }

    @Override
    public void initialise() throws Exception {
        getEditorKit().getOWLModelManager().addListener(changeActiveProject);
    }

    private void fireChangeEvent(EventCategory category) {
        ClientSessionChangeEvent event = new ClientSessionChangeEvent(this, category);
        for (ClientSessionListener listener : clientSessionListeners) {
            listener.handleChange(event);
        }
    }

    public void addListener(ClientSessionListener listener) {
        clientSessionListeners.add(listener);
    }

    public void removeListener(ClientSessionListener listener) {
        clientSessionListeners.remove(listener);
    }

    public void setActiveClient(Client client) {
        if (hasActiveClient()) {
            activeClient = null;
            unregisterAllProjects();
            unregisterAllVersionOntologies();
        }
        activeClient = client;
        fireChangeEvent(EventCategory.SWITCH_CLIENT);
    }

    public Client getActiveClient() {
        return activeClient;
    }

    public boolean hasActiveClient() {
        return getActiveClient() != null;
    }

    public void setActiveProject(ProjectId projectId, VersionedOWLOntology versionOntology) {
        registerProject(versionOntology.getOntology().getOntologyID(), projectId);
        registerVersionOntology(versionOntology.getOntology().getOntologyID(), versionOntology);
        getEditorKit().getOWLModelManager().setActiveOntology(versionOntology.getOntology());
        //fireChangeEvent();

    }

    public ProjectId getActiveProject() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return projectMap.get(ontologyId);
    }

    public VersionedOWLOntology getActiveVersionOntology() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return ontologyMap.get(ontologyId);
    }


    public void clear() throws Exception {
        activeClient = null;
        closeOpenOntologies();
        unregisterAllProjects();
        unregisterAllVersionOntologies();
        fireChangeEvent(EventCategory.CLEAR_SESSION);
    }

    private void closeOpenOntologies() throws Exception {
        OWLOntology lastOntology = null;
        for (VersionedOWLOntology vont : ontologyMap.values()) {
            OWLOntology openOntology = vont.getOntology();
            if (!getEditorKit().getOWLModelManager().removeOntology(openOntology)) {
                /*
                 * If the open ontology couldn't be removed then it means the ontology
                 * was the last ontology.
                 */
                lastOntology = openOntology;
            }
        }
        /*
         * Remove the last ontology by first creating a dummy new ontology (it then
         * becomes the last ontology) and then remove the recorded last ontology.
         */
        if (lastOntology != null) {
            getEditorKit().handleNewRequest();
            getEditorKit().getModelManager().removeOntology(lastOntology);
        }
    }

    @Override
    public void dispose() throws Exception {
        getEditorKit().getOWLModelManager().removeListener(changeActiveProject);
    }

    private void registerProject(OWLOntologyID ontologyId, ProjectId projectId) {
        projectMap.put(ontologyId, projectId);
    }

    private void unregisterAllProjects() {
        projectMap.clear();
    }

    private void registerVersionOntology(OWLOntologyID ontologyId, VersionedOWLOntology versionOntology) {
        ontologyMap.put(ontologyId, versionOntology);
    }

    private void unregisterAllVersionOntologies() {
        ontologyMap.clear();
    }
}
