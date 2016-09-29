package org.protege.editor.owl.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.workspace.WorkspaceTab;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.LocalHttpClient.UserType;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.CommitOperationEvent;
import org.protege.editor.owl.client.event.CommitOperationListener;
import org.protege.editor.owl.model.OWLEditorKitHook;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.OWLWorkspaceViewsTab;
import org.protege.editor.owl.ui.ontology.OntologyPreferences;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.swing.*;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.base.Optional;

import edu.stanford.protege.metaproject.api.ProjectId;

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

    private Set<CommitOperationListener> commitListeners = new HashSet<>();
    
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

    public void fireCommitPerformedEvent(CommitOperationEvent event) {
        for (CommitOperationListener listener : commitListeners) {
            listener.operationPerformed(event);
        }
    }

    public void addCommitOperationListener(CommitOperationListener listener) {
        commitListeners.add(listener);
    }

    public void removeCommitOperationListener(CommitOperationListener listener) {
        commitListeners.remove(listener);
    }

    public void setActiveClient(Client client) {
        if (!hasActiveClient()) {
            activeClient = client;            
            getEditorKit().getWorkspace().setCheckLevel(new TabViewableChecker(client));
            getEditorKit().getWorkspace().recheckPlugins();
            if (((LocalHttpClient) client).getClientType() == UserType.ADMIN) {
            	WorkspaceTab admin = getEditorKit().getOWLWorkspace().getWorkspaceTab("org.protege.editor.owl.client.AdminTab");
        		
        		((OWLWorkspaceViewsTab) admin).fireUpViews();
            }
            fireChangeEvent(EventCategory.USER_LOGIN);
        }
        else {
            if (isPreviouslyLoggedIn(client)) {
                activeClient = client; // change the client ref without broadcasting login event
            }
            else {
                JOptionPaneEx.showConfirmDialog(getEditorKit().getWorkspace(), "Login warning",
                        new JLabel("Cannot re-login as a different user. Please logout first."),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
            }
        }
    }

    private boolean isPreviouslyLoggedIn(Client client) {
        UserInfo userInfo = client.getUserInfo();
        return getActiveClient().getUserInfo().getId().equals(userInfo.getId())
                && getActiveClient().getUserInfo().getName().equals(userInfo.getName())
                && getActiveClient().getUserInfo().getEmailAddress().equals(userInfo.getEmailAddress())
                && client.getAuthToken().isAuthorized();
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
        fireChangeEvent(EventCategory.OPEN_PROJECT);

    }

    public ProjectId getActiveProject() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return projectMap.get(ontologyId);
    }

    public VersionedOWLOntology getActiveVersionOntology() {
        OWLOntologyID ontologyId = getEditorKit().getOWLModelManager().getActiveOntology().getOntologyID();
        return ontologyMap.get(ontologyId);
    }


    public void clear() {
    	activeClient = null;
        closeOpenVersionedOntologies();
        unregisterAllProjects();
        unregisterAllVersionOntologies();
        fireChangeEvent(EventCategory.USER_LOGOUT); 
        getEditorKit().getWorkspace().recheckPlugins();
    }

    private void closeOpenVersionedOntologies() {
        try {
            switchActiveOntologyToNonVersionedOntology();
            for (VersionedOWLOntology vont : ontologyMap.values()) {
                OWLOntology openOntology = vont.getOntology();
                getEditorKit().getOWLModelManager().removeOntology(openOntology);
            }
        }
        catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Could not create fresh active ontology to switch to", e);
        }
    }

    private void switchActiveOntologyToNonVersionedOntology() throws OWLOntologyCreationException {
        if (!isActiveOntologyVersionedOntology()) {
            return;
        }
        Optional<OWLOntology> candidateActiveOntology = getExistingNonVersionedOntology();
        if (candidateActiveOntology.isPresent()) {
            getEditorKit().getModelManager().setActiveOntology(candidateActiveOntology.get());
        }
        else {
            Optional<IRI> freshOntologyIRI =
                    Optional.of(IRI.create(OntologyPreferences.getInstance().generateNextURI()));
            OWLOntologyID ontologyID = new OWLOntologyID(freshOntologyIRI, freshOntologyIRI);
            getEditorKit().getModelManager().createNewOntology(ontologyID,
                    ontologyID.getDefaultDocumentIRI().get().toURI());
        }
    }

    private boolean isActiveOntologyVersionedOntology() {
        OWLOntology activeOntology = getEditorKit().getOWLModelManager().getActiveOntology();
        return ontologyMap.containsKey(activeOntology.getOntologyID());
    }

    private Optional<OWLOntology> getExistingNonVersionedOntology() {
        for (OWLOntology ont : getEditorKit().getOWLModelManager().getOntologies()) {
            if (!ontologyMap.containsKey(ont.getOntologyID())) {
                return Optional.of(ont);
            }
        }
        return Optional.absent();
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
