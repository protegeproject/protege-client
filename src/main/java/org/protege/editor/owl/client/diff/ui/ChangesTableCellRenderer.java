package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.ChangeMode;
import org.protege.editor.owl.client.diff.model.ChangeReview;
import org.protege.editor.owl.client.diff.model.ChangeType;
import org.semanticweb.owlapi.model.OWLObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangesTableCellRenderer extends JTextArea implements TableCellRenderer, Serializable {
    private static final long serialVersionUID = 17448042406257048L;
    private TableCellRenderer owlCellRenderer;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public ChangesTableCellRenderer(OWLEditorKit editorKit) {
        super();
        owlCellRenderer = new OwlCellRenderer(checkNotNull(editorKit));
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(new EmptyBorder(1, 2, 1, 2));
        setAlignmentY(CENTER_ALIGNMENT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // coordinates passed to the renderer are in view coordinate system after sorting, so they need converting to model coordinates before accessing the model
        int modelRow = table.convertRowIndexToModel(row);
        ChangeMode mode = (ChangeMode) table.getModel().getValueAt(modelRow, ChangesTableModel.Column.MODE.ordinal());
        ChangeType type = (ChangeType) table.getModel().getValueAt(modelRow, ChangesTableModel.Column.CHANGE_TYPE.ordinal());
        if (value instanceof OWLObject) {
            Component c = owlCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(table, type, mode, c, isSelected);
            return c;
        }
        if(value instanceof Boolean) {
            if((Boolean)value) {
                return getIconLabel(table, type, mode, "warning.png", isSelected, true);
            } else {
                value = null;
            }
        }
        if(value != null && value instanceof ChangeReview) {
            ChangeReview review = (ChangeReview) value;
            boolean committed = review.getAuthor().isPresent();
            switch(review.getStatus()) {
                case ACCEPTED:
                    return getIconLabel(table, type, mode, "review-accepted.png", isSelected, committed);
                case REJECTED:
                    return getIconLabel(table, type, mode, "review-rejected.png", isSelected, committed);
                case PENDING:
                default:
                    value = "";
            }
        }
        setBackground(table, type, mode, this, isSelected);
        setFont(table.getFont());
        setText(value != null ? value.toString() : "");
        return this;
    }

    private void setBackground(JTable table, ChangeType type, ChangeMode mode, Component c, boolean isSelected) {
        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        }
        else if(type.getDisplayColor().isPresent()) {
            c.setBackground(type.getDisplayColor().get());
            c.setForeground(GuiUtils.UNSELECTED_FOREGROUND);
        }
        else {
            if (mode.equals(ChangeMode.ADDITION)) {
                c.setBackground(GuiUtils.ADDITION_COLOR);
            } else if (mode.equals(ChangeMode.REMOVAL)) {
                c.setBackground(GuiUtils.REMOVAL_COLOR);
            } else if (mode.equals(ChangeMode.ONTOLOGY_IRI)) {
                c.setBackground(GuiUtils.DEFAULT_CHANGE_COLOR);
            }
            c.setForeground(GuiUtils.UNSELECTED_FOREGROUND);
        }
    }

    private JLabel getIconLabel(JTable table, ChangeType type, ChangeMode mode, String iconFilename, boolean isSelected, boolean committed) {
        Icon icon;
        if(committed) {
            icon = GuiUtils.getUserIcon(iconFilename, 30, 30);
        }
        else {
            ImageIcon base = (ImageIcon)GuiUtils.getUserIcon(iconFilename, 30, 30);
            ImageIcon badge = (ImageIcon) GuiUtils.getUserIcon("review-new.png", 13, 13);
            BufferedImage img = new BufferedImage(30, 30, AlphaComposite.SRC_OVER);
            Graphics g = img.getGraphics();
            g.drawImage(base.getImage(), 0, 0, null);
            g.drawImage(badge.getImage(), 17, 0, null);
            icon = new ImageIcon(img);
        }
        JLabel lbl = new JLabel("", icon, JLabel.CENTER);
        lbl.setOpaque(true);
        setBackground(table, type, mode, lbl, isSelected);
        return lbl;
    }
}
