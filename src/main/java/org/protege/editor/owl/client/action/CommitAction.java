package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.client.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CommitAction extends ProtegeOWLAction {
    private static final long serialVersionUID = 4601012273632698091L;
    private ServerConnectionManager connectionManager;
    private OWLModelManagerListener listener = new OWLModelManagerListener() {
		@Override
		public void handleChange(OWLModelManagerChangeEvent event) {
			updateEnabled();
		}
	};

    @Override
    public void initialise() throws Exception {
    	updateEnabled();
    	getOWLModelManager().addListener(listener);
    }
    
    private void updateEnabled() {
        connectionManager = ServerConnectionManager.get(getOWLEditorKit());
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
        if (vont == null) {
        	setEnabled(false);
        } else {
        	setEnabled(true);
        }
    }

    @Override
    public void dispose() throws Exception {
        getOWLModelManager().removeListener(listener);
    }
    
    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            Container owner = SwingUtilities.getAncestorOfClass(Frame.class,getOWLWorkspace());
            final OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
            final VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
            final String commitComment = JOptionPane.showInputDialog(owner, "Commit comment: ", "Commit", JOptionPane.PLAIN_MESSAGE);
            if (vont == null) {
                JOptionPane.showMessageDialog(owner, "Commit ignored because the ontology is not associated with a server");
                return;
            }
            if(commitComment == null) {
                return; // user pressed cancel
            }
            Client client = connectionManager.createClient(ontology);

            // TODO MetaData should not accept null commit comment..
            connectionManager.getSingleThreadExecutorService().submit(new DoCommit(client, vont, (!commitComment.isEmpty() ? commitComment : "")));
        }
        catch (UserDeclinedAuthenticationException udae) {
            ; // ignore this because the user knows that he didn't want to authenticate
        }
        catch (Exception e) {
        	handleError(e);
        }

    }
    
    // ToDo modify the synchronization so that this can run in the background
    //       even while edits are in progress.
    //       make this part of the utility? 
    private class DoCommit implements Runnable {
        private Client client;
        private VersionedOntologyDocument vont;
        private String commitcomment;
        public DoCommit(Client client, VersionedOntologyDocument vont, String commitComment) {
            this.client = client;
            this.vont = vont;
            this.commitcomment = commitComment;
        }
        
        @Override
        public void run() {
            ChangeMetaData metaData = new ChangeMetaData(commitcomment);
            try {
                ClientUtilities.commit(client, metaData, vont);
            }
            catch (OWLServerException ose) {
            	handleError(ose);
            }
            catch (Error e) {
            	handleError(e);
            }
            catch (RuntimeException re) {
            	handleError(re);
            }
        }
    }
    
    private void handleError(Throwable t) {
        ErrorLogPanel.showErrorDialog(t);
		UIHelper ui = new UIHelper(getOWLEditorKit());
		ui.showDialog("Error connecting to server", new JLabel("Commit failed - " + t.getMessage()));
    }
}
