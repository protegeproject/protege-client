package org.protege.editor.owl.client.admin.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AugmentedJTextArea extends JTextArea {
    public static final Color DEFAULT_GHOST_TEXT_COLOR;
    private String errorMessage = "";
    private int errorLocation = -1;
    private String ghostText = "";

    public AugmentedJTextArea(String ghostText) {
        this.ghostText = ghostText;
    }

    public AugmentedJTextArea(String text, String ghostText) {
        super(text);
        this.ghostText = ghostText;
    }

    public AugmentedJTextArea(int rows, int columns, String ghostText) {
        super(rows, columns);
        this.ghostText = ghostText;
    }

    public AugmentedJTextArea(String text, int rows, int columns, String ghostText) {
        super(text, rows, columns);
        this.ghostText = ghostText;
    }

    public AugmentedJTextArea(Document doc, String text, int rows, int columns, String ghostText) {
        super(doc, text, rows, columns);
        this.ghostText = ghostText;
    }

    public int getErrorLocation() {
        return this.errorLocation;
    }

    public void setErrorLocation(int errorLocation) {
        if (this.errorLocation != errorLocation) {
            this.errorLocation = errorLocation;
            this.repaint();
        }
    }

    public void clearErrorLocation() {
        if (this.errorLocation != -1) {
            this.errorLocation = -1;
            this.repaint();
        }
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getGhostText() {
        return this.ghostText;
    }

    public void setGhostText(String ghostText) {
        if (!this.ghostText.equals(ghostText)) {
            this.ghostText = ghostText;
            this.repaint();
        }
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.repaint();
    }

    public void clearErrorMessage() {
        if (!this.errorMessage.isEmpty()) {
            this.errorMessage = "";
            this.repaint();
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color oldColor = g.getColor();

        try {
            Rectangle rect;
            if (this.errorLocation != -1) {
                g.setColor(Color.PINK);
                Rectangle e = this.modelToView(this.errorLocation);
                rect = this.modelToView(this.errorLocation + 1);
                g.fillRect(e.x, e.y, rect.x - e.x, e.height);
            }

            int e1;
            if (this.getText().isEmpty()) {
                g.setColor(DEFAULT_GHOST_TEXT_COLOR);
                e1 = this.getBaseline(this.getWidth(), this.getHeight());
                Insets rect1 = this.getInsets();
                g.drawString(this.ghostText, rect1.left, e1);
            }

            if (!this.errorMessage.isEmpty()) {
                e1 = this.getBaseline(this.getWidth(), this.getHeight());
                rect = this.modelToView(this.getText().length());
                g.setColor(Color.PINK);
                g.drawString(this.errorMessage, rect.x + 20, e1);
            }
        } catch (BadLocationException var6) {
            System.err.println(var6.getMessage());
        }
        g.setColor(oldColor);
    }

    static {
        DEFAULT_GHOST_TEXT_COLOR = Color.LIGHT_GRAY;
    }
}



