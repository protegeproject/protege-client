package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.User;
import org.protege.editor.owl.client.diff.ui.GuiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class UserListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof UserPanel.UserListItem) {
            User user = ((UserPanel.UserListItem)value).getUser();
            label.setText(user.getName().get());
            label.setIcon(GuiUtils.getIcon(GuiUtils.USER_ICON_FILENAME, 20, 20));
            label.setBorder(new EmptyBorder(0, 7, 0, 0));
            label.setIconTextGap(8);
        }
        return label;
    }

}
