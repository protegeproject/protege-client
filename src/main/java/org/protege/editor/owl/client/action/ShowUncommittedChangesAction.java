package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.ui.ChangeListTableModel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.api.exception.OWLServerException;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class ShowUncommittedChangesAction extends AbstractClientAction {

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
    public void actionPerformed(ActionEvent arg0) {
        try {
            final VersionedOWLOntology vont = getActiveVersionedOntology();
            List<OWLOntologyChange> uncommitted = ChangeUtils.getUncommittedChanges(vont);
            if (uncommitted.isEmpty()) {
                Container container = SwingUtilities.getAncestorOfClass(Frame.class, getOWLWorkspace());
                JOptionPane.showMessageDialog(container, "No uncommitted changes");
            }
            else {
                saveLocalHistoryInBackground(vont);
                displayUncommittedChanges(uncommitted);
            }
        }
        catch (SynchronizationException e) {
            showSynchronizationErrorDialog(e.getMessage(), e);
        }
        catch (OWLServerException e) {
            showSynchronizationErrorDialog("Show uncommitted changes failed: " + e.getMessage(), e);
        }
    }

    private void saveLocalHistoryInBackground(VersionedOWLOntology vont) {
        submit(new SaveHistory(vont));
    }

    private void displayUncommittedChanges(List<OWLOntologyChange> uncommitted) {
        ChangeListTableModel tableModel = new ChangeListTableModel(uncommitted);
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(getOWLEditorKit()));
        JScrollPane pane = new JScrollPane(table);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, getOWLWorkspace()));
        dialog.setTitle("Synchronize changes");
        dialog.getContentPane().add(pane);
        dialog.setResizable(true);
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    private class SaveHistory implements Runnable {
        private VersionedOWLOntology vont;

        public SaveHistory(VersionedOWLOntology vont) {
            this.vont = vont;
        }

        @Override
        public void run() {
//            try {
//                vont.saveLocalHistory();
//            }
//            catch (Exception e) {
//                showSynchronizationErrorDialog("Save local history failed: " + e.getMessage(), e);
//            }
        }
    }
}
