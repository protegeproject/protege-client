package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.ProjectOptions;

import javax.swing.table.AbstractTableModel;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOptionsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -5039396360955539648L;
    private ProjectOptions options;

    /**
     * No-args constructor
     */
    public ProjectOptionsTableModel() { }

    public void setOptions(ProjectOptions options) {
        this.options = checkNotNull(options);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return (options != null ? options.getOptions().size() : 0);
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (Column.values()[columnIndex]) {
            case KEY:
                return getKey(rowIndex);
            case VALUES:
                return options.getOptions().get(getKey(rowIndex));
            default:
                throw new IllegalStateException();
        }
    }

    public String getKey(int rowIndex) {
        return options.getOptions().keySet().toArray(new String[options.getOptions().size()])[rowIndex];
    }

    public Set<String> getValues(String key) {
        return options.getOptions().get(key);
    }

    public Set<String> getValues(int rowIndex) {
        return getValues(getKey(rowIndex));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        switch (col) {
            case KEY:
                return String.class;
            case VALUES:
                return Set.class;
            default:
                throw new IllegalStateException("Programmer Error: a case was missed");
        }
    }

    public String getColumnName(int column) {
        return Column.values()[column].toString();
    }

    public enum Column {
        KEY("Key"),
        VALUES("Value(s)");

        private String name;

        Column(String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
