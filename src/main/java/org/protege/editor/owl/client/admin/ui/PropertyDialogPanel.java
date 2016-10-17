package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.exception.PropertyAlreadyExistsException;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.Config;
import org.protege.editor.owl.ui.UIHelper;

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
public class PropertyDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = 5311938044842568436L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField name, value;
    private JLabel nameLbl, valueLbl;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH * 2);
    private boolean currentlyValid = false, isEditing = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private String selectedPropertyName;

    /**
     * Constructor
     *
     * @param editorKit OWL Editor Kit
     */
    public PropertyDialogPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        name = new AugmentedJTextField(FIELD_WIDTH, "Property name");
        value = new AugmentedJTextField(FIELD_WIDTH, "Property value");

        nameLbl = new JLabel("Name:");
        valueLbl = new JLabel("Value:");

        addListener(name);
        addListener(value);
    }

    private void initUi() {
        setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel, BorderLayout.CENTER);

        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;

        holderPanel.add(nameLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(name, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(valueLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(value, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;

        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        holderPanel.add(errorArea, new GridBagConstraints(0, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.LAST_LINE_START, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    private void setIsEditing(String propertyName, String propertyValue) {
        selectedPropertyName = checkNotNull(propertyName);
        isEditing = true;

        name.setText(propertyName);
        value.setText(propertyValue);
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void removeUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void changedUpdate(DocumentEvent e) {
                handleValueChange();
            }
        });
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (PropertyAlreadyExistsException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if (cause != null) {
                errorArea.setText(cause.getMessage());
            } else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws PropertyAlreadyExistsException {
        boolean allValid = true;
        if (name.getText().trim().isEmpty()) {
            allValid = false;
        }
        if ((!isEditing && exists(name.getText())) || (isEditing && !name.getText().equals(selectedPropertyName) && exists(name.getText()))) {
            throw new PropertyAlreadyExistsException("A property with the given property name already exists");
        }
        return allValid;
    }

    private boolean exists(String propertyName) {
        Config config = ClientSession.getInstance(editorKit).getActiveClient().getConfig();
        boolean exists = false;
        exists = config.getServerProperties().containsKey(propertyName);
        return exists;
    }

    private void addProperty() {
        Config config = ClientSession.getInstance(editorKit).getActiveClient().getConfig();
        try {
            config.setServerProperty(name.getText(), value.getText());
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void updateProperty(String propertyName) {
        if (propertyName.equals(name.getText())) {
            addProperty();
        } else { // property key changed
            Config config = ClientSession.getInstance(editorKit).getActiveClient().getConfig();
            try {
                config.unsetServerProperty(propertyName);
                addProperty();
            } catch (AuthorizationException | ClientRequestException e) {
                ErrorLogPanel.showErrorDialog(e);
            }
        }
    }

    public static boolean showDialog(OWLEditorKit editorKit) {
        PropertyDialogPanel panel = new PropertyDialogPanel(editorKit);
        int response = new UIHelper(editorKit).showValidatingDialog("Add New Property", panel, null);
        if (response == JOptionPane.OK_OPTION) {
            panel.addProperty();
            return true;
        }
        return false;
    }

    public static boolean showDialog(OWLEditorKit editorKit, ServerSettingsPanel.PropertyListItem propertyListItem) {
        PropertyDialogPanel panel = new PropertyDialogPanel(editorKit);
        panel.setIsEditing(propertyListItem.getPropertyName(), propertyListItem.getPropertyValue());
        int response = new UIHelper(editorKit).showValidatingDialog("Edit Property '" + propertyListItem.getPropertyName() + "'", panel, null);
        if (response == JOptionPane.OK_OPTION) {
            panel.updateProperty(propertyListItem.getPropertyName());
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
