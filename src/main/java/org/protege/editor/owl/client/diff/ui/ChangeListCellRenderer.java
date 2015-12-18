package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.LogDiff;
import org.semanticweb.owlapi.model.*;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeListCellRenderer extends JTextArea implements ListCellRenderer<OWLOntologyChange>, Serializable {
    private OwlCellRenderer owlCellRenderer;

    public ChangeListCellRenderer(OWLEditorKit editorKit) {
        super();
        owlCellRenderer = new OwlCellRenderer(checkNotNull(editorKit));
    }

    @Override
    public Component getListCellRendererComponent(JList list, OWLOntologyChange change, int index, boolean isSelected, boolean cellHasFocus) {
        if(change.isAxiomChange()) {
            OWLAxiom axiom = change.getAxiom();
            Component c = owlCellRenderer.getListCellRendererComponent(list, axiom, index, isSelected, cellHasFocus);
            setBackground(list, change, c, isSelected);
            return c;
        }
        else if(change.isImportChange()) {
            OWLImportsDeclaration decl = ((ImportChange)change).getImportDeclaration();
            Component c = owlCellRenderer.getListCellRendererComponent(list, decl.getIRI(), index, isSelected, cellHasFocus);
            setBackground(list, change, c, isSelected);
            return c;
        }
        else if(change instanceof SetOntologyID) {
            setText("New IRI: " + ((SetOntologyID)change).getNewOntologyID().getOntologyIRI().toString());
            setBackground(list, change, this, isSelected);
        }
        else if(change instanceof AnnotationChange) {
            OWLAxiom axiom = getAnnotationAssertionAxiom((AnnotationChange)change);
            Component c = owlCellRenderer.getListCellRendererComponent(list, axiom, index, isSelected, cellHasFocus);
            setBackground(list, change, c, isSelected);
            return c;
        }
        return this;
    }

    private void setBackground(JList list, OWLOntologyChange change, Component c, boolean isSelected) {
        if (isSelected) {
            c.setBackground(list.getSelectionBackground());
            c.setForeground(list.getSelectionForeground());
        }
        else {
            GuiUtils.setComponentBackground(c, LogDiff.getChangeMode(change));
            c.setForeground(GuiUtils.UNSELECTED_FOREGROUND);
        }
    }

    private OWLAnnotationAssertionAxiom getAnnotationAssertionAxiom(AnnotationChange change) {
        OWLAnnotation ann = change.getAnnotation();
        OWLOntology ont = change.getOntology();
        OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
        return df.getOWLAnnotationAssertionAxiom(ont.getOntologyID().getOntologyIRI(), ann);
    }
}
