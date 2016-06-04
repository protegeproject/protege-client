package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Role;
import org.protege.editor.owl.client.admin.model.RoleMListItem;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RoleListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof RoleMListItem) {
            Role role = ((RoleMListItem)value).getRole();
            label.setText(role.getName().get());
            label.setPreferredSize(new Dimension(0, 25));
        }
        return label;
    }

}
