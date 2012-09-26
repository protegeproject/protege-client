package org.protege.editor.owl.client.action;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;

public class CommitAction extends ProtegeOWLAction {
    private static final long serialVersionUID = 4601012273632698091L;
    private ServerConnectionManager connectionManager;

    @Override
    public void initialise() throws Exception {
        connectionManager = ServerConnectionManager.get(getOWLEditorKit());
    }

    @Override
    public void dispose() throws Exception {
        
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
            Client client = connectionManager.createClient(ontology);
            Future<?> future = connectionManager.getSingleThreadExecutorService().submit(new DoCommit(client, vont, commitComment));
            future.get();
        }
        catch (UserDeclinedAuthenticationException udae) {
            ; // ignore this because the user knows that he didn't want to authenticate
        }
        catch (Exception e) {
            ProtegeApplication.getErrorLog().logError(e);
        }

    }
    
    // ToDo modify the synchronization so that this can run in tne background
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
            catch (OWLServerException ioe) {
                ProtegeApplication.getErrorLog().logError(ioe);
            }
        }
    }


}
