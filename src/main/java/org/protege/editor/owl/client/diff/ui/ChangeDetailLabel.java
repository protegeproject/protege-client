package org.protege.editor.owl.client.diff.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailLabel extends JLabel {

    public ChangeDetailLabel(String label) {
        super(label);
        setFont(GuiUtils.DEFAULT_FONT);
        setForeground(new Color(84, 84, 84));
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(new EmptyBorder(10, 10, 5, 0));
    }

}