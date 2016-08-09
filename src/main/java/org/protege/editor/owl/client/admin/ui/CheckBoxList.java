package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.PolicyObject;
import edu.stanford.protege.metaproject.api.Operation;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CheckBoxList<E> extends JList<E> {
    private static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public CheckBoxList() {
        setCellRenderer(new CheckBoxCellRenderer<>());
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index != -1) {
                    JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                    checkbox.setSelected(!checkbox.isSelected());
                    fireSelectionValueChanged(index, index, false);
                    repaint();
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int index = getSelectedIndex();
                    if (index != -1) {
                        JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                        checkbox.setSelected(!checkbox.isSelected());
                        fireSelectionValueChanged(index, index, false);
                        repaint();
                    }
                }
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }


    public class CheckBoxCellRenderer<T> implements ListCellRenderer<T> {

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
            AugmentedJCheckBox checkbox = (AugmentedJCheckBox) value;
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            PolicyObject obj = checkbox.getObject();
            if(obj.isOperation()) {
                checkbox.setText("<html>" + obj.getName().get() + " <i>&nbsp;(" + ((Operation)obj).getType().toString().toLowerCase() + ")</i></html>");
            } else {
                checkbox.setText(obj.getName().get());
            }
            return checkbox;
        }

    }
}