package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.transport.rmi.RemoteLoginService;
import org.protege.editor.owl.server.transport.rmi.RmiLoginService;
import org.protege.editor.owl.server.versioning.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.UIHelper;

import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.UserId;

public class OpenFromServerDialog extends JDialog {

    private static final long serialVersionUID = -6710802337675443598L;

    private static String currentPassword = null;

    private ClientSession clientRegistry;
    private JButton cancelButton;
    private JButton connectButton;
    private JButton openButton;
    private JComboBox<String> serverLocationsList;
    private JPanel mainPanel;
    private AugmentedJTextField registryPort;
    private JPasswordField password;
    private JTable serverContentTable;
    private JTextField username;
    private OWLEditorKit editorKit;
    private ServerTableModel tableModel;

    public OpenFromServerDialog(ClientSession clientRegistry) {
        this.clientRegistry = clientRegistry;
        setTitle("Open from Protege OWL Server");
        setPreferredSize(new Dimension(650, 650));
        setModal(true);
        setResizable(true);
        initUI();
    }

    private void initUI() {
        mainPanel = new JPanel(new GridBagLayout());

        // gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady
        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
                GridBagConstraints.NONE, new Insets(12, 12, 0, 11), 0, 0);
        JLabel serverIRILabel = new JLabel("Server address:");
        mainPanel.add(serverIRILabel, c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 0, 12);
        mainPanel.add(getServerLocationsList(), c);

        JLabel registryPortLabel = new JLabel("Registry port:");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        mainPanel.add(registryPortLabel, c);

        registryPort = new AugmentedJTextField(70, "(optional)");
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(11, 0, 0, 12);
        mainPanel.add(registryPort, c);

        JLabel usernameLabel = new JLabel("Username:");
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        mainPanel.add(usernameLabel, c);

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
        mainPanel.add(username, c);

        JLabel passwordlabel = new JLabel("Password:");
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 12, 0, 11);
        mainPanel.add(passwordlabel, c);

        password = new JPasswordField("");
        password.setColumns(50);
        if (currentPassword != null) {
            password.setText(currentPassword);
        }
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(11, 0, 0, 12);
        mainPanel.add(password, c);

        connectButton = new JButton("Connect to server");
        connectButton.addActionListener(new ConnectServerActionListener());
        c.gridy = 4;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(11, 0, 0, 0);
        mainPanel.add(connectButton, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(11, 12, 12, 12);
        c.weightx = 1.0;
        c.weighty = 1.0;
        mainPanel.add(getServerContentPanel(), c);

        c.gridy = 6;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(getButtonPanel(), c);

        getRootPane().setDefaultButton(connectButton);
        pack();
        getContentPane().add(mainPanel);

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
                OpenFromServerDialog.this.setVisible(false);
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

    private class ConnectServerActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent evt) {
            String serverLocation = (String) serverLocationsList.getSelectedItem();
            try {
                // TODO Make it switchable for different transport implementation
                int registryPortNumber = -1;
                if (!registryPort.getText().trim().isEmpty()) {
                    registryPortNumber = Integer.parseInt(registryPort.getText());
                }
                RemoteLoginService loginService = (RemoteLoginService) ServerUtils
                        .getRemoteService(serverLocation, registryPortNumber, RmiLoginService.LOGIN_SERVICE);
                DefaultUserAuthenticator authenticator = new DefaultUserAuthenticator(loginService);

                MetaprojectFactory f = Manager.getFactory();
                UserId userId = f.getUserId(username.getText());
                PlainPassword plainPassword = f.getPlainPassword(new String(password.getPassword()));

                AuthToken authToken = authenticator.hasValidCredentials(userId, plainPassword);
                Client client = new LocalClient(authToken, serverLocation);
                clientRegistry.setActiveClient(client);

                saveServerConnectionData();
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
                Client client = clientRegistry.getActiveClient();
                ServerDocument serverDocument = client.openProject(pid);
                OWLOntology ontology = ClientUtils.buildOntology(serverDocument, createEmptyMutableOntology());
                VersionedOWLOntology vont = ClientUtils.constructVersionedOntology(serverDocument, ontology);
                clientRegistry.addVersionedOntology(vont);
                editorKit.getOWLModelManager().setActiveOntology(ontology);
                OpenFromServerDialog.this.setVisible(false);
            }
            else {
                JOptionPane.showMessageDialog(mainPanel, "No project was selected", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception ex) {
            ErrorLogPanel.showErrorDialog(ex);
        }
    }

    private OWLMutableOntology createEmptyMutableOntology() throws OWLOntologyCreationException {
        return (OWLMutableOntology) editorKit.getOWLModelManager().getOWLOntologyManager().createOntology();
    }
}
