package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.ui.OpenFromServerPanel;
import org.protege.editor.owl.model.OWLWorkspace;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

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
