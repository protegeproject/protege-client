package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.PolicyObject;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PolicyObjectComboBoxRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value != null) {
            label.setText(((PolicyObject) value).getName().get());
        }
        return label;
    }

}
