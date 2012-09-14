package org.protege.editor.owl.client.panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.RemoteServerDirectory;
import org.protege.owl.server.api.RemoteServerDocument;
import org.protege.owl.server.api.exception.OWLServerException;


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
			return "Document";
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
			    int end;
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
