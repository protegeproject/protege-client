package org.protege.editor.owl.client.panel;

import java.util.Date;

import javax.swing.table.AbstractTableModel;

import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.ChangeMetaData;

public class HistoryTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -1510343786742688724L;

	public enum Column {
        DATE("Date", Date.class) {
            @Override
            public Date getValue(ChangeMetaData metaData) {
                return metaData.getDate();
            } 
        },
        USER("Committer", String.class) {
            @Override
            public String getValue(ChangeMetaData metaData) {
                return metaData.getUserId().getUserName();
            }             
        },
        COMMIT_COMMENT("Description", String.class) {
            @Override
            public String getValue(ChangeMetaData metaData) {
                return metaData.getCommitComment();
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
        
        public abstract Object getValue(ChangeMetaData metaData);
        
    }
    private ChangeHistory changes;
    
    public HistoryTableModel(ChangeHistory changes) {
        this.changes = changes;
    }

    @Override
    public int getRowCount() {
        return changes.getEndRevision().getRevisionDifferenceFrom(changes.getStartRevision());
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
        ChangeMetaData metaData = changes.getMetaData(changes.getStartRevision().add(rowIndex));
        return col.getValue(metaData);
    }

}
