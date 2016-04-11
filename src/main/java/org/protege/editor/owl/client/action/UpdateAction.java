package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import java.awt.event.ActionEvent;

import javax.swing.JLabel;

public class UpdateAction extends AbstractClientAction {

    private static final long serialVersionUID = 2694484296709954780L;

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
            updateOntologyInBackground(vont);
        }
        catch (Exception e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private VersionedOntologyDocument findActiveVersionedOntology() throws Exception {
        if (!getOntologyResource().isPresent()) {
            throw new Exception("The current active ontology does not link to the server");
        }
        return getOntologyResource().get();
    }

    private void updateOntologyInBackground(VersionedOntologyDocument vont) {
        submit(new DoUpdate(vont));
    }

    private class DoUpdate implements Runnable {
        private VersionedOntologyDocument vont;

        public DoUpdate(VersionedOntologyDocument vont) {
            this.vont = vont;
        }

        @Override
        public void run() {
            try {
                ChangeUtils.getLatestChanges(vont);
            }
            catch (OWLServerException e) {
                handleError(e);
            }
        }
    }

    private void handleError(Throwable t) {
        ErrorLogPanel.showErrorDialog(t);
        UIHelper ui = new UIHelper(getOWLEditorKit());
        ui.showDialog("Error at client instance", new JLabel("Save history failed: " + t.getMessage()));
    }
}
