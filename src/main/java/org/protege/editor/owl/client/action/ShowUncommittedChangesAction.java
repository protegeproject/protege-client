package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.panel.ChangeListTableModel;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
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
            final VersionedOntologyDocument vont = findActiveVersionedOntology();
                List<OWLOntologyChange> uncommitted = ChangeUtils.getUncommittedChanges(vont);
                if (uncommitted.isEmpty()) {
                    JOptionPane.showMessageDialog(getOWLWorkspace(), "No uncommitted changes");
                }
                else {
                    saveLocalHistoryInBackground(vont);
                    displayUncommittedChanges(uncommitted);
                }
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

    private void saveLocalHistoryInBackground(VersionedOntologyDocument vont) {
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
        private VersionedOntologyDocument vont;

        public SaveHistory(VersionedOntologyDocument vont) {
            this.vont = vont;
        }

        @Override
        public void run() {
            try {
                vont.saveLocalHistory();
            }
            catch (Exception e) {
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
