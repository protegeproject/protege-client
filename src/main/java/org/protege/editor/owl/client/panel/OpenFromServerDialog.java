package org.protege.editor.owl.client.panel;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.client.*;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.connect.rmi.RMIClient;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

public class OpenFromServerDialog extends JDialog {

	private static final long serialVersionUID = -6710802337675443598L;
	
	private static String currentPassword = null; 
	
	private Client client;
	private JButton cancelButton;
	private JButton connectButton;
	private JButton openButton;
	private JButton uploadButton;
	private JComboBox serverLocationsList;
	private JPanel mainPanel;
	private JPasswordField password;
	private JTable serverContentTable;
	private JTextField username;
	private OWLEditorKit editorKit;
	private RemoteServerDirectory currentDirectory;
	private ServerTableModel tableModel;
	
	public OpenFromServerDialog(OWLEditorKit editorKit) {
		setTitle("Open from Protege OWL Server");
		setPreferredSize(new Dimension(650, 650));
		setModal(true);
		setResizable(true);
		
		this.editorKit = editorKit;
		
		initUI();
		
		if (password.getPassword().length == 0) {
			password.requestFocus();
		} else {
			connectButton.requestFocus();
		}
	}

	private void initUI() {
		mainPanel = new JPanel(new GridBagLayout());
		
		/*
		 * gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady
		 */
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(12, 12, 0, 11), 0, 0);
		JLabel serverIRILabel = new JLabel("Server address:");
		mainPanel.add(serverIRILabel, c);

		c.gridx = 1;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12, 0, 0, 12);
		mainPanel.add(getServerLocationsList(), c);
		
		JLabel usernameLabel = new JLabel("Username:");
		c.gridx = 0;
		c.gridy = 1;
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
		c.gridy = 2;
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
		connectButton.addActionListener(new ConnectToServerActionListener());
		c.gridy = 3;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(11, 0, 0, 0);
		mainPanel.add(connectButton, c);
		
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(11, 12, 12, 12);
		c.weightx = 1.0;
		c.weighty = 1.0;
		mainPanel.add(getServerContentPanel(), c);
		
		c.gridy = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0;
		mainPanel.add(getButtonPanel(), c);

		getRootPane().setDefaultButton(connectButton);
		pack();
		getContentPane().add(mainPanel);
	}
	
	private JComboBox getServerLocationsList() {
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		serverLocationsList = new JComboBox(model);
		serverLocationsList.setEditable(true);

		ClientPreferences prefs = ClientPreferences.getInstance();
		ArrayList<String> serverLocations = new ArrayList<String>(prefs.getServerLocations());

		if (serverLocations.isEmpty()) {
			serverLocationsList.addItem(new String(RMIClient.SCHEME + "://localhost:4875/"));
			serverLocationsList.setSelectedIndex(0);
		} else {
			Collections.sort(serverLocations);
			for (String serverLocation : serverLocations) {
				serverLocationsList.addItem(serverLocation);
			}
			
			String lastLocation = prefs.getLastServerLocation();
			if (serverLocations.contains(lastLocation)) {
				serverLocationsList.setSelectedItem(lastLocation);
			}
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
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
		uploadButton = new JButton("Upload");
		uploadButton.addActionListener(new UploadActionListener());
		buttonPanel.add(uploadButton);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		
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
	
	public void setDirectory(RemoteServerDirectory dir) throws OWLServerException {
	    tableModel.loadServerData(client, dir);
	    currentDirectory = dir;
	}
	
	private void saveServerConnectionData() {
		ClientPreferences prefs = ClientPreferences.getInstance();

		// Save server location information
		ArrayList<String> serverLocations = new ArrayList<String>();
		
		String serverLocation = (String) serverLocationsList.getEditor().getItem();
		if (((DefaultComboBoxModel) serverLocationsList.getModel()).getIndexOf(serverLocation) == -1) {
			serverLocationsList.addItem(serverLocation);
		}
		
		int count = serverLocationsList.getItemCount();
		for (int i=0; i<count; i++) {
			serverLocations.add((String) serverLocationsList.getItemAt(i));
		}
		
		// Save which server was last connected to
		prefs.setServerLocations(serverLocations);
		prefs.setLastServerLocation((String) serverLocationsList.getSelectedItem());
		
		// Save username
		prefs.setCurrentUsername(username.getText());
		
		// Save password
		currentPassword = new String(password.getPassword());
	}

	private class ConnectToServerActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String serverIRI = (String) serverLocationsList.getSelectedItem();
			IRI serverLocation = IRI.create(serverIRI);
			ServerConnectionManager connectionManager = ServerConnectionManager.get(editorKit);
			try {
				client = connectionManager.createClient(serverLocation, username.getText(), new String(password.getPassword()));
				if (client != null) {

					/*
					 * After a successful connection, save server location to the preferences so that users
					 * don't have to retype this information.
					 */
					saveServerConnectionData();
					
					// Do we still need this code?
					RemoteServerDocument dir = client.getServerDocument(serverLocation);
	                if (dir instanceof RemoteServerDirectory) {
	                    setDirectory((RemoteServerDirectory) dir);
	                }
	                
				}
			} catch (OWLServerException ose) {
				ErrorLogPanel.showErrorDialog(ose);
				UIHelper ui = new UIHelper(editorKit);
				ui.showDialog("Error connecting to server", new JLabel("Connection failed - " + ose.getMessage()));
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
			if(row != -1) {
				RemoteServerDocument doc = tableModel.getValueAt(row);
				if (doc instanceof RemoteOntologyDocument) {
					RemoteOntologyDocument remoteOntology = (RemoteOntologyDocument) doc;
					ServerConnectionManager connectionManager = ServerConnectionManager.get(editorKit);
					VersionedOntologyDocument vont = ClientUtilities.loadOntology(client, editorKit.getOWLModelManager().getOWLOntologyManager(), remoteOntology);
					connectionManager.addVersionedOntology(vont);
					editorKit.getOWLModelManager().setActiveOntology(vont.getOntology());
					OpenFromServerDialog.this.setVisible(false);
				}
			}
			else {
				JOptionPane.showMessageDialog(mainPanel, "Select a document from the 'Ontology Documents' list to open.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception ex) {
			ErrorLogPanel.showErrorDialog(ex);
		}
	}
	
	private class UploadActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (client != null) {
				try {
					JRootPane rootPane = OpenFromServerDialog.this.getRootPane();
					File input = UIUtil.openFile(rootPane, "Choose file to upload", "OWL File", UIHelper.OWL_EXTENSIONS);
					if (input != null) {
						OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
						OWLOntology ontology = manager.loadOntologyFromOntologyDocument(input);
						String name = (String) JOptionPane.showInputDialog(rootPane, "Name: ", "Upload Ontology Document", JOptionPane.PLAIN_MESSAGE);
						if ((name != null) && (name.length() > 0)) {
							StringBuilder builder = new StringBuilder();
							builder.append((String) serverLocationsList.getSelectedItem());
							if (!(builder.toString().endsWith("/"))) {
								builder.append("/");
							}
							builder.append(URLEncoder.encode(name, "UTF-8"));
							builder.append(".history");
							IRI serverIRI = IRI.create(builder.toString());
							ClientUtilities.createServerOntology(client, serverIRI, new ChangeMetaData("Uploaded from file " + input), ontology);
							tableModel.loadServerData(client, currentDirectory);
						}
					}
				}
				catch (Exception ex) {
					ErrorLogPanel.showErrorDialog(ex);
				}
			}
		}
	}
}
