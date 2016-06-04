package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Host;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class HostDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField uri, port;
    private JLabel uriLbl, portLbl;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH * 2);
    private boolean currentlyValid = false, uriChanged = false, portChanged = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();

    /**
     * Constructor
     */
    public HostDialogPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        uri = new AugmentedJTextField(FIELD_WIDTH, "Host URI");
        port = new AugmentedJTextField(FIELD_WIDTH, "Registry port (e.g., for RMI)");

        uriLbl = new JLabel("URI:");
        portLbl = new JLabel("Registry Port:");

        addListener(uri);
        addListener(port);
    }

    private void initUi() {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);

        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;

        holderPanel.add(uriLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(uri, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(portLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(port, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;

        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        holderPanel.add(errorArea, new GridBagConstraints(0, rowIndex, 2, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    private void setIsEditing(Host host) {
        uri.setText(host.getUri().toString());
        if(host.getSecondaryPort().isPresent()) {
            port.setText(host.getSecondaryPort().get().get().toString());
        }
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if(field.equals(uri)) {
                    uriChanged = true;
                } else if(field.equals(port)) {
                    portChanged = true;
                }
                handleValueChange();
            }

            public void removeUpdate(DocumentEvent e) {
                if(field.equals(uri)) {
                    uriChanged = true;
                } else if(field.equals(port)) {
                    portChanged = true;
                }
                handleValueChange();
            }

            public void changedUpdate(DocumentEvent e) {
                if(field.equals(uri)) {
                    uriChanged = true;
                } else if(field.equals(port)) {
                    portChanged = true;
                }
                handleValueChange();
            }
        });
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (URISyntaxException | NumberFormatException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if (cause != null) {
                errorArea.setText(cause.getMessage());
            } else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws NumberFormatException, URISyntaxException {
        boolean allValid = true;
        if (uri.getText().trim().isEmpty()) {
            allValid = false;
        } else {
            new URI(uri.getText());
        }
        if (!port.getText().isEmpty()) {
            Integer.parseInt(port.getText().trim());
        }
        return allValid;
    }

    private void updateHost() {
        try {
            URI newUri = new URI(uri.getText());
            int secondaryPort = -1;
            if (!port.getText().trim().isEmpty()) {
                secondaryPort = Integer.parseInt(port.getText());
            }
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            Logger logger = LoggerFactory.getLogger(HostDialogPanel.class.getName());
            if (uriChanged) {
                logger.info("Changing URI");
                client.setHostAddress(newUri);
            }
            if (portChanged && secondaryPort != -1) {
                logger.info("Changing secondary Port");
                client.setSecondaryPort(secondaryPort);
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static boolean showDialog(OWLEditorKit editorKit, Host host) {
        HostDialogPanel panel = new HostDialogPanel(editorKit);
        panel.setIsEditing(host);
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), "Edit Host '" + host.getUri().toString() + "'", panel,
                JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            panel.updateHost();
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
