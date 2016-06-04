package org.protege.editor.owl.client.admin.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RootListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof ServerSettingsPanel.RootListItem) {
            String root = ((ServerSettingsPanel.RootListItem)value).getRoot();
            label.setText(root);
            label.setPreferredSize(new Dimension(0, 25));
        }
        return label;
    }

}
