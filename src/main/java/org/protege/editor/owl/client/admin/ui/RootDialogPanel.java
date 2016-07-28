package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RootDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = -2157297160417578629L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField root;
    private JLabel rootLbl;
    private boolean currentlyValid = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();

    /**
     * Constructor
     */
    public RootDialogPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        root = new AugmentedJTextField(FIELD_WIDTH, "Server root folder");
        rootLbl = new JLabel("Root:");
        addListener(root);
    }

    private void initUi() {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);
        holderPanel.add(rootLbl, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING,
                GridBagConstraints.NONE, new Insets(20, 2, 2, 2), 0, 0));
        holderPanel.add(root, new GridBagConstraints(1, 0, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING,
                GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 2), 0, 0));
    }

    private void setIsEditing(String root) {
        this.root.setText(root);
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                setValid(checkInputs());
            }

            public void removeUpdate(DocumentEvent e) {
                setValid(checkInputs());
            }

            public void changedUpdate(DocumentEvent e) {
                setValid(checkInputs());
            }
        });
    }

    private boolean checkInputs() {
        boolean allValid = true;
        if (root.getText().trim().isEmpty()) {
            allValid = false;
        }
        return allValid;
    }

    private void updateRoot() {
        try {
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            client.setRootDirectory(root.getText());
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    public static boolean showDialog(OWLEditorKit editorKit, String root) {
        RootDialogPanel panel = new RootDialogPanel(editorKit);
        panel.setIsEditing(root);
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), "Edit Root '" + root + "'", panel,
                JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            panel.updateRoot();
            return true;
        }
        return false;
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners) {
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
