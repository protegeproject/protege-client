package org.protege.editor.owl.client.action;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.protege.editor.owl.client.panel.ChangeListTableModel.Column;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.RevisionPointer;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntologyChange;

public class ClientStatusTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -465483270258124763L;

	public enum Column {
		NAME, VALUE;
	}
	
	public enum Row {
		SERVER_DOCUMENT("Server document:") {
			@Override
			public String evaluate(Client client, VersionedOntologyDocument vont) {
				return vont.getServerDocument().getServerLocation().toString();
			}
		},
		CLIENT_REVISION("Local revision:") {
			@Override
			public String evaluate(Client client, VersionedOntologyDocument vont) {
				return vont.getRevision().toString();
			}
		},
		SERVER_REVISION("Latest server revision:") {
			@Override
			public String evaluate(Client client, VersionedOntologyDocument vont) throws OWLServerException {
				return client.evaluateRevisionPointer(vont.getServerDocument(), RevisionPointer.HEAD_REVISION).toString();
			}
		},
		UNCOMMITTED_CHANGES("# of uncommitted changes:") {
			@Override
			public String evaluate(Client client, VersionedOntologyDocument vont) throws OWLServerException {
				List<OWLOntologyChange> changes = ClientUtilities.getUncommittedChanges(client, vont);
				return "" + changes.size();
			}
		}
		;
		
		private String name;
		
		private Row(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		public abstract Object evaluate(Client client, VersionedOntologyDocument vont) throws OWLServerException;
	}
	
	private Client client;
	private VersionedOntologyDocument vont;
	
	public ClientStatusTableModel(Client client, VersionedOntologyDocument vont) {
		this.client = client;
		this.vont   = vont;
	}

	@Override
	public int getRowCount() {
		return Row.values().length;
	}

	@Override
	public int getColumnCount() {
		return Column.values().length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (vont == null) {
			return "Not a server ontology";
		}
		Row row    = Row.values()[rowIndex];
		Column column = Column.values()[columnIndex];
		try {
			switch (column) {
			case NAME:
				return row.getName();
			case VALUE:
				return row.evaluate(client, vont);
			default:
				return "Unknown column type";
			}
		}
		catch (OWLServerException e) {
			return "Error: " + e.getMessage();
		}
	}

}
