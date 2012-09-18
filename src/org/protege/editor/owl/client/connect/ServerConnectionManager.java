package org.protege.editor.owl.client.connect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.editorkit.plugin.EditorKitHook;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.io.IOListener;
import org.protege.editor.owl.model.io.IOListenerEvent;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.util.ClientRegistry;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;

public class ServerConnectionManager extends EditorKitHook {
	public static String ID = "org.protege.editor.owl.client.ServerConnectionManager";
	
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
					ProtegeApplication.getErrorLog().logError(ioe);
				}
			}
		}
		
		@Override
		public void afterLoad(IOListenerEvent event) {
		    try {
		        OWLOntologyManager manager = getOWLOntologyManager();
		        OWLOntologyID id = event.getOntologyID();
		        OWLOntology ontology = manager.getOntology(id);
		        if (registry.hasSuitableMetaData(ontology)) {
		            addVersionedOntology(registry.getVersionedOntologyDocument(ontology));
		        }
		    }
		    catch (IOException ioe) {
		        ProtegeApplication.getErrorLog().logError(ioe);
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
	public void dispose() throws Exception {
		getOWLModelManager().removeIOListener(ioListener);
		getOWLOntologyManager().removeOntologyChangeListener(ontologyIdChangeListener);
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

}
