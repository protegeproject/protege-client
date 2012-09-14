package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

public class EnableAutoUpdateAction extends ProtegeOWLAction {
	private static final long serialVersionUID = 1098490684799516207L;
	private ScheduledFuture<?> autoUpdate;

	public EnableAutoUpdateAction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initialise() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    final ServerConnectionManager connectionManager = ServerConnectionManager.get(getOWLEditorKit());
	    final OWLOntology ontology = getOWLModelManager().getActiveOntology();
	    final VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
	    if (autoUpdate != null) {
	        autoUpdate.cancel(false);
	    }
	    else {
	        ScheduledExecutorService executor = connectionManager.getSingleThreadExecutorService();
	        autoUpdate = executor.scheduleWithFixedDelay(new AutoUpdate(connectionManager, vont), 15, 15, TimeUnit.SECONDS);
	    }
	}
	
	private static class AutoUpdate implements Runnable {
	    private ServerConnectionManager connectionManager;
	    private VersionedOntologyDocument vont;
	    private boolean lastRunSuccessful = true;
	    
	    public AutoUpdate(ServerConnectionManager connectionManager, VersionedOntologyDocument vont) {
	        this.connectionManager = connectionManager;
	        this.vont = vont;
	    }
	    
	    @Override
	    public void run() {
            try {
                Client client = connectionManager.createClient(vont.getServerDocument().getServerLocation());
                ClientUtilities.update(client, vont);
                lastRunSuccessful = true;
            }
            catch (Throwable t) {
                if (!lastRunSuccessful) { 
                    ProtegeApplication.getErrorLog().logError(t);
                }
                lastRunSuccessful = false;
            }
	    }
	}

}
