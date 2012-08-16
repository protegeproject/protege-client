package org.protege.editor.owl.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.ServerDirectory;
import org.protege.owl.server.api.ServerDocument;
import org.protege.owl.server.api.exception.OWLServerException;


public class ServerTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -1677982790864801841L;

	/*
	 * some growth coming...
	 */
	public enum Column {
		SERVER_DOCUMENT_LOCATION;
	}
	private List<ServerDocument> serverDocuments = new ArrayList<ServerDocument>();
	
	public void loadServerData(Client client, ServerDirectory dir) throws OWLServerException {
		List<ServerDocument> docs = new ArrayList<ServerDocument>(client.list(dir));
		Collections.sort(docs, new Comparator<ServerDocument>() {
			@Override
			public int compare(ServerDocument doc1, ServerDocument doc2) {
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
		ServerDocument doc = serverDocuments.get(row);
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
	
	public ServerDocument getValueAt(int row) {
		return serverDocuments.get(row);
	}

}
