package org.protege.editor.owl.client.ui;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ProjectId;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.SessionRecorder;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.OpenProjectResult;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.api.exception.OWLClientException;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OpenFromServerPanel extends JPanel {

    private static final long serialVersionUID = -6710802337675443598L;

    private ClientSession clientSession;

    private OWLEditorKit editorKit;
    private OWLOntologyManager owlManager;

    private JButton btnOpenProject;
    private JButton btnCancel;

    private JTable tblRemoteProjects;
    private ServerTableModel remoteProjectModel;

    public OpenFromServerPanel(ClientSession clientSession, OWLEditorKit editorKit) {
        this.clientSession = clientSession;
        this.editorKit = editorKit;
        owlManager = editorKit.getOWLModelManager().getOWLOntologyManager();

        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                // NO-OP
            }
            @Override
            public void focusGained(FocusEvent e) {
                showLoginWhenNecessary();
            }
        });

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));

        add(getRemoteProjectsPanel(), BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0)); // padding-top

        btnOpenProject = new JButton("Open Project");
        btnOpenProject.setSelected(true);
        btnOpenProject.addActionListener(new OpenActionListener());
        pnlButtons.add(btnOpenProject);

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> {
            closeDialog();
        });
        pnlButtons.add(btnCancel);

        add(pnlButtons, BorderLayout.SOUTH);

        setFocusable(true);
    }

    private JPanel getRemoteProjectsPanel() {
        JPanel pnlRemoteProjects = new JPanel(new BorderLayout());
        
        remoteProjectModel = new ServerTableModel();
        tblRemoteProjects = new JTable(remoteProjectModel);
        tblRemoteProjects.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblRemoteProjects.getSelectedRow();
                    openOntologyDocument(row);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(tblRemoteProjects);
        pnlRemoteProjects.add(scrollPane, BorderLayout.CENTER);
        return pnlRemoteProjects;
    }
    
    private void showLoginWhenNecessary() {
        if (!clientSession.hasActiveClient()) {
            Optional<AuthToken> authToken = UserLoginPanel.showDialog(editorKit, OpenFromServerPanel.this);
            if (!authToken.isPresent()) {
                closeDialog();
            } else if(authToken.isPresent() && clientSession.hasActiveClient()) {
                if(((LocalHttpClient) clientSession.getActiveClient()).getClientType() == LocalHttpClient.UserType.ADMIN) {
                    closeDialog();
                }
            }
        }
        else {
            if(((LocalHttpClient) clientSession.getActiveClient()).getClientType() == LocalHttpClient.UserType.NON_ADMIN) {
                loadProjectList();
            }
        }
    }

    
    private void loadProjectList() {
        try {
            Client client = clientSession.getActiveClient();
            remoteProjectModel.initialize(client);
            tblRemoteProjects.changeSelection(0, 0, false, false); // select the first item as default
        }
        catch (OWLClientException e) {
            JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Error opening project",
                    new JLabel("Open project failed: " + e.getMessage()),
                    JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
        }
    }

    private class OpenActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int row = tblRemoteProjects.getSelectedRow();
            openOntologyDocument(row);
        }
    }

    protected void openOntologyDocument(int row) {
        ProjectId pid = remoteProjectModel.getValueAt(row);
        Object pobj = remoteProjectModel.getValueAt(row, 0);
        try {
            LocalHttpClient httpClient = (LocalHttpClient) clientSession.getActiveClient();
            OpenProjectResult openProjectResult = httpClient.openProject(pid);
            ServerDocument serverDocument = openProjectResult.serverDocument;

            Optional<String> clientChecksum = httpClient.getSnapshotChecksum(pid);
            if (clientChecksum.isPresent() &&
                openProjectResult.snapshotChecksum.isPresent() &&
                !clientChecksum.get().equals(openProjectResult.snapshotChecksum.get())) {
                SnapShot snapshot = httpClient.getSnapShot(pid);
                httpClient.createLocalSnapShot(snapshot.getOntology(), pid);
            }
            
            if (serverDocument != null && pobj != null) {
            	String serverConnection = "Server: " + serverDocument.getServerAddress().toString() + " | User: " + httpClient.getUserInfo().getId() 
            			+ " | Project: " + pobj.toString();
            
            	editorKit.getOWLModelManager().setServerConnectionData(serverConnection);
            }
            
            SessionRecorder.getInstance(this.editorKit).stopRecording();
            VersionedOWLOntology vont = httpClient.buildVersionedOntology(serverDocument, owlManager, pid);
            SessionRecorder.getInstance(this.editorKit).startRecording();
            
            clientSession.setActiveProject(pid, vont);
            
            // update index with possibly new changes from other modelers
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            
            for (List<OWLOntologyChange> c : vont.getChangeHistory().getRevisions().values()) {
            	for (OWLOntologyChange oc : c) {
            		changes.add(oc);
            	}            	            	
            }
            
            editorKit.getSearchManager().updateIndex(changes);
            
            closeDialog();
        }
        catch (LoginTimeoutException e) {
            JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Open project error",
                    new JLabel(e.getMessage()), JOptionPane.ERROR_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null);
            Optional<AuthToken> authToken = UserLoginPanel.showDialog(editorKit, this);
            if (authToken.isPresent() && authToken.get().isAuthorized()) {
                loadProjectList();
            }
        }
        catch (Exception e) {
            JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Open project error",
                    new JLabel(e.getMessage()), JOptionPane.ERROR_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null);
        }
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(OpenFromServerPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
