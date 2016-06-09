package org.protege.editor.owl.client.action;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.protege.editor.owl.client.ui.UserLoginPanel;
import org.protege.editor.owl.model.OWLWorkspace;

public class LoginAction extends AbstractClientAction {

    private static final long serialVersionUID = 8068484183504794638L;

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
        final OWLWorkspace editorWindow = getOWLEditorKit().getOWLWorkspace();
        JDialog dialog = createDialog();
        dialog.setLocationRelativeTo(editorWindow);
        dialog.setVisible(true);
    }

    private JDialog createDialog() {
        final JDialog dialog = new JDialog(null, "Login to Protege OWL Server", Dialog.ModalityType.MODELESS);
        UserLoginPanel userLoginPanel = new UserLoginPanel(getClientSession(), getOWLEditorKit());
        userLoginPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
        userLoginPanel.getActionMap().put("CLOSE_DIALOG", new AbstractAction()
        {
           private static final long serialVersionUID = 1L;
           @Override
           public void actionPerformed(ActionEvent e)
           {
               dialog.setVisible(false);
               dialog.dispose();
           }
        });
        dialog.addWindowListener(new WindowAdapter()
        {
           @Override
           public void windowClosing(WindowEvent e)
           {
               dialog.setVisible(false);
               dialog.dispose();
           }
        });
        dialog.setContentPane(userLoginPanel);
        dialog.setSize(415, 185);
        dialog.setResizable(false);
        return dialog;
    }
}
