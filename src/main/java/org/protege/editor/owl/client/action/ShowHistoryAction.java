package org.protege.editor.owl.client.action;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.ui.ChangeHistoryPanel;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ShowHistoryAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = -7628375950917155764L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is disabled
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
            setEnabled(activeVersionOntology.isPresent());
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final OWLWorkspace editorWindow = getOWLEditorKit().getOWLWorkspace();
        try {
            JDialog dialog = createDialog();
            dialog.setLocationRelativeTo(editorWindow);
            dialog.setVisible(true);
        }
        catch (ClientRequestException | SynchronizationException e) {
            showErrorDialog("Unable to show change history", e.getMessage(), e);
        }
    }

    private JDialog createDialog() throws ClientRequestException, SynchronizationException {
        final JDialog dialog = new JDialog(null, "Browse Change History", Dialog.ModalityType.MODELESS);
        ChangeHistoryPanel changeHistoryPanel = new ChangeHistoryPanel(activeVersionOntology.get(), getOWLEditorKit());
        changeHistoryPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
        changeHistoryPanel.getActionMap().put("CLOSE_DIALOG", new AbstractAction()
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
        dialog.setContentPane(changeHistoryPanel);
        dialog.setSize(800, 600);
        dialog.setResizable(true);
        return dialog;
    }
}
