package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class GuiUtils {

    public static final Color
            BORDER_COLOR = new Color(220, 220, 220);

    public static final Border
            MATTE_BORDER = new MatteBorder(1, 1, 1, 1, BORDER_COLOR);


    public static Icon getIcon(String filename, int width, int height) {
        ClassLoader classLoader = GuiUtils.class.getClassLoader();
        return getIcon(classLoader, filename, width, height);
    }

    public static Icon getIcon(ClassLoader classLoader, String filename, int width, int height) {
        BufferedImage icon = null;
        try {
            URL url = classLoader.getResource(checkNotNull(filename));
            if(url != null) {
                icon = ImageIO.read(url);
            }
        } catch (IOException | NullPointerException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        if(icon != null) {
            return new ImageIcon(icon.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } else {
            return new ImageIcon();
        }
    }

}
