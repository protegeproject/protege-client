package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.OWLClientException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.UIHelper;

import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.UserId;

public class OpenFromServerPanel extends JPanel {

    private static final long serialVersionUID = -6710802337675443598L;

    private static String currentPassword = null;

    private ClientSession clientSession;

    private OWLEditorKit editorKit;
    private OWLOntologyManager owlManager;

    private JButton cancelButton;
    private JButton connectButton;
    private JButton openButton;
    private JComboBox<String> serverLocationsList;
    private AugmentedJTextField registryPort;
    private JPasswordField password;
    private JTable serverContentTable;
    private JTextField username;
    private ServerTableModel tableModel;

    public OpenFromServerPanel(ClientSession clientSession, OWLEditorKit editorKit) {
        this.clientSession = clientSession;
        this.editorKit = editorKit;
        owlManager = editorKit.getOWLModelManager().getOWLOntologyManager();

        setLayout(new GridBagLayout());

        // gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady
        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
                GridBagConstraints.NONE, new Insets(12, 12, 0, 11), 0, 0);
        JLabel serverIRILabel = new JLabel("Server address:");
        add(serverIRILabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 0, 12);
        add(getServerLocationsList(), c);

        JLabel registryPortLabel = new JLabel("Registry port:");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        add(registryPortLabel, c);

        registryPort = new AugmentedJTextField(70, "(optional)");
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(11, 0, 0, 12);
        add(registryPort, c);

        JLabel usernameLabel = new JLabel("Username:");
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        add(usernameLabel, c);

        username = new JTextField("");
        username.setColumns(50);
        ClientPreferences prefs = ClientPreferences.getInstance();
        String currentUsername = prefs.getCurrentUsername();
        if (currentUsername != null) {
            username.setText(currentUsername);
        }
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(11, 0, 0, 12);
        add(username, c);

        JLabel passwordlabel = new JLabel("Password:");
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        add(passwordlabel, c);

        password = new JPasswordField("");
        password.setColumns(50);
        if (currentPassword != null) {
            password.setText(currentPassword);
        }
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(11, 0, 0, 12);
        add(password, c);

        connectButton = new JButton("Connect to server");
        connectButton.setSelected(true);
        connectButton.addActionListener(new ConnectServerActionListener());
        c.gridy = 4;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 0, 0, 0);
        add(connectButton, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(11, 12, 12, 12);
        c.weightx = 1.0;
        c.weighty = 1.0;
        add(getServerContentPanel(), c);

        c.gridy = 6;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        add(getButtonPanel(), c);

        if (password.getPassword().length == 0) {
            password.requestFocus();
        }
        else {
            connectButton.requestFocus();
        }
    }

    private JComboBox<String> getServerLocationsList() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        serverLocationsList = new JComboBox<>(model);
        serverLocationsList.setEditable(true);

        ClientPreferences prefs = ClientPreferences.getInstance();
        ArrayList<String> serverLocations = new ArrayList<String>(prefs.getServerLocations());

        Collections.sort(serverLocations);
        for (String serverLocation : serverLocations) {
            serverLocationsList.addItem(serverLocation);
        }
        String lastLocation = prefs.getLastServerLocation();
        if (serverLocations.contains(lastLocation)) {
            serverLocationsList.setSelectedItem(lastLocation);
        }
        return serverLocationsList;
    }

    private JPanel getServerContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        CompoundBorder border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Server Content"),
                BorderFactory.createEmptyBorder(12, 12, 11, 11));
        panel.setBorder(border);

        tableModel = new ServerTableModel();
        serverContentTable = new JTable(tableModel);
        serverContentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openOntologyDocument();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(serverContentTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel getButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        openButton = new JButton("Open");
        openButton.addActionListener(new OpenActionListener());
        panel.add(openButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
        panel.add(cancelButton);
        return panel;
    }

    private void saveServerConnectionData() {
        ClientPreferences prefs = ClientPreferences.getInstance();

        // Save server location information
        ArrayList<String> serverLocations = new ArrayList<String>();

        String serverLocation = (String) serverLocationsList.getEditor().getItem();
        if (((DefaultComboBoxModel<String>) serverLocationsList.getModel()).getIndexOf(serverLocation) == -1) {
            serverLocationsList.addItem(serverLocation);
        }

        int count = serverLocationsList.getItemCount();
        for (int i = 0; i < count; i++) {
            serverLocations.add((String) serverLocationsList.getItemAt(i));
        }

        // Save which server was last connected to
        prefs.setServerLocations(serverLocations);
        prefs.setLastServerLocation((String) serverLocationsList.getSelectedItem());

        // Save username
        prefs.setCurrentUsername(username.getText());
    }

    private void loadProjectList(Client client) {
        try {
            tableModel.initialize(client);
        }
        catch (OWLClientException e) {
            ErrorLogPanel.showErrorDialog(e);
            UIHelper ui = new UIHelper(editorKit);
            ui.showDialog("Error opening project", new JLabel("Could not retrieve remote projects: " + e.getMessage()));
        }
    }

    private class ConnectServerActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            String serverAddress = (String) serverLocationsList.getSelectedItem();
            String registryPortStr = registryPort.getText().trim();
            try {
                // TODO Make it switchable for different transport implementation
                Integer registryPort = null;
                if (!registryPortStr.isEmpty()) {
                    registryPort = Integer.parseInt(registryPortStr);
                }
                RemoteLoginService loginService = (RemoteLoginService) ServerUtils
                        .getRemoteService(serverAddress, registryPort, RmiLoginService.LOGIN_SERVICE);
                DefaultUserAuthenticator authenticator = new DefaultUserAuthenticator(loginService);

                MetaprojectFactory f = Manager.getFactory();
                UserId userId = f.getUserId(username.getText());
                PlainPassword plainPassword = f.getPlainPassword(new String(password.getPassword()));

                AuthToken authToken = authenticator.hasValidCredentials(userId, plainPassword);
                Client client = new LocalClient(authToken, serverAddress, registryPort);
                clientSession.setActiveClient(client);

                saveServerConnectionData();
                loadProjectList(client);
            }
            catch (Exception e) {
                ErrorLogPanel.showErrorDialog(e);
                UIHelper ui = new UIHelper(editorKit);
                ui.showDialog("Error connecting to server", new JLabel("Connection failed: " + e.getMessage()));
            }
        }
    }

    private class OpenActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            openOntologyDocument();
        }
    }

    protected void openOntologyDocument() {
        try {
            int row = serverContentTable.getSelectedRow();
            if (row != -1) {
                ProjectId pid = tableModel.getValueAt(row);
                ServerDocument serverDocument = clientSession.getActiveClient().openProject(pid);
                VersionedOWLOntology vont = ClientUtils.buildVersionedOntology(serverDocument, owlManager);
                editorKit.getOWLModelManager().setActiveOntology(vont.getOntology());
                clientSession.registerProject(pid, vont);
                closeDialog();
            }
            else {
                JOptionPane.showMessageDialog(this, "No project was selected", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception ex) {
            ErrorLogPanel.showErrorDialog(ex);
        }
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(OpenFromServerPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
