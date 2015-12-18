package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Date;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangesTable extends JTable {
    private static final long serialVersionUID = 224898759560741040L;

    public ChangesTable(TableModel model, OWLEditorKit editorKit) {
        TableCellRenderer renderer = new ChangesTableCellRenderer(editorKit);
        setModel(model);
        setDefaultRenderer(Object.class, renderer);
        setDefaultRenderer(Date.class, renderer);
        setDefaultRenderer(Boolean.class, renderer);
        setRowHeight(32);
        setShowGrid(false);
        setAlignmentY(SwingConstants.CENTER);
        setRowMargin(0);
        setIntercellSpacing(new Dimension(0, 0));
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
}