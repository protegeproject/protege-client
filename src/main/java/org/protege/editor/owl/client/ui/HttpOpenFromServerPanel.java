package org.protege.editor.owl.client.ui;

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
import java.io.File;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientPreferences;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.OWLClientException;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.http.messages.HttpAuthResponse;
import org.protege.editor.owl.server.http.messages.LoginCreds;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.util.GetUncommittedChangesVisitor;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.UIHelper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.Serializer;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpOpenFromServerPanel extends JPanel {

    private static final long serialVersionUID = -6710802337675443598L;

    private static String currentPassword = null;

    private ClientSession clientRegistry;
    private JButton cancelButton;
    private JButton connectButton;
    private JButton openButton;
    private JButton uploadButton;
    private JComboBox<String> serverLocationsList;
    private JPasswordField password;
    private JTable serverContentTable;
    private JTextField username;
    private OWLEditorKit editorKit;
    private OWLOntologyManager owlManager;
    private ServerTableModel tableModel;

    public HttpOpenFromServerPanel(ClientSession clientRegistry, OWLEditorKit ek) {
        this.clientRegistry = clientRegistry;
        editorKit = ek;
        owlManager = ek.getOWLModelManager().getOWLOntologyManager();

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
                Window window = SwingUtilities.getWindowAncestor(HttpOpenFromServerPanel.this);
                window.setVisible(false);
                window.dispose();
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
            try {
            	
            	String userName = username.getText();
            	String pwd = new String(password.getPassword());
            	
            	
            	     		
                
                Client client = new LocalHttpClient(userName, pwd, serverAddress);
                clientRegistry.setActiveClient(client);

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
    			Client client = clientRegistry.getActiveClient();
    			ServerDocument sdoc = client.openProject(pid);
    			VersionedOWLOntology vont = ((LocalHttpClient) client).buildVersionedOntology(sdoc, owlManager);
    			editorKit.getOWLModelManager().setActiveOntology(vont.getOntology());
    			clientRegistry.registerProject(pid, vont);    			
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
    	Window window = SwingUtilities.getWindowAncestor(HttpOpenFromServerPanel.this);
    	window.setVisible(false);
    	window.dispose();
    }
    
    private class UploadActionListener implements ActionListener {

    	@Override
    	public void actionPerformed(ActionEvent e) {
    		if (clientRegistry.getActiveClient() != null) {
    			try {
    				JRootPane rootPane = HttpOpenFromServerPanel.this.getRootPane();
    				File input = UIUtil.openFile(rootPane, "Choose file to upload", "OWL File", UIHelper.OWL_EXTENSIONS);
    				if (input != null) {
    					OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    					OWLOntology ontology = manager.loadOntologyFromOntologyDocument(input);
    					String name = (String) JOptionPane.showInputDialog(rootPane, "Name: ", "Upload Ontology Document", JOptionPane.PLAIN_MESSAGE);
    					if ((name != null) && (name.length() > 0)) {
    						uploadProject(name, ontology);
    						loadProjectList(clientRegistry.getActiveClient());					
    					}
    				}
    			}
    			catch (Exception ex) {
    				ErrorLogPanel.showErrorDialog(ex);
    			}
    		}
    	}

    	private void uploadProject(String name, OWLOntology ont) {

    		MetaprojectFactory f = Manager.getFactory();
    		ProjectId projectId = f.getProjectId(name);
    		Name projectName = f.getName(name);
    		Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
    		UserId owner = f.getUserId("guest");
    		ProjectOptions options = null;

    		/*
    		 * Create a new project
    		 */

    		GetUncommittedChangesVisitor visitor = new GetUncommittedChangesVisitor(ont);
    		List<OWLOntologyChange> changes = visitor.getChanges();
    		DocumentRevision R0 = DocumentRevision.START_REVISION;
    		RevisionMetadata metadata = new RevisionMetadata(
    				"guest",
    				"Guest User",
    				"dionnero@nih.gov",
    				"First commit");
    		CommitBundle commitBundle = new CommitBundleImpl(R0, new Commit(metadata, changes));
    		try {
    			clientRegistry.getActiveClient().createProject(projectId, projectName, description, owner, Optional.ofNullable(options), Optional.ofNullable(commitBundle));
    		} catch (AuthorizationException | ClientRequestException | RemoteException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    	}
    }
}
