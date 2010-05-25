package org.protege.editor.owl.client;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

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
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ClientOntologyBuilder implements OntologyBuilder {
    public static final String ID = ClientOntologyBuilder.class.getCanonicalName();
    public static final int AUTO_UPDATE_INTERVAL = 5000;
    private Logger logger = Logger.getLogger(getClass());
    private boolean loaded = false;

    public ClientOntologyBuilder() {
    }

    @Override
    public boolean loadOntology(EditorKit editorKit) {
        if (editorKit instanceof OWLEditorKit) {
            OWLEditorKit kit = (OWLEditorKit) editorKit;
            final OWLModelManager p4Manager = kit.getModelManager();
            OWLOntologyManager manager = p4Manager.getOWLOntologyManager();
            Window parent = (Window) SwingUtilities.getAncestorOfClass(Window.class, kit.getOWLWorkspace());
            final ServerConnectionDialog dialog = new ServerConnectionDialog(parent, manager);
            JButton open = new JButton("Open in Protege");
            dialog.getConnectionInfoPanel().add(open);
            open.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ServerOntologyInfo info = dialog.getSelectedOntology();
                    if (info != null) {
                        try {
                            ClientConnection connection = dialog.getClientConnection();
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
                    if (!connection.isUpdateFromServer()) {
                        connection.commit(Collections.singleton(ontology));
                    }
                }
                catch (Exception e) {
                    ProtegeApplication.getErrorLog().logError(e);
                }
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sleepABit();
                while (true) {
                    try {
                        connection.update(ontology, null);
                    }
                    catch (Throwable t) {
                        ProtegeApplication.getErrorLog().logError(t);
                    }
                    sleepABit();
                }
            }
            
            private void sleepABit() {
                try {
                    Thread.sleep(AUTO_UPDATE_INTERVAL);
                }
                catch (InterruptedException e) {
                    logger.error("Ouch! Why did you do that?", e);
                }
            }
            
        }, "Ontology Auto-Update Thread for " + info.getShortName());
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {
    }

}
