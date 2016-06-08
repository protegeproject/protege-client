package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.diff.ui.GuiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOptionsTableCellRenderer extends JTextArea implements TableCellRenderer {

    /**
     * Constructor
     */
    public ProjectOptionsTableCellRenderer() {
        super();
        setOpaque(true);
        setBorder(new EmptyBorder(0, 2, 0, 2));
        setAlignmentY(CENTER_ALIGNMENT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof String) {
            setText((String) value);
            setToolTipText(getText());
        }
        else if(value instanceof Set) {
            List<String> values = new ArrayList<>((Set<String>) value);
            Collections.sort(values);
            String valueSet = "";
            Iterator it = values.iterator();
            while(it.hasNext()) {
                valueSet += it.next() + (it.hasNext() ? ", " : "");
            }
            setText(valueSet);
            setToolTipText(valueSet);
        }
        setBackground(table, this, isSelected);
        setFont(table.getFont());
        return this;
    }

    private void setBackground(JTable table, Component c, boolean isSelected) {
        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        }
        else {
            c.setBackground(table.getBackground());
            c.setForeground(GuiUtils.UNSELECTED_FOREGROUND);
        }
    }
}
