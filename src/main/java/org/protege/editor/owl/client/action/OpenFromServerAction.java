package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.panel.OpenFromServerDialog;

import java.awt.event.ActionEvent;

public class OpenFromServerAction extends AbstractClientAction {

    private static final long serialVersionUID = 1921872278936323557L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenFromServerDialog dialog = new OpenFromServerDialog(getClientRegistry());
        dialog.setLocationRelativeTo(getOWLWorkspace());
        dialog.setVisible(true);
    }
}
