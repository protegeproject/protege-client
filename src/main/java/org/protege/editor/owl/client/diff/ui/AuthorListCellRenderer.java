package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.LogDiffManager;
import org.protege.owl.server.api.UserId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        UserId user = (UserId)value;
        if(user.equals(LogDiffManager.ALL_AUTHORS)) {
            label.setIcon(GuiUtils.getUserIcon(GuiUtils.USERS_ICON_FILENAME, 20, 20));
        }
        else {
            label.setIcon(GuiUtils.getUserIcon(GuiUtils.USER_ICON_FILENAME, 20, 20));
        }
        label.setBorder(new EmptyBorder(0, 7, 0, 0));
        label.setIconTextGap(7);
        return label;
    }

}
