package org.protege.editor.owl.client.diff.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
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

    public static final MatteBorder MATTE_BORDER = new MatteBorder(1, 1, 1, 1, BORDER_COLOR);

    public static final EmptyBorder EMPTY_BORDER = new EmptyBorder(0,0,0,0);

    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);


    /* methods */

    public static Icon getUserIcon(String filename, int width, int height) {
        BufferedImage icon = null;
        ClassLoader classLoader = AuthorListCellRenderer.class.getClassLoader();
        try {
            icon = ImageIO.read(checkNotNull(classLoader.getResource(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Image img = icon.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
