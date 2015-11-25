package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.ChangeMode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class GuiUtils {

    /* constants */

    public static final Color
            ADDITION_COLOR = new Color(219, 255, 219),
            REMOVAL_COLOR = new Color(255, 236, 236),
            DEFAULT_CHANGE_COLOR = new Color(255, 251, 237),
            WHITE_BACKGROUND = Color.WHITE,
            UNSELECTED_FOREGROUND = Color.BLACK,
            BORDER_COLOR = new Color(220, 220, 220);

    public static final Border
            MATTE_BORDER = new MatteBorder(1, 1, 1, 1, BORDER_COLOR),
            EMPTY_BORDER = new EmptyBorder(0,0,0,0);

    public static final Font
            DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    public static final String
            USER_ICON_FILENAME = "user.png",
            USERS_ICON_FILENAME = "users.png",
            COMMIT_ICON_FILENAME = "commit.png",
            STRING_ICON_FILENAME = "string.png",
            ONTOLOGY_ICON_FILENAME = "ontology.png",
            WARNING_ICON_FILENAME = "warning.png",
            REVIEW_ACCEPTED_ICON_FILENAME = "review-accepted.png",
            REVIEW_REJECTED_ICON_FILENAME = "review-rejected.png",
            REVIEW_CLEAR_ICON_FILENAME = "review-clear.png",
            REVIEW_COMMIT_ICON_FILENAME = "review-commit.png",
            REVIEW_PENDING_ICON_FILENAME = "review-pending.png",
            NEW_REVIEW_ICON_FILENAME = "review-new.png";


    /* methods */

    public static Icon getUserIcon(String filename, int width, int height) {
        BufferedImage icon = null;
        ClassLoader classLoader = GuiUtils.class.getClassLoader();
        try {
            icon = ImageIO.read(checkNotNull(classLoader.getResource(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Image img = icon.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    public static void setComponentBackground(Component c, ChangeMode mode) {
        if (mode.equals(ChangeMode.ADDITION)) {
            c.setBackground(GuiUtils.ADDITION_COLOR);
        } else if (mode.equals(ChangeMode.REMOVAL)) {
            c.setBackground(GuiUtils.REMOVAL_COLOR);
        } else if (mode.equals(ChangeMode.ONTOLOGY_IRI)) {
            c.setBackground(GuiUtils.DEFAULT_CHANGE_COLOR);
        }
    }
}
