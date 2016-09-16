package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.LogDiffManager;
import org.protege.editor.owl.client.ui.GuiUtils;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String user = (String) value;
        if(user.equals(LogDiffManager.ALL_AUTHORS)) {
            label.setIcon(GuiUtils.getIcon(GuiUtils.USERS_ICON_FILENAME, 20, 20));
            label.setFont(getFont().deriveFont(Font.BOLD));
        }
        else {
            label.setIcon(GuiUtils.getIcon(GuiUtils.USER_ICON_FILENAME, 20, 20));
        }
        label.setBorder(new EmptyBorder(0, 7, 0, 0));
        label.setIconTextGap(7);
        return label;
    }

}
