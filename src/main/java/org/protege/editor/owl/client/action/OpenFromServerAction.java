package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;

import org.protege.editor.owl.client.panel.OpenFromServerDialog;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

public class OpenFromServerAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 1921872278936323557L;

    @Override
    public void initialise() throws Exception {
        // NO-OP
    }

    @Override
    public void dispose() throws Exception {
        // NO-OP
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenFromServerDialog dialog = new OpenFromServerDialog(getOWLEditorKit());
        dialog.setLocationRelativeTo(getOWLWorkspace());
        dialog.setVisible(true);
    }
}
