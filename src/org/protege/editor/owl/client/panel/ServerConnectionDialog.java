package org.protege.editor.owl.client.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ServerConnectionManager;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.RemoteOntologyDocument;
import org.protege.owl.server.api.ServerDirectory;
import org.protege.owl.server.api.ServerDocument;
import org.protege.owl.server.api.User;
import org.protege.owl.server.api.VersionedOWLOntology;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.connect.rmi.RMIClient;
import org.protege.owl.server.policy.RMILoginUtility;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

// ToDo - this only barely works - add error checking...
public class ServerConnectionDialog extends JDialog {
	private static final long serialVersionUID = 720048610707964509L;
	private Logger logger = Logger.getLogger(ServerConnectionDialog.class.getCanonicalName());
	private OWLEditorKit editorKit;
	private Client client;
	private ServerDirectory currentDirectory;
	private RemoteOntologyDocument remoteOntology;
	private ServerTableModel tableModel;
	private JTextField urlField;
	private JButton uploadButton;
	private JButton newDirectoryButton;
	
	public ServerConnectionDialog(Frame owner, OWLEditorKit editorKit) {
		super(owner, "Server Connection Dialog");
		this.editorKit = editorKit;
	}

	public void initialise() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(getNorth(), BorderLayout.NORTH);
		panel.add(getCenter(),BorderLayout.CENTER);
		panel.add(getSouth(), BorderLayout.SOUTH);
		setContentPane(panel);
		pack();
		validate();
	}
	
	public Client getClient() {
		return client;
	}
	
	public RemoteOntologyDocument getRemoteOntology() {
		return remoteOntology;
	}
	
	private JPanel getNorth() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		urlField = new JTextField(RMIClient.SCHEME + "://localhost:" +  "5100/");  // Registry.REGISTRY_PORT + "/");
		panel.add(urlField);
		urlField.addActionListener(new ConnectActionListener());
		return panel;
	}
	
	private JScrollPane getCenter() {
		tableModel = new ServerTableModel();
		JTable table = new JTable(tableModel);
		table.addMouseListener(new ClientTableMouseAdapter(table));
		JScrollPane pane = new JScrollPane(table);		
		pane.setSize(new Dimension(800, 600));
		return pane;
	}
	
	private JPanel getSouth() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		JButton connect = new JButton("Connect");
		connect.addActionListener(new ConnectActionListener());
		panel.add(connect);
		uploadButton = new JButton("Upload");
		uploadButton.addActionListener(new UploadActionListener());
		uploadButton.setEnabled(false);
		panel.add(uploadButton);
		newDirectoryButton = new JButton("New Server Directory");
		newDirectoryButton.addActionListener(new NewDirectoryListener());
		newDirectoryButton.setEnabled(false);
		panel.add(newDirectoryButton);
		return panel;
	}
	
	public void setDirectory(ServerDirectory dir) throws OWLServerException {
	    urlField.setText(dir.getServerLocation().toString());
	    tableModel.loadServerData(client, dir);
	    currentDirectory = dir;
	}
	
	private class ClientTableMouseAdapter extends MouseAdapter {
		private JTable table;

		public ClientTableMouseAdapter(JTable table)	 {
			this.table = table;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			try {
				if (e.getClickCount() == 2) {
					int row = table.getSelectedRow();
					ServerDocument doc = tableModel.getValueAt(row);
					if (doc instanceof RemoteOntologyDocument) {
						RemoteOntologyDocument remoteOntology = (RemoteOntologyDocument) doc;
						ServerConnectionManager connectionManager = ServerConnectionManager.get(editorKit);
						ClientUtilities clientUtilities = new ClientUtilities(client);
						VersionedOWLOntology vont = clientUtilities.loadOntology(editorKit.getOWLModelManager().getOWLOntologyManager(), remoteOntology);
						editorKit.getOWLModelManager().setActiveOntology(vont.getOntology());
						connectionManager.addVersionedOntology(client, vont);
						ServerConnectionDialog.this.setVisible(false);
					}
					else if (doc instanceof ServerDirectory) {
					    setDirectory((ServerDirectory) doc);
					}
				}
			}
			catch (Exception ex) {
				ProtegeApplication.getErrorLog().logError(ex);
			}
		}
	}

	/*
	 * ToDo -- This is really messed up but I wanted to see it work...
	 */
	private class ConnectActionListener implements ActionListener {
	    @Override
	    public void actionPerformed(ActionEvent e) {
            IRI serverLocation = IRI.create(urlField.getText());
            if (!findExistingClient(serverLocation)) {
                loginToServer(serverLocation);
            }
	    }
	    
	    private boolean findExistingClient(IRI serverLocation) {
	        ServerConnectionManager connectionManager = ServerConnectionManager.get(editorKit);
	        Client client = connectionManager.hasClient(serverLocation);
	        return client != null;
	    }
	    
	    private void loginToServer(IRI serverLocation) {
	           LoginDialog login = new LoginDialog(null, "Login");
	            if (login.showDialog()) {
	                boolean success = false;
	                try {
	                    User authenticatedUser = RMILoginUtility.login(serverLocation, login.getName(), login.getPass());
	                    client = new RMIClient(authenticatedUser, IRI.create(urlField.getText()));
	                    ((RMIClient) client).initialise();
	                    currentDirectory = (ServerDirectory) client.getServerDocument(serverLocation);
	                    tableModel.loadServerData(client, currentDirectory);
	                    success = true;
	                }
	                catch (Exception ex) {
	                    ProtegeApplication.getErrorLog().logError(ex);
	                }
	                finally {
	                    if (!success) {
	                        client = null;
	                        JOptionPane.showMessageDialog(getOwner(), "Connection Failed");
	                    }
	                    else {
	                        JOptionPane.showMessageDialog(getOwner(), "Connection succeeded");
	                    }
	                    uploadButton.setEnabled(success);
	                    newDirectoryButton.setEnabled(success);
	                }
	            }
	    }
	}
	
	private class UploadActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) { 
			if (client != null) {
				try {
					File input = new UIHelper(editorKit).chooseOWLFile("Choose file to upload");
					OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
					OWLOntology ontology = manager.loadOntologyFromOntologyDocument(input);
					String name = (String) JOptionPane.showInputDialog(getOwner(), "Name of upload: ");
					ClientUtilities util = new ClientUtilities(client);
					util.createServerOntology(IRI.create(urlField.getText() + "/" + name + ".history"), new ChangeMetaData("Uploaded from file " + input), ontology);
					tableModel.loadServerData(client, currentDirectory);
					JOptionPane.showMessageDialog(getOwner(), "Uploaded!");
				}
				catch (Exception ex) {
					ProtegeApplication.getErrorLog().logError(ex);
				}
			}
		}
	}

	private class NewDirectoryListener implements ActionListener {
	    @Override
	    public void actionPerformed(ActionEvent event) {
	        try {
	            String dirName = (String) JOptionPane.showInputDialog(getOwner(), "Enter the directory name: ", "Create Server Directory", JOptionPane.PLAIN_MESSAGE);
	            URI dir = URI.create(urlField.getText()).resolve(dirName);
	            client.createRemoteDirectory(IRI.create(dir));
	            setDirectory(currentDirectory);
	        }
	        catch (OWLServerException ioe) {
	            throw new RuntimeException(ioe);
	        }
	    }
	}

}
