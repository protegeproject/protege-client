package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

public class EnableAutoUpdateAction extends ProtegeOWLAction {
	private static final long serialVersionUID = 1098490684799516207L;
	private ScheduledFuture<?> autoUpdate;
	private JCheckBoxMenuItem checkBoxMenuItem;
	private ServerConnectionManager connectionManager;

	public EnableAutoUpdateAction() {
	}

	@Override
	public void initialise() throws Exception {
	    connectionManager = ServerConnectionManager.get(getOWLEditorKit());
	}

	@Override
	public void dispose() throws Exception {

	}

	public void setMenuItem(JMenuItem menu) {
	    checkBoxMenuItem = (JCheckBoxMenuItem) menu;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	    if (autoUpdate != null) {
	        autoUpdate.cancel(false);
	        autoUpdate = null;
	    }
	    else {
	        ScheduledExecutorService executor = connectionManager.getSingleThreadExecutorService();
	        autoUpdate = executor.scheduleWithFixedDelay(new AutoUpdate(), 15, 15, TimeUnit.SECONDS);
	    }
	}
	
	private class AutoUpdate implements Runnable {
	    private boolean lastRunSuccessful = true;
	    
	    @Override
	    public void run() {
            try {
                for (OWLOntology ontology : getOWLModelManager().getActiveOntologies()) {
                    VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
                    if (vont != null) {
                        Client client = connectionManager.createClient(vont.getServerDocument().getServerLocation());
                        ClientUtilities.update(client, vont);
                    }
                }
                lastRunSuccessful = true;
            }
            catch (UserDeclinedAuthenticationException udae) {
                autoUpdate.cancel(false);
                autoUpdate = null;
                checkBoxMenuItem.setSelected(false);
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
