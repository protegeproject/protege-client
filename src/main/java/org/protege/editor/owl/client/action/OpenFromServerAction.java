package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.ui.OpenFromServerPanel;
import org.protege.editor.owl.model.OWLWorkspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OpenFromServerAction extends AbstractClientAction {

    private static final long serialVersionUID = 1921872278936323557L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(true);
        getClientSession().addListener(sessionListener);
    }

    private ClientSessionListener sessionListener = event -> {
        ClientSessionChangeEvent.EventCategory category = event.getCategory();
        if(category.equals(ClientSessionChangeEvent.EventCategory.USER_LOGIN)) {
            setEnabled(event.getSource());
        } else if(category.equals(ClientSessionChangeEvent.EventCategory.USER_LOGOUT)) {
            setEnabled(event.getSource());
        }
    };

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

    private void setEnabled(ClientSession session) {
        if(session.hasActiveClient()) {
            if(((LocalHttpClient)session.getActiveClient()).getClientType().equals(LocalHttpClient.UserType.ADMIN)) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        } else {
            setEnabled(true);
        }
    }

    private JDialog createDialog() {
        final JDialog dialog = new JDialog(null, "Open from Protege OWL Server", Dialog.ModalityType.MODELESS);
        OpenFromServerPanel openDialogPanel = new OpenFromServerPanel(getClientSession(), getOWLEditorKit());
        openDialogPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
        openDialogPanel.getActionMap().put("CLOSE_DIALOG", new AbstractAction()
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
        dialog.setContentPane(openDialogPanel);
        dialog.setSize(650, 650);
        dialog.setResizable(true);
        return dialog;
    }
}
