package org.protege.editor.owl.client;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOWLOntology;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;
import org.protege.owl.server.api.exception.ServerException;

public class CommitAction extends ProtegeOWLAction {
    private static final long serialVersionUID = 4601012273632698091L;
    private ServerConnectionManager connectionManager;

    @Override
    public void initialise() throws Exception {
        connectionManager = ServerConnectionManager.get(getOWLEditorKit());
    }

    @Override
    public void dispose() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Container owner = SwingUtilities.getAncestorOfClass(Frame.class,getOWLWorkspace());
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        VersionedOWLOntology vont = connectionManager.getVersionedOntology(ontology);
        if (vont == null) {
            JOptionPane.showMessageDialog(owner, "Commit ignored because the ontology is not associated with a server");
            return;
        }
        Client client = connectionManager.getClient(ontology);
        ClientUtilities util = new ClientUtilities(client);
        String commitComment = JOptionPane.showInputDialog(owner, "Commit comment: ", "Commit", JOptionPane.PLAIN_MESSAGE);
        ChangeMetaData metaData = new ChangeMetaData(commitComment);
        try {
            util.commit(metaData, vont);
        }
        catch (ServerException ioe) {
            ProtegeApplication.getErrorLog().logError(ioe);
        }
    }



}
