package org.protege.editor.owl.client;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.owl.server.api.ClientConnection;
import org.protege.owl.server.api.ServerOntologyInfo;
import org.protege.owl.server.exception.RemoteQueryException;
import org.protege.owlapi.model.ProtegeOWLOntologyManager;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ClientOntologyBuilder extends ProtegeAction {
    public static final String ID = ClientOntologyBuilder.class.getCanonicalName();
    public static final int AUTO_UPDATE_INTERVAL = 5000;
    public static final Logger LOGGER = Logger.getLogger(ClientOntologyBuilder.class);
    
    private static final Map<OWLEditorKit, ClientOntologyBuilder> builderMap = new HashMap<OWLEditorKit, ClientOntologyBuilder>();
    
    private OWLEditorKit owlEditorKit;
    private ServerPreferences preferences = new ServerPreferences();
    private ClientConnection connection;

    private boolean loaded = false;
    
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
       @Override
        public Thread newThread(Runnable r) {
           Thread th = new Thread(r, "Ontology Update & Commit Thread");
           th.setDaemon(true);
           return th;
        } 
    });
    
    private OWLModelManagerListener modelManagerListener = new OWLModelManagerListener() {
        
        public void handleChange(OWLModelManagerChangeEvent event) {
            if (event.getType() == EventType.ACTIVE_ONTOLOGY_CHANGED) {
                updateTitle();
            }
        }
 
    };

    public ClientOntologyBuilder() {
    }
    
    public static ServerPreferences getServerPreferences(OWLEditorKit editorKit) {
        ClientOntologyBuilder builder = builderMap.get(editorKit);
        return builder != null ? builder.preferences : null;
    }
    
    public static void update(OWLEditorKit editorKit) {
        ClientOntologyBuilder builder = builderMap.get(editorKit);
        if (builder != null) {
            builder.update(editorKit.getModelManager().getActiveOntology());
        }
    }
    
    public static void commit(OWLEditorKit editorKit) {
        ClientOntologyBuilder builder = builderMap.get(editorKit);
        if (builder != null) {
            builder.commit(editorKit.getModelManager().getActiveOntology());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	OWLEditorKit editorKit = (OWLEditorKit) getEditorKit();
    	owlEditorKit = (OWLEditorKit) editorKit;
    	builderMap.put(owlEditorKit, this);
    	final OWLModelManager p4Manager = owlEditorKit.getModelManager();
    	ProtegeOWLOntologyManager manager = (ProtegeOWLOntologyManager) p4Manager.getOWLOntologyManager();
    	Window parent = (Window) SwingUtilities.getAncestorOfClass(Window.class, owlEditorKit.getOWLWorkspace());
    	final ServerConnectionDialog dialog = new ServerConnectionDialog(parent, manager);
    	JButton open = new JButton("Open in Protege");
    	dialog.getConnectionInfoPanel().add(open);
    	open.addActionListener(new ActionListener() {
    		@Override
    		public void actionPerformed(ActionEvent e) {
    			ServerOntologyInfo info = dialog.getSelectedOntology();
    			if (info != null) {
    				try {
    					connection = dialog.getClientConnection();
    					loadOntology(p4Manager, connection, info);
    					dialog.dispose();
    					loaded = true;
    				}
    				catch (Exception ex) {
    					ProtegeApplication.getErrorLog().logError(ex);
    				}
    			}
    		}
    	});
    	dialog.setVisible(true);
    }
    
    private void loadOntology(OWLModelManager p4Manager, 
                              final ClientConnection connection, 
                              ServerOntologyInfo info) throws OWLOntologyCreationException, RemoteQueryException {
        OWLOntologyManager manager = p4Manager.getOWLOntologyManager();
        final OWLOntology ontology = connection.pull(info.getOntologyName(), null);
        p4Manager.setActiveOntology(ontology);
        updateTitle();
        manager.addOntologyChangeListener(new OWLOntologyChangeListener() {
            
            @Override
            public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
                try {
                    updateTitle();
                }
                catch (Throwable t) {
                    ProtegeApplication.getErrorLog().logError(t);
                }
            }
        });

        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!connection.isUpdateFromServer() 
                            && !connection.getUncommittedChanges(ontology).isEmpty() 
                            && preferences.isAutoCommit()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Auto-committing changes for ontology " + ontology);
                        }
                        commit(ontology);
                        updateTitle();
                    }
                    if (preferences.isAutoUpdate()) {
                        update(ontology);
                        updateTitle();
                    }
                }
                catch (Throwable t) {
                    ProtegeApplication.getErrorLog().logError(t);
                }
            }
        }, AUTO_UPDATE_INTERVAL, AUTO_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        owlEditorKit.getOWLModelManager().addListener(modelManagerListener);
    }
    
    
    private void updateTitle() {
        OWLWorkspace workspace = owlEditorKit.getOWLWorkspace();
        OWLOntology activeOntology = owlEditorKit.getModelManager().getActiveOntology();
        if (connection.getOntologies().contains(activeOntology)) {
            StringBuffer title = new StringBuffer("Server Ontology: ");
            title.append(activeOntology.getOntologyID().getOntologyIRI());
            title.append(" -- Uncommitted Changes = ");
            title.append(connection.getUncommittedChanges(activeOntology).size());
            title.append(" Revision: ");
            title.append(connection.getClientRevision(activeOntology));
            workspace.setTitle(title.toString());
        }
        else {
            workspace.setTitle(null);
        }
    }

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {
        owlEditorKit.getModelManager().removeListener(modelManagerListener);
        executor.shutdown();
        builderMap.remove(owlEditorKit);
        synchronized (this) {
            notifyAll();
        }
    }

    private void commit(OWLOntology ontology) {
        try {
            connection.commit(Collections.singleton(ontology));
        }
        catch (Throwable t) {
            ProtegeApplication.getErrorLog().logError(t);
        }
    }

    private void update(OWLOntology ontology) {
        try {
            connection.update(ontology, null);
        }
        catch (Throwable t) {
            ProtegeApplication.getErrorLog().logError(t);
        }
    }

}
