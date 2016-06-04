package org.protege.editor.owl.client.admin.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PropertyListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof ServerSettingsPanel.PropertyListItem) {
            String propertyName = ((ServerSettingsPanel.PropertyListItem)value).getPropertyName();
            String propertyValue = ((ServerSettingsPanel.PropertyListItem)value).getPropertyValue();
            label.setText("<html><b>" + propertyName + "</b>: " + propertyValue + "</html>");
            label.setPreferredSize(new Dimension(0, 25));
        }
        return label;
    }

}
