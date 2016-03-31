package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;
import org.protege.owl.server.changes.api.RevisionPointer;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ShowStatusAction extends ProtegeOWLAction {
    private static final long serialVersionUID = 4601012273632698091L;
    private ServerConnectionManager connectionManager;
    private OWLModelManagerListener listener = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            updateEnabled();
        }
    };

    @Override
    public void initialise() throws Exception {
        updateEnabled();
        getOWLModelManager().addListener(listener);
    }

    private void updateEnabled() {
        connectionManager = ServerConnectionManager.get(getOWLEditorKit());
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
        if (vont == null) {
            setEnabled(false);
        }
        else {
            setEnabled(true);
        }
    }

    @Override
    public void dispose() throws Exception {
        getOWLModelManager().removeListener(listener);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            final OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
            final VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
            Client client = connectionManager.createClient(ontology);
            JDialog dialog = new JDialog();
            dialog.setTitle("Client status");
            dialog.setLocationRelativeTo(getOWLWorkspace());

            JPanel panel = new JPanel(new GridLayout(0, 2));

            panel.add(new JLabel("Server Document:"));
            panel.add(new JLabel(vont.getServerDocument().getServerLocation().toString()));

            panel.add(new JLabel("Local Revision:"));
            panel.add(new JLabel(vont.getRevision().toString()));

            panel.add(new JLabel("Latest Server Revision:"));
            panel.add(new JLabel(client.evaluateRevisionPointer(vont.getServerDocument(), RevisionPointer.HEAD_REVISION).toString()));

            panel.add(new JLabel("# of uncommitted changes:"));
            panel.add(new JLabel("" + ClientUtilities.getUncommittedChanges(client, vont).size()));

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(panel, BorderLayout.CENTER);
            dialog.pack();
            dialog.setVisible(true);

        }
        catch (UserDeclinedAuthenticationException udae) {
            ; // ignore this because the user knows that he didn't want to authenticate
        }
        catch (Exception e) {
            ErrorLogPanel.showErrorDialog(e);
            UIHelper ui = new UIHelper(getOWLEditorKit());
            ui.showDialog("Error connecting to server", new JLabel("Commit failed - " + e.getMessage()));
        }
    }
}
