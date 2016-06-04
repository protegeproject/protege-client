package org.protege.editor.owl.client.admin.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOptionsTable extends JTable {
    private static final long serialVersionUID = -6067222486109051358L;

    public ProjectOptionsTable(TableModel model) {
        TableCellRenderer renderer = new ProjectOptionsTableCellRenderer();
        setDefaultRenderer(String.class, renderer);
        setDefaultRenderer(Set.class, renderer);
        setModel(model);
        setRowHeight(20);
        setShowGrid(true);
        setAlignmentY(SwingConstants.CENTER);
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }

}
