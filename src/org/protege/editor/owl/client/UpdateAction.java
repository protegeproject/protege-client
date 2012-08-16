package org.protege.editor.owl.client;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOWLOntology;
import org.protege.owl.server.api.exception.ServerException;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

public class UpdateAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 2694484296709954780L;
    private ServerConnectionManager connectionManager;

	@Override
	public void initialise() throws Exception {
	    connectionManager = ServerConnectionManager.get(getOWLEditorKit());
	}

	@Override
	public void dispose() throws Exception {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
        Container owner = SwingUtilities.getAncestorOfClass(Frame.class,getOWLWorkspace());
	    OWLOntology ontology = getOWLModelManager().getActiveOntology();
	    VersionedOWLOntology vont = connectionManager.getVersionedOntology(ontology);
	    if (vont == null) {
            JOptionPane.showMessageDialog(owner, "Update ignored because the ontology is not associated with a server");
            return;
	    }
	    Client client = connectionManager.getClient(ontology);
	    ClientUtilities util = new ClientUtilities(client);
	    try {
	        util.update(vont);
	    }
	    catch (ServerException ioe) {
	        ProtegeApplication.getErrorLog().logError(ioe);
	    }
	}

}
