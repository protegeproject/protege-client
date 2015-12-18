package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.Change;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class MultipleChangeDetailsTableModel extends ChangeDetailsTableModel {
    private static final long serialVersionUID = 4589232596919944476L;
    private List<OWLOntologyChange> changes;

    /**
     * No-args constructor
     */
    public MultipleChangeDetailsTableModel() { }

    @Override
    public void setChange(Change change) {
        checkNotNull(change);
        this.changes = new ArrayList<>(change.getChanges());
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return (changes != null ? changes.size() : 0);
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return changes.get(rowIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        switch (col) {
            case CHANGES:
                return OWLOntologyChange.class;
            default:
                throw new IllegalStateException("Programmer Error: a case was missed");
        }
    }

    @Override
    public String getColumnName(int column) {
        return Column.values()[column].toString();
    }

    public enum Column {
        CHANGES("Change(s)");

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
