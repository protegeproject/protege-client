package org.protege.editor.owl.client.ui;

import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;

import java.util.Date;

import javax.swing.table.AbstractTableModel;

public class HistoryTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -1510343786742688724L;

    public enum Column {
        DATE("Date", Date.class) {
            @Override
            public Date getValue(RevisionMetadata metadata) {
                return metadata.getDate();
            }
        },
        USER("Author", String.class) {
            @Override
            public String getValue(RevisionMetadata metadata) {
                return metadata.getAuthorId();
            }
        },
        COMMIT_COMMENT("Comment", String.class) {
            @Override
            public String getValue(RevisionMetadata metadata) {
                return metadata.getComment();
            }
        };

        private String name;
        private Class<?> clazz;

        private Column(String name, Class<?> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public String getName() {
            return name;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public abstract Object getValue(RevisionMetadata metadata);
    }

    private ChangeHistory changes;

    public HistoryTableModel(ChangeHistory changes) {
        this.changes = changes;
    }

    @Override
    public int getRowCount() {
        return DocumentRevision.distance(changes.getBaseRevision(), changes.getHeadRevision());
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        return col.getClazz();
    }

    @Override
    public String getColumnName(int column) {
        Column col = Column.values()[column];
        return col.getName();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Column col = Column.values()[columnIndex];
        RevisionMetadata metadata = changes.getMetadataForRevision(changes.getBaseRevision().next(rowIndex + 1));
        return col.getValue(metadata);
    }
}
