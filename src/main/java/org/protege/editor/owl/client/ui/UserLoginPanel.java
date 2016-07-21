package org.protege.editor.owl.client.ui;

import edu.stanford.protege.metaproject.api.AuthToken;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.diff.ui.GuiUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class UserLoginPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = 7096961238522927373L;
    private static final int FIELD_WIDTH = 20;
    private ClientSession clientSession;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private JLabel lblServerAddress, lblUsername, lblPassword;
    private AugmentedJTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbServerList;
    private boolean currentlyValid = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();

    /**
     * Constructor
     *
     * @param clientSession Client session
     */
    public UserLoginPanel(ClientSession clientSession) {
        this.clientSession = checkNotNull(clientSession);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        lblServerAddress = new JLabel("Server address:");
        lblUsername = new JLabel("Username:");
        lblPassword = new JLabel("Password:");

        cmbServerList = getServerLocationsList();
        txtUsername = new AugmentedJTextField(FIELD_WIDTH, "User name");
        txtPassword = new JPasswordField(FIELD_WIDTH);
        txtPassword.setBorder(GuiUtils.MATTE_BORDER);

        ClientPreferences prefs = ClientPreferences.getInstance();
        String currentUsername = prefs.getCurrentUsername();
        if (currentUsername != null) {
            txtUsername.setText(currentUsername);
        }
        addListener(txtUsername);
        addListener(txtPassword);
        cmbServerList.addActionListener(e -> handleValueChange());
    }

    private void initUi() {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);
        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;
        holderPanel.add(lblServerAddress, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, new Insets(15,2,2,2), 0, 0));
        holderPanel.add(cmbServerList, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(new JSeparator(), new GridBagConstraints(0, rowIndex, 2, 1, 100.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 2), 0, 0));
        rowIndex++;
        holderPanel.add(lblUsername, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(txtUsername, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(lblPassword, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(txtPassword, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
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

    private JComboBox<String> getServerLocationsList() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        cmbServerList = new JComboBox<>(model);
        cmbServerList.setEditable(true);

        ClientPreferences prefs = ClientPreferences.getInstance();
        ArrayList<String> serverLocations = new ArrayList<String>(prefs.getServerLocations());

        Collections.sort(serverLocations);
        for (String serverLocation : serverLocations) {
            cmbServerList.addItem(serverLocation);
        }
        String lastLocation = prefs.getLastServerLocation();
        if (serverLocations.contains(lastLocation)) {
            cmbServerList.setSelectedItem(lastLocation);
        }
        return cmbServerList;
    }

    public void saveServerConnectionData() {
        ClientPreferences prefs = ClientPreferences.getInstance();

        // Save server location information
        ArrayList<String> serverLocations = new ArrayList<String>();
        String serverLocation = (String) cmbServerList.getEditor().getItem();
        if (((DefaultComboBoxModel<String>) cmbServerList.getModel()).getIndexOf(serverLocation) == -1) {
            cmbServerList.addItem(serverLocation);
        }
        int count = cmbServerList.getItemCount();
        for (int i = 0; i < count; i++) {
            serverLocations.add((String) cmbServerList.getItemAt(i));
        }

        // Save which server was last connected to
        prefs.setServerLocations(serverLocations);
        prefs.setLastServerLocation((String) cmbServerList.getSelectedItem());

        // Save username
        prefs.setCurrentUsername(txtUsername.getText());
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleValueChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleValueChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleValueChange();
            }
        });
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (NumberFormatException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if(cause != null) {
                errorArea.setText(cause.getMessage());
            }
            else {
                errorArea.setText(e.toString());
            }
        }
    }

    private boolean checkInputs() throws NumberFormatException {
        boolean allValid = true;
        if(txtUsername.getText().trim().isEmpty()) {
            allValid = false;
        }
        if(txtPassword.getPassword().length == 0) {
            allValid = false;
        }
        if(cmbServerList.getSelectedItem() == null || cmbServerList.getSelectedItem().equals("")) {
            allValid = false;
        }
        return allValid;
    }

    public AuthToken authenticateUser() throws Exception {
        String serverAddress = (String)cmbServerList.getSelectedItem();
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        
        LocalHttpClient client = new LocalHttpClient(username, password, serverAddress);
        clientSession.setActiveClient(client);
        clientSession.addListener(client);
        
        return client.getAuthToken();
    }

    public static Optional<AuthToken> showDialog(OWLEditorKit editorKit, JComponent parent) {
        ClientSession clientSession = ClientSession.getInstance(editorKit);
        UserLoginPanel userLoginPanel = new UserLoginPanel(clientSession);
        int res = JOptionPaneEx.showValidatingConfirmDialog(
                parent, "Login to Protege OWL Server", userLoginPanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (res == JOptionPane.CANCEL_OPTION) {
            return Optional.empty();
        }
        if (res == JOptionPane.OK_OPTION) {
            try {
                AuthToken authToken = userLoginPanel.authenticateUser();
                userLoginPanel.saveServerConnectionData();
                return Optional.of(authToken);
            } catch (Exception e) {
                String msg;
                if (e.getCause() != null) {
                    msg = e.getCause().getMessage();
                } else {
                    msg = e.getMessage();
                }
                JOptionPaneEx.showConfirmDialog(parent, "Error connecting to server",
                        new JLabel("Connection failed: " + msg),
                        JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
            }
        }
        return Optional.empty();
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
