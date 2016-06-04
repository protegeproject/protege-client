package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationType;
import org.protege.editor.owl.client.diff.ui.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OperationListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Operation operation = null;
        if(value instanceof OperationPanel.OperationListItem) {
            operation = ((OperationPanel.OperationListItem)value).getOperation();
        }
        else if(value instanceof Operation) {
            operation = (Operation) value;
        }
        if(operation != null) {
            label.setText(operation.getName().get());
            label.setIcon(getIcon(operation.getType()));
            label.setPreferredSize(new Dimension(0, 25));
            label.setIconTextGap(8);
        }
        return label;
    }

    private Icon getIcon(OperationType type) {
        switch(type) {
            case READ:
                return GuiUtils.getIcon(GuiUtils.PERMISSION_READ, 20, 20);
            case WRITE:
                return GuiUtils.getIcon(GuiUtils.PERMISSION_WRITE, 20, 20);
            case EXECUTE:
                return GuiUtils.getIcon(GuiUtils.PERMISSION_EXECUTE, 20, 20);
            default:
                throw new IllegalArgumentException("Programmer error: unknown operation type");
        }
    }
}
