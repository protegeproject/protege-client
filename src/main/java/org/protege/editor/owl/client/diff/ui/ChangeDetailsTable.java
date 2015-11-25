package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsTable extends JTable {
    private static final long serialVersionUID = 6346557554881882633L;

    public ChangeDetailsTable(TableModel model, OWLEditorKit editorKit) {
        TableCellRenderer renderer = new ChangeDetailsTableCellRenderer(editorKit);
        setModel(model);
        setDefaultRenderer(Object.class, renderer);
        setRowHeight(30);
        setShowGrid(false);
        setAlignmentY(SwingConstants.CENTER);
        setRowMargin(0);
        setIntercellSpacing(new Dimension(0, 0));
        setRowSelectionAllowed(false);
        setCellSelectionEnabled(false);
        setFocusable(false);
    }
}