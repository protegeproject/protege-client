package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.ui.UserLoginPanel;

import java.awt.event.ActionEvent;

public class LoginAction extends AbstractClientAction {
    private static final long serialVersionUID = -467953803650067917L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(true);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UserLoginPanel.showDialog(getOWLEditorKit());
    }
}
