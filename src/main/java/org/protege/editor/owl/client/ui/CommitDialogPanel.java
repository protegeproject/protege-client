package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class CommitDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = 8821779954993388044L;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private boolean currentlyValid = false;
    private JTextArea commentArea;

    /**
     * Constructor
     */
    public CommitDialogPanel() {
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout(0, 8));
        commentArea = new JTextArea(4, 45);
        add(new JLabel("Commit message (do not leave blank):"), BorderLayout.NORTH);
        add(commentArea, BorderLayout.CENTER);
        commentArea.addAncestorListener(new RequestFocusListener());
        commentArea.getDocument().addDocumentListener(txtAreaListener);
    }

    private DocumentListener txtAreaListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            checkInput();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkInput();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkInput();
        }
    };

    public JTextArea getTextArea() {
        return commentArea;
    }

    private void checkInput() {
        setValid(!commentArea.getText().isEmpty());
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners){
            l.verifiedStatusChanged(currentlyValid);
        }
    }

    @Override
    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.add(listener);
        listener.verifiedStatusChanged(currentlyValid);
    }

    @Override
    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.remove(listener);
    }


}
