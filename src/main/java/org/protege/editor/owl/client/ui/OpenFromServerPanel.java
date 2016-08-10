package org.protege.editor.owl.client.ui;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.*;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.OWLClientException;
import org.protege.editor.owl.server.versioning.api.*;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.*;

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
            }
        }
        else {
            Client client = clientSession.getActiveClient();
            loadProjectList(client);
        }
    }

    
    private void loadProjectList(Client client) {
        try {
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
        try {
            Client client = clientSession.getActiveClient();
            ServerDocument serverDocument = client.openProject(pid);
            
            SessionRecorder.getInstance(this.editorKit).stopRecording();
            VersionedOWLOntology vont = ((LocalHttpClient) client).buildVersionedOntology(serverDocument, owlManager, pid);
            SessionRecorder.getInstance(this.editorKit).startRecording();
            
            clientSession.setActiveProject(pid, vont);
            closeDialog();
        }
        catch (Exception e) {
            JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Error opening project",
                    new JLabel("Open project failed: " + e.getMessage()), JOptionPane.ERROR_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null);
        }
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(OpenFromServerPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
