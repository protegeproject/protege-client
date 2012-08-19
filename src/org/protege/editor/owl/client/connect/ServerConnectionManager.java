package org.protege.editor.owl.client.connect;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.protege.owl.server.api.DocumentFactory;
import org.protege.owl.server.api.VersionedOntologyDocument;
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
	
	private Map<OWLOntologyID, VersionedOntologyDocument> ontologyMap = new TreeMap<OWLOntologyID, VersionedOntologyDocument>();
	private Map<OWLOntologyID, Client> clientMap = new TreeMap<OWLOntologyID, Client>();
	private Map<IRI, Client> serverIRIToClientMap = new TreeMap<IRI, Client>();
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
			OWLOntologyID id = event.getOntologyID();
			OWLOntology ontology = getOWLOntologyManager().getOntology(id);
			for (Client client : serverIRIToClientMap.values()) {
				connectIfCompatible(client, ontology);
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
					Client client = clientMap.get(originalId);
					if (vont != null) {
						OWLOntologyID newId = idChange.getNewOntologyID();
						ontologyMap.put(newId, vont);
						clientMap.put(newId, client);
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
	
	public Client getClient(OWLOntology ontology) {
		return clientMap.get(ontology.getOntologyID());
	}
	
	public void addVersionedOntology(Client client, VersionedOntologyDocument vont) {
		OWLOntologyID id = vont.getOntology().getOntologyID();
		ontologyMap.put(id, vont);
		clientMap.put(id, client);
	}
	
	public void addClient(Client client) {
		serverIRIToClientMap.put(client.getServerIRI(), client);
		for (OWLOntology ontology : getOWLOntologyManager().getOntologies()) {
			if (ontologyMap.get(ontology.getOntologyID()) == null) {
				connectIfCompatible(client, ontology);
			}
		}
	}
	
	public Client hasClient(IRI serverLocation) {
	    URI serverURI = serverLocation.toURI();
	    String schema = serverURI.getScheme();
	    String host   = serverURI.getHost();
	    int port      = serverURI.getPort();
	    for (Entry<IRI, Client> entry : serverIRIToClientMap.entrySet()) {
	        URI clientURI = entry.getKey().toURI();
	        Client client = entry.getValue();
	        if (schema.equals(clientURI.getScheme()) && host.equals(clientURI.getHost()) && port == clientURI.getPort()) {
	            return client;
	        }
	    }
	    return null;
	}
	
	private boolean connectIfCompatible(Client client, OWLOntology ontology) {
		DocumentFactory docFactory = client.getDocumentFactory();
		OWLOntologyID id = ontology.getOntologyID();
		if (docFactory.hasServerMetadata(ontology)) {
			try {
				VersionedOntologyDocument vont = docFactory.createVersionedOntology(ontology);
				if (client.isCompatible(vont)) {
					ontologyMap.put(id, vont);
					clientMap.put(id, client);
					return true;
				}
			} catch (IOException e) {
				ProtegeApplication.getErrorLog().logError(e); // ToDo: until I figure out what to do...
			}
		}
		return false;
	}

}
