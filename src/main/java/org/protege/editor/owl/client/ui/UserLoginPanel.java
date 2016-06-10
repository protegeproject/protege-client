package org.protege.editor.owl.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;

import edu.stanford.protege.metaproject.api.UserAuthenticator;

public class UserLoginPanel extends JPanel {

    private static final long serialVersionUID = 9204192451059622356L;

    private static String currentPassword = null;

    private ClientSession clientSession;

    private OWLEditorKit editorKit;

    private JComboBox<String> cmbServerList;
    //private JTextField txtRegistryPort;
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    private JButton btnLogin;
    private JButton btnCancel;

    public UserLoginPanel(ClientSession clientSession, OWLEditorKit editorKit) {
        this.clientSession = clientSession;
        this.editorKit = editorKit;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel pnlLogin = new JPanel();
        pnlLogin.setLayout(new GridLayout(4, 1));

        JPanel pnlServerAddress = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblServerAddress = new JLabel("Server address:");
        pnlServerAddress.add(lblServerAddress);
        pnlServerAddress.add(getServerLocationsList());
        pnlLogin.add(pnlServerAddress);

        /**
        JPanel pnlRegistryPort = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblRegistryPort = new JLabel("Registry port:");
        pnlRegistryPort.add(lblRegistryPort);
        txtRegistryPort = new JTextField("", 15);
        pnlRegistryPort.add(txtRegistryPort);
        pnlLogin.add(pnlRegistryPort);
        **/

        JPanel pnlUsername = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblUsername = new JLabel("Username:");
        pnlUsername.add(lblUsername);
        txtUsername = new JTextField("", 25);
        ClientPreferences prefs = ClientPreferences.getInstance();
        String currentUsername = prefs.getCurrentUsername();
        if (currentUsername != null) {
            txtUsername.setText(currentUsername);
        }
        pnlUsername.add(txtUsername);
        pnlLogin.add(pnlUsername);

        JPanel pnlPassword = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblPassword = new JLabel("Password:");
        pnlPassword.add(lblPassword);
        txtPassword = new JPasswordField("", 25);
        if (currentPassword != null) {
            txtPassword.setText(currentPassword);
        }
        pnlPassword.add(txtPassword);
        pnlLogin.add(pnlPassword);

        add(pnlLogin, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0)); // padding-top

        btnLogin = new JButton("Login");
        btnLogin.setSelected(true);
        btnLogin.addActionListener(new LoginActionListener());
        pnlButtons.add(btnLogin);

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> {
            closeDialog();
        });
        pnlButtons.add(btnCancel);

        add(pnlButtons, BorderLayout.SOUTH);
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

    private void saveServerConnectionData() {
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

    private class LoginActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            String serverAddress = (String) cmbServerList.getSelectedItem();
            //String registryStr = txtRegistryPort.getText().trim();
            //Integer registryPort = !registryStr.isEmpty() ? Integer.parseInt(registryStr) : null;
            try {
                if (!serverAddress.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                	
                	LocalHttpClient client = new LocalHttpClient(username, password, serverAddress);
                    clientSession.setActiveClient(client);
                    clientSession.addListener(client);
                                        
                    saveServerConnectionData();
                    closeDialog();
                }
                else {
                    setUIFeedback(serverAddress.isEmpty(),
                            username.isEmpty(),
                            password.isEmpty());
                }
            }
            catch (Exception e) {
                JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Error connecting to server",
                        new JLabel("Connection failed: " + e.getCause().getMessage()),
                        JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
            }
        }

        private void setUIFeedback(boolean isServerAddressEmpty, 
                boolean isUsernameEmpty, boolean isPasswordEmpty) {
            if (isServerAddressEmpty) {
                cmbServerList.setBackground(Color.YELLOW);
            }
            else {
                cmbServerList.setBackground(Color.WHITE);
            }
            
            if (isUsernameEmpty) {
                txtUsername.setBackground(Color.YELLOW);
            }
            else {
                txtUsername.setBackground(Color.WHITE);
            }
            if (isPasswordEmpty) {
                txtPassword.setBackground(Color.YELLOW);
            }
            else {
                txtPassword.setBackground(Color.WHITE);
            }
        }

        private UserAuthenticator setupAuthenticator(String serverAddress, Integer registryPort) throws RemoteException {
            RemoteLoginService loginService = (RemoteLoginService) ServerUtils
                   .getRemoteService(serverAddress, registryPort, RmiLoginService.LOGIN_SERVICE);
            return new DefaultUserAuthenticator(loginService);
        }
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(UserLoginPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
