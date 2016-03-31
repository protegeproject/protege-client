package org.protege.editor.owl.client.connect;

import org.protege.editor.core.editorkit.plugin.EditorKitHook;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.io.IOListener;
import org.protege.editor.owl.model.io.IOListenerEvent;
import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientRegistry;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerConnectionManager extends EditorKitHook {
	public static String ID = "org.protege.editor.owl.client.ServerConnectionManager";
	private Logger logger = Logger.getLogger(ServerConnectionManager.class.getCanonicalName());
	
	public static ServerConnectionManager get(OWLEditorKit editorKit) {
		return (ServerConnectionManager) editorKit.get(ID);
	}
	
	private ClientRegistry registry = new ClientRegistry();
	private Map<OWLOntologyID, VersionedOntologyDocument> ontologyMap = new TreeMap<OWLOntologyID, VersionedOntologyDocument>();

	private ScheduledExecutorService singleThreadExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
	   @Override
	    public Thread newThread(Runnable r) {
	       Thread th = new Thread(r, "Client-Server Communications");
	       th.setDaemon(true);
	       return th;
	    } 
	});
	
	private IOListener ioListener = new IOListener() {
		
		@Override
		public void beforeSave(IOListenerEvent event) {

		}
		
		@Override
		public void beforeLoad(IOListenerEvent event) {
			
		}
		
		@Override
		public void afterSave(IOListenerEvent event) {
			OWLOntologyID id = event.getOntologyID();
			VersionedOntologyDocument vont = ontologyMap.get(id);
			if (vont != null) {
				try {
					vont.saveMetaData();
				}
				catch (IOException ioe) {
					ErrorLogPanel.showErrorDialog(ioe);
				}
			}
		}
		
		@Override
		public void afterLoad(IOListenerEvent event) {
		    try {
		        OWLOntologyManager manager = getOWLOntologyManager();
		        OWLOntologyID id = event.getOntologyID();
		        OWLOntology ontology = manager.getOntology(id);
		        if (ontology != null && registry.hasSuitableMetaData(ontology)) {
		            addVersionedOntology(registry.getVersionedOntologyDocument(ontology));
		        }
		    }
		    catch (IOException ioe) {
				ErrorLogPanel.showErrorDialog(ioe);
		    }
		}
	};
	
	private OWLOntologyChangeListener ontologyIdChangeListener = new OWLOntologyChangeListener() {
		public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
			for (OWLOntologyChange change : changes) {
				if (change instanceof SetOntologyID) {
					SetOntologyID idChange = (SetOntologyID) change;
					OWLOntologyID originalId = idChange.getOriginalOntologyID();
					VersionedOntologyDocument vont = ontologyMap.remove(originalId);
					if (vont != null) {
						OWLOntologyID newId = idChange.getNewOntologyID();
						ontologyMap.put(newId, vont);
					}
				}
			}
		}
	};
	
	@Override
	protected OWLEditorKit getEditorKit() {
		return (OWLEditorKit) super.getEditorKit();
	}
	
	protected OWLModelManager getOWLModelManager() {
		return getEditorKit().getOWLModelManager();
	}
	
	protected OWLOntologyManager getOWLOntologyManager() {
		return getEditorKit().getOWLModelManager().getOWLOntologyManager();
	}

	@Override
	public void initialise() throws Exception {
	    registry.addFactory(new RMIClientFactory(getEditorKit().getOWLWorkspace()));
		getOWLModelManager().addIOListener(ioListener);
		getOWLOntologyManager().addOntologyChangeListener(ontologyIdChangeListener);
	}

	@Override
	public void dispose() throws InterruptedException {
		getOWLModelManager().removeIOListener(ioListener);
		getOWLOntologyManager().removeOntologyChangeListener(ontologyIdChangeListener);
		singleThreadExecutorService.shutdown();
		singleThreadExecutorService.awaitTermination(5, TimeUnit.MINUTES);
	}
	
	public ScheduledExecutorService getSingleThreadExecutorService() {
        return singleThreadExecutorService;
    }
	
	public VersionedOntologyDocument getVersionedOntology(OWLOntology ontology) {
		return ontologyMap.get(ontology.getOntologyID());
	}
	
	public void addVersionedOntology(VersionedOntologyDocument vont) {
		OWLOntologyID id = vont.getOntology().getOntologyID();
		ontologyMap.put(id, vont);
	}
	
	public Client createClient(OWLOntology ontology) throws OWLServerException {
	    VersionedOntologyDocument vont = ontologyMap.get(ontology.getOntologyID());
	    if (vont != null) {
	        return registry.connectToServer(vont.getServerDocument().getServerLocation());
	    }
	    return null;
	}
	
	public Client createClient(IRI serverLocation) throws OWLServerException {
	    return registry.connectToServer(serverLocation);
	}

	public Client createClient(IRI serverLocation, String username, String password) throws OWLServerException {
        return registry.connectToServer(serverLocation, username, password);
	}

	public void saveHistoryInBackground(VersionedOntologyDocument vont) {
	    singleThreadExecutorService.submit(new SaveHistory(vont));
	}
	
	private class SaveHistory implements Runnable {
	    private VersionedOntologyDocument vont;
	    
	    public SaveHistory(VersionedOntologyDocument vont) {
	        this.vont = vont;
        }
	    
	    @Override
	    public void run() {
	        try {
	            long startTime = System.currentTimeMillis();
	            vont.saveLocalHistory();
	            long interval = System.currentTimeMillis() - startTime;
	            if (interval > 1000) {
	                logger.info("Save of history file for " + vont.getOntology().getOntologyID() + " took " + (interval / 1000) + " seconds.");
	            }
	        }
	        catch (Error | RuntimeException | IOException e) {
				ErrorLogPanel.showErrorDialog(e);
	        }
		}
	}

}
