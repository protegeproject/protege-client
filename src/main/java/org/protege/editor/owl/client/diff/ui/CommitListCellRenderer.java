package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.Commit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setBorder(new EmptyBorder(7, 7, 7, 3));
        label.setIcon(GuiUtils.getUserIcon("commit.png", 17, 17));
        label.setIconTextGap(9);

        Commit c = (Commit) value;
        label.setToolTipText("Comment: " + c.getComment());

        Date date = c.getDate();
        String dateStr = new SimpleDateFormat("MM/dd/yyyy HH:mm").format(date);
        label.setText("<html><strong>" + dateStr + " - " + c.getUserId().getUserName() +
                "</strong><br><p style=\"padding-top:3\"><nobr>" + c.getComment() + "</nobr></p></html>");
        return label;
    }

}
