package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.ui.ChangeHistoryPanel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import java.awt.event.ActionEvent;

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
            final VersionedOWLOntology vont = getActiveVersionedOntology();
            ChangeHistory remoteChanges = ChangeUtils.getAllChanges(vont);
            ChangeHistoryPanel changeHistoryPanel = new ChangeHistoryPanel(getOWLEditorKit(), remoteChanges);
            changeHistoryPanel.setLocationRelativeTo(getOWLWorkspace());
            changeHistoryPanel.setVisible(true);
        }
        catch (SynchronizationException e) {
            showSynchronizationErrorDialog(e.getMessage(), e);
        }
        catch (ClientRequestException e) {
            showSynchronizationErrorDialog("Show history failed: " + e.getMessage(), e);
        }
    }
}
