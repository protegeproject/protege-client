package org.protege.editor.owl.client.action;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.ui.workspace.WorkspaceFrame;
import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.ui.UserLoginPanel;

public class LoginAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = -467953803650067917L;

    /*
     * The names below must be the same as the <name value=...> entry in plugin.xml
     */
    private static final String SERVER_MENU_NAME = "Server";
    private static final String LOGIN_MENU_ITEM_NAME = "Login";

    private JMenuItem loginMenuItem;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(true);
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        detectLoginMenuItem();
        if (event.hasCategory(EventCategory.SWITCH_CLIENT)) {
            setEnabled(false);
            changeLoginMenuText(String.format("Logged in as %s (%s)",
                    event.getSource().getActiveClient().getUserInfo().getId(),
                    event.getSource().getActiveClient().getUserInfo().getName()));
        }
        else if (event.hasCategory(EventCategory.CLEAR_SESSION)) {
            setEnabled(true);
            changeLoginMenuText(LOGIN_MENU_ITEM_NAME);
        }
    }

    private void detectLoginMenuItem() {
        if (loginMenuItem == null) {
            WorkspaceFrame wf = ProtegeManager.getInstance().getFrame(getWorkspace());
            JMenu serverMenu = wf.getMenu(SERVER_MENU_NAME);
            for (Component c : serverMenu.getMenuComponents()) {
                if (c instanceof JMenuItem) {
                    JMenuItem menuItem = (JMenuItem) c;
                    if (menuItem.getText().equals(LOGIN_MENU_ITEM_NAME)) {
                        loginMenuItem = menuItem;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UserLoginPanel.showDialog(getOWLEditorKit());
    }

    private void changeLoginMenuText(String newText) {
        if (loginMenuItem != null) {
            loginMenuItem.setText(newText);
        }
    }
}
