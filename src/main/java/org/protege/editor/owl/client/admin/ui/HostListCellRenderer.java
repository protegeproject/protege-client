package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Host;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class HostListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof ServerSettingsPanel.HostListItem) {
            Host host = ((ServerSettingsPanel.HostListItem)value).getHost();
            label.setText("<html><b>" + host.getUri().toString() + "</b>" +
                    (host.getSecondaryPort().isPresent() ? " (admin port: " + host.getSecondaryPort().get().get() + ")" : "") + "</html> b ");
            label.setPreferredSize(new Dimension(0, 25));
        }
        return label;
    }

}
