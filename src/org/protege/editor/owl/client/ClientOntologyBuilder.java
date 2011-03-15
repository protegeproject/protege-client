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
import org.protege.editor.core.OntologyBuilder;
import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.editorkit.EditorKit;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
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

public class ClientOntologyBuilder implements OntologyBuilder {
    public static final String ID = ClientOntologyBuilder.class.getCanonicalName();
    public static final int AUTO_UPDATE_INTERVAL = 5000;
    private static final Map<OWLEditorKit, ClientOntologyBuilder> builderMap = new HashMap<OWLEditorKit, ClientOntologyBuilder>();
    private OWLEditorKit owlEditorKit;
    private ServerPreferences preferences = new ServerPreferences();
    private ClientConnection connection;
    private Logger logger = Logger.getLogger(getClass());
    private boolean loaded = false;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
       @Override
        public Thread newThread(Runnable r) {
           Thread th = new Thread(r, "Ontology Update & Commit Thread");
           th.setDaemon(true);
           return th;
        } 
    });

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
    public boolean loadOntology(EditorKit editorKit) {
        if (editorKit instanceof OWLEditorKit) {
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
        return loaded;
    }
    
    private void loadOntology(OWLModelManager p4Manager, 
                              final ClientConnection connection, 
                              ServerOntologyInfo info) throws OWLOntologyCreationException, RemoteQueryException {
        OWLOntologyManager manager = p4Manager.getOWLOntologyManager();
        final OWLOntology ontology = connection.pull(info.getOntologyName(), null);
        p4Manager.setActiveOntology(ontology);
        manager.addOntologyChangeListener(new OWLOntologyChangeListener() {

            @Override
            public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
                try {
                    if (!connection.isUpdateFromServer() && preferences.isAutoCommit()) {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    commit(ontology);
                                }
                                catch (Throwable t) {
                                    ProtegeApplication.getErrorLog().logError(t);
                                }
                            }
                        });
                    }
                }
                catch (Exception e) {
                    ProtegeApplication.getErrorLog().logError(e);
                }
            }
        });

        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (preferences.isAutoUpdate()) {
                        update(ontology);
                    }
                }
                catch (Throwable t) {
                    ProtegeApplication.getErrorLog().logError(t);
                }
            }
        }, AUTO_UPDATE_INTERVAL, AUTO_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {
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
