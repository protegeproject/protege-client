package org.protege.editor.owl.client.ui;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.UserAuthenticator;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalClient;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;

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
    private static final long serialVersionUID = -6708992419156552723L;
    private static final int FIELD_WIDTH = 20;
    private ClientSession clientSession;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private JLabel lblServerAddress, lblRegistryPort, lblUsername, lblPassword;
    private AugmentedJTextField txtRegistryPort, txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbServerList;
    private boolean currentlyValid = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();

    /**
     * Constructor
     *
     * @param clientSession Client session
     * @param editorKit OWL Editor Kit
     */
    public UserLoginPanel(ClientSession clientSession) {
        this.clientSession = checkNotNull(clientSession);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        lblServerAddress = new JLabel("Server address:");
        lblRegistryPort = new JLabel("Registry port:");
        lblUsername = new JLabel("Username:");
        lblPassword = new JLabel("Password:");

        cmbServerList = getServerLocationsList();
        txtRegistryPort = new AugmentedJTextField(FIELD_WIDTH, "RMI registry port");
        txtUsername = new AugmentedJTextField(FIELD_WIDTH, "User name");
        txtPassword = new JPasswordField(FIELD_WIDTH);
        txtPassword.setBorder(GuiUtils.MATTE_BORDER);

        ClientPreferences prefs = ClientPreferences.getInstance();
        String currentUsername = prefs.getCurrentUsername();
        if (currentUsername != null) {
            txtUsername.setText(currentUsername);
        }

        addListener(txtRegistryPort);
        addListener(txtUsername);
        addListener(txtPassword);
        addListener(txtRegistryPort);
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
        holderPanel.add(lblRegistryPort, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(txtRegistryPort, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
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
        if(txtRegistryPort.getText().isEmpty()) {
            allValid = false;
        } else {
            Integer.parseInt(txtRegistryPort.getText());
        }
        return allValid;
    }

    public AuthToken authenticateUser() throws Exception {
        RemoteLoginService loginService;
        if(!txtRegistryPort.getText().isEmpty()) {
            loginService = (RemoteLoginService) ServerUtils
                    .getRemoteService((String)cmbServerList.getSelectedItem(), Integer.parseInt(txtRegistryPort.getText()), RmiLoginService.LOGIN_SERVICE);
        } else {
            loginService = (RemoteLoginService) ServerUtils
                    .getRemoteService((String)cmbServerList.getSelectedItem(), RmiLoginService.LOGIN_SERVICE);
        }

        UserAuthenticator authenticator = new DefaultUserAuthenticator(loginService);
        AuthToken token = authenticator.hasValidCredentials(Manager.getFactory().getUserId(txtUsername.getText()),
                Manager.getFactory().getPlainPassword(new String(txtPassword.getPassword())));

        if(token.isAuthorized()) {
            LocalClient client = new LocalClient(token, (String) cmbServerList.getSelectedItem(), Integer.parseInt(txtRegistryPort.getText()));
            clientSession.addListener(client);
            clientSession.setActiveClient(client);
        }
        return token;
    }

    public static Optional<AuthToken> showDialog(OWLEditorKit editorKit, JComponent parent) {
        ClientSession clientSession = ClientSession.getInstance(editorKit);
        UserLoginPanel userLoginPanel = new UserLoginPanel(clientSession);

        while (true) {
            int res = JOptionPaneEx.showValidatingConfirmDialog(
                    parent, "Login to Protege OWL Server", userLoginPanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
            if (res == JOptionPane.CANCEL_OPTION) {
                break;
            }
            if (res == JOptionPane.OK_OPTION) {
                try {
                    AuthToken authToken = userLoginPanel.authenticateUser();
                    userLoginPanel.saveServerConnectionData();
                    return Optional.of(authToken);
                }
                catch (Exception e) {
                    JOptionPaneEx.showConfirmDialog(parent, "Error connecting to server",
                            new JLabel("Connection failed: " + e.getCause().getMessage()),
                            JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
                }
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
