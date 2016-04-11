package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.panel.ChangeHistoryPanel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.owl.server.changes.api.ChangeHistory;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import java.awt.event.ActionEvent;

import javax.swing.JLabel;

public class ShowHistoryAction extends AbstractClientAction {

    private static final long serialVersionUID = -7628375950917155764L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            final VersionedOntologyDocument vont = findActiveVersionedOntology();
            ChangeHistory changes = ChangeUtils.getAllChanges(vont);
            ChangeHistoryPanel changeHistoryPanel = new ChangeHistoryPanel(getOWLEditorKit(), changes);
            changeHistoryPanel.setLocationRelativeTo(getOWLWorkspace());
            changeHistoryPanel.setVisible(true);
        }
        catch (Exception e) {
            ErrorLogPanel.showErrorDialog(e);
            UIHelper ui = new UIHelper(getOWLEditorKit());
            ui.showDialog("Error connecting to server", new JLabel("Show history failed: " + e.getMessage()));
        }
    }

    private VersionedOntologyDocument findActiveVersionedOntology() throws Exception {
        if (!getOntologyResource().isPresent()) {
            throw new Exception("The current active ontology does not link to the server");
        }
        return getOntologyResource().get();
    }
}
