package org.protege.editor.owl.client.panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.changes.api.RemoteServerDirectory;
import org.protege.owl.server.changes.api.RemoteServerDocument;


public class ServerTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -1677982790864801841L;

	/*
	 * some growth coming...
	 */
	public enum Column {
		SERVER_DOCUMENT_LOCATION;
	}
	private List<RemoteServerDocument> serverDocuments = new ArrayList<RemoteServerDocument>();
	
	public void loadServerData(Client client, RemoteServerDirectory dir) throws OWLServerException {
		List<RemoteServerDocument> docs = new ArrayList<RemoteServerDocument>(client.list(dir));
		
		/*
		 * Tim and Jenn decided that for the first release, there shouldn't be any server directories 
		 * exposed to the user - only a list of ontology documents.  Server folder structure will come later.
		 */
		Iterator<RemoteServerDocument> iterator = docs.iterator();
		while (iterator.hasNext()) {
			RemoteServerDocument doc = iterator.next();
			if (doc instanceof RemoteServerDirectory) {
				iterator.remove();
			}
		}
		
		Collections.sort(docs, new Comparator<RemoteServerDocument>() {
			@Override
			public int compare(RemoteServerDocument doc1, RemoteServerDocument doc2) {
				return doc1.getServerLocation().compareTo(doc2.getServerLocation());
			}
		});
		serverDocuments = docs;
		fireTableStructureChanged();
	}

	@Override
	public int getColumnCount() {
		return Column.values().length;
	}
	
	@Override
	public String getColumnName(int col) {
		Column column = Column.values()[col];
		switch (column) {
		case SERVER_DOCUMENT_LOCATION:
			return "Ontology Documents";
		default:
			throw new IllegalStateException("Programmer missed a case");
		}
	}

	@Override
	public int getRowCount() {
		return serverDocuments.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		Column column = Column.values()[col];
		RemoteServerDocument doc = serverDocuments.get(row);
		switch (column) {
		case SERVER_DOCUMENT_LOCATION:
			String fragment = doc.getServerLocation().getFragment();
			if (fragment == null || fragment.isEmpty()) {
			    String path = doc.getServerLocation().toURI().getPath();
			    if (path.endsWith("/")) {
			        path = path.substring(0, path.length() - 1);
			    }
			    int start = path.lastIndexOf('/');
			    return path.substring(start + 1);
			}
			else return fragment;
		default:
			throw new IllegalStateException("Programmer missed a case");
		}
	}
	
	public RemoteServerDocument getValueAt(int row) {
		return serverDocuments.get(row);
	}

}
