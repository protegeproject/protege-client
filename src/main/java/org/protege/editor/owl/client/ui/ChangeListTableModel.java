package org.protege.editor.owl.client.ui;

import org.protege.editor.owl.server.versioning.ChangeMetadata;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitor;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;

import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ChangeListTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -3184570774482187890L;

    public enum Column {
        CHANGE_TYPE("Type") {
            @Override
            public Date getValue(ChangeMetadata metaData) {
                return metaData.getDate();
            }
        },
        ENTITY("Entity") {
            @Override
            public String getValue(ChangeMetadata metaData) {
                return metaData.getAuthorId().get();
            }
        };

        private String name;

        private Column(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract Object getValue(ChangeMetadata metaData);
    }

    private List<OWLOntologyChange> changes;

    public ChangeListTableModel(List<OWLOntologyChange> changes) {
        this.changes = changes;
    }

    public void setChangeList(List<OWLOntologyChange> changes) {
        this.changes = changes;
        fireTableStructureChanged();
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public int getRowCount() {
        return changes.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Column col = Column.values()[columnIndex];
        switch (col) {
        case CHANGE_TYPE:
            return String.class;
        case ENTITY:
            return OWLObject.class;
        default:
            throw new IllegalStateException("Programmer Error: he missed a case.");
        }
    }

    @Override
    public String getColumnName(int column) {
        Column col = Column.values()[column];
        return col.getName();
    }

    @Override
    public Object getValueAt(int row, int column) {
        Column col = Column.values()[column];
        OWLOntologyChange change = changes.get(row);
        RenderOntologyChangeVisitor visitor = new RenderOntologyChangeVisitor();
        change.accept(visitor);
        switch (col) {
            case CHANGE_TYPE:
                return visitor.getChangeType();
            case ENTITY:
                return visitor.getEntityChanged();
            default:
                throw new IllegalStateException("Programmer error: he missed a case");
        }
    }

    private static class RenderOntologyChangeVisitor implements OWLOntologyChangeVisitor {

        private String changeType;
        private OWLObject entityChanged;

        public String getChangeType() {
            return changeType;
        }

        public Object getEntityChanged() {
            return entityChanged;
        }

        @Override
        public void visit(AddAxiom change) {
            changeType = "Add Axiom";
            entityChanged = change.getAxiom();
        }

        @Override
        public void visit(RemoveAxiom change) {
            changeType = "Remove Axiom";
            entityChanged = change.getAxiom();
        }

        @Override
        public void visit(SetOntologyID change) {
            changeType = "Set Ontology Id";
            entityChanged = change.getNewOntologyID().getDefaultDocumentIRI().get();
        }

        @Override
        public void visit(AddImport change) {
            changeType = "Add Import";
            entityChanged = change.getImportDeclaration().getIRI();
        }

        @Override
        public void visit(RemoveImport change) {
            changeType = "Remove Import";
            entityChanged = change.getImportDeclaration().getIRI();
        }

        @Override
        public void visit(AddOntologyAnnotation change) {
            changeType = "Add Ontology Annotation";
            entityChanged = change.getAnnotation();
        }

        @Override
        public void visit(RemoveOntologyAnnotation change) {
            changeType = "Remove Ontology Annotation";
            entityChanged = change.getAnnotation();
        }
    }
}
