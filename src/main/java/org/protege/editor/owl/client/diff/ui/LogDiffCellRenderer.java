package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public abstract class LogDiffCellRenderer extends JTextArea implements TableCellRenderer {
    protected TableCellRenderer owlCellRenderer;

    protected LogDiffCellRenderer(OWLEditorKit editorKit) {
        super();
        owlCellRenderer = new OwlCellRenderer(checkNotNull(editorKit));
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(new EmptyBorder(1, 2, 1, 2));
        setAlignmentY(CENTER_ALIGNMENT);
    }
}
