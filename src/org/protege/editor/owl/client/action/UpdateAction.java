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
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.OWLServerException;
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
	    VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
	    if (vont == null) {
            JOptionPane.showMessageDialog(owner, "Update ignored because the ontology is not associated with a server");
            return;
	    }
	    @SuppressWarnings("unused")
        Future<?> ret = connectionManager.getSingleThreadExecutorService().submit(new DoUpdate(vont));
	    // if you wait here with ret.get(), then Protege will deadlock because he needs the UI thread to modify the ontology.
	}
	
	private class DoUpdate implements Runnable {
	    private VersionedOntologyDocument vont;
	    
	    public DoUpdate(VersionedOntologyDocument vont) {
	        this.vont = vont;
        }
	    
	    @Override
	    public void run() {
	        try {
	            OWLOntology ontology = vont.getOntology();
	            Client client = connectionManager.createClient(ontology);
	            ClientUtilities util = new ClientUtilities(client);
	            util.update(vont);
	        }
	        catch (OWLServerException ioe) {
	            ProtegeApplication.getErrorLog().logError(ioe);
	        }	        
	    }
	}

}
