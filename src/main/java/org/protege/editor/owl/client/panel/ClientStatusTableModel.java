package org.protege.editor.owl.client.panel;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.api.exception.OWLServerException;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.table.AbstractTableModel;

public class ClientStatusTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -465483270258124763L;

    public enum Column {
        NAME, VALUE;
    }

    public enum Row {
        SERVER_DOCUMENT("Ontology:") {
            @Override
            public String evaluate(Client client, VersionedOWLOntology vont) {
                return vont.getOntology().getOntologyID().toString();
            }
        },
        CLIENT_REVISION("Local revision:") {
            @Override
            public String evaluate(Client client, VersionedOWLOntology vont) {
                return vont.getRevision().toString();
            }
        },
        SERVER_REVISION("Remote revision:") {
            @Override
            public String evaluate(Client client, VersionedOWLOntology vont) throws OWLServerException {
                return ChangeUtils.getRemoteHeadRevision(vont).toString();
            }
        },
        UNCOMMITTED_CHANGES("#Uncommitted Changes:") {
            @Override
            public String evaluate(Client client, VersionedOWLOntology vont) throws OWLServerException {
                List<OWLOntologyChange> changes = ChangeUtils.getUncommittedChanges(vont);
                return changes.size()+"";
            }
        };

        private String name;

        private Row(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract Object evaluate(Client client, VersionedOWLOntology vont) throws OWLServerException;
    }

    private Client client;
    private VersionedOWLOntology vont;

    public ClientStatusTableModel(@Nonnull Client client, @Nonnull VersionedOWLOntology vont) {
        this.client = client;
        this.vont = vont;
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
        Row row = Row.values()[rowIndex];
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
