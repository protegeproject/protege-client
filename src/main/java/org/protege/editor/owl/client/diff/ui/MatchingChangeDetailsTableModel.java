package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.Change;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class MatchingChangeDetailsTableModel extends ChangeDetailsTableModel {
    private static final long serialVersionUID = 5697537763526759029L;
    private OWLOntologyChange baselineChange, newChange;

    /**
     * No-args constructor
     */
    public MatchingChangeDetailsTableModel() { }

    @Override
    public void setChange(Change change) {
        checkNotNull(change);
        baselineChange = checkNotNull(change.getBaselineChange().get());
        newChange = change.getChanges().iterator().next(); // there should only be one ontology change
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return (newChange != null && baselineChange != null ? 1 : 0);
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(getColumnName(columnIndex).equals(Column.BASELINE.toString())) {
            return baselineChange;
        }
        else if(getColumnName(columnIndex).equals(Column.NEW.toString())) {
            return newChange;
        }
        else {
            throw new IllegalStateException("Column does not exist");
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        switch (col) {
            case NEW:
            case BASELINE:
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
        BASELINE("Baseline"),
        NEW("New");

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
