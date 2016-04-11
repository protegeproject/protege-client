package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ShowStatusAction extends AbstractClientAction {

    private static final long serialVersionUID = 4601012273632698091L;

    private OWLModelManagerListener listener = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            updateEnabled();
        }
    };

    @Override
    public void initialise() throws Exception {
        super.initialise();
        getOWLModelManager().addListener(listener);
    }

    private void updateEnabled() {
        setEnabled(getOntologyResource().isPresent());
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        getOWLModelManager().removeListener(listener);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            final VersionedOntologyDocument vont = findActiveVersionedOntology();

            JDialog dialog = new JDialog();
            dialog.setTitle("Client status");
            dialog.setLocationRelativeTo(getOWLWorkspace());

            JPanel panel = new JPanel(new GridLayout(0, 2));

            panel.add(new JLabel("Server Document:"));
            panel.add(new JLabel(vont.getRemoteFile().getName()));

            panel.add(new JLabel("Local Revision:"));
            panel.add(new JLabel(vont.getRevision().toString()));

            panel.add(new JLabel("Latest Server Revision:"));
            panel.add(new JLabel(ChangeUtils.getRemoteHeadRevision(vont).toString()));

            panel.add(new JLabel("# of uncommitted changes:"));
            panel.add(new JLabel(ChangeUtils.getUncommittedChanges(vont).size()+""));

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(panel, BorderLayout.CENTER);
            dialog.pack();
            dialog.setVisible(true);

        }
        catch (Exception e) {
            ErrorLogPanel.showErrorDialog(e);
            UIHelper ui = new UIHelper(getOWLEditorKit());
            ui.showDialog("Error connecting to server", new JLabel("Show status failed: " + e.getMessage()));
        }
    }

    private VersionedOntologyDocument findActiveVersionedOntology() throws Exception {
        if (!getOntologyResource().isPresent()) {
            throw new Exception("The current active ontology does not link to the server");
        }
        return getOntologyResource().get();
    }
}
