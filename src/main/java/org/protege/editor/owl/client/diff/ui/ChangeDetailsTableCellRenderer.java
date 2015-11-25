package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.ChangeMode;
import org.protege.editor.owl.client.diff.model.LogDiff;
import org.semanticweb.owlapi.model.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsTableCellRenderer extends LogDiffCellRenderer {

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public ChangeDetailsTableCellRenderer(OWLEditorKit editorKit) {
        super(editorKit);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        ChangeMode mode = LogDiff.getChangeMode((OWLOntologyChange)value);
        if (value instanceof OWLAxiomChange) {
            OWLAxiom axiomChange = ((OWLAxiomChange)value).getAxiom();
            Component c = owlCellRenderer.getTableCellRendererComponent(table, axiomChange, isSelected, hasFocus, row, column);
            setBackground(table, mode, c, isSelected);
            return c;
        }
        else if(value instanceof AnnotationChange) {
            OWLAxiom ax = getAnnotationAssertionAxiom((AnnotationChange) value);
            Component c = owlCellRenderer.getTableCellRendererComponent(table, ax, isSelected, hasFocus, row, column);
            setBackground(table, mode, c, isSelected);
            return c;
        }
        else if(value instanceof ImportChange) {
            OWLImportsDeclaration decl = ((ImportChange)value).getImportDeclaration();
            Component c = owlCellRenderer.getTableCellRendererComponent(table, decl.getIRI(), isSelected, hasFocus, row, column);
            setBackground(table, mode, c, isSelected);
            return c;
        }
        else if(value instanceof SetOntologyID) {
            IRI newIri = ((SetOntologyID)value).getNewOntologyID().getOntologyIRI();
            setText("New IRI: " + newIri);
        }
        setBackground(table, mode, this, isSelected);
        setFont(table.getFont());
        return this;
    }

    private OWLAnnotationAssertionAxiom getAnnotationAssertionAxiom(AnnotationChange change) {
        OWLAnnotation ann = change.getAnnotation();
        OWLOntology ont = change.getOntology();
        OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
        return df.getOWLAnnotationAssertionAxiom(ont.getOntologyID().getOntologyIRI(), ann);
    }

    private void setBackground(JTable table, ChangeMode mode, Component c, boolean isSelected) {
        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        }
        else {
            GuiUtils.setComponentBackground(c, mode);
            c.setForeground(GuiUtils.UNSELECTED_FOREGROUND);
        }
    }
}
