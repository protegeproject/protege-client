package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.client.panel.ChangeListTableModel;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ShowUncommittedChangesAction extends ProtegeOWLAction {
    private static final long serialVersionUID = -7628375950917155764L;

    @Override
    public void initialise() throws Exception {
        // NO-OP
    }

    @Override
    public void dispose() throws Exception {
        // NO-OP
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            ServerConnectionManager connectionManager = ServerConnectionManager.get(getOWLEditorKit());
            OWLOntology ontology = getOWLModelManager().getActiveOntology();

            VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
            if (vont != null) {
                Client client = connectionManager.createClient(ontology);
                List<OWLOntologyChange> uncommitted = ClientUtilities.getUncommittedChanges(client, vont);
                connectionManager.saveHistoryInBackground(vont);
                if (uncommitted.isEmpty()) {
                    JOptionPane.showMessageDialog(getOWLWorkspace(), "No uncommitted changes");
                }
                else {
                    displayUncommittedChanges(ontology, uncommitted);
                }
            }
            else {
                JOptionPane.showMessageDialog(getOWLWorkspace(), "Active ontology is not connected to a server.");
            }
        }
        catch (UserDeclinedAuthenticationException udae) {
            ; // ignore this because the user knows that he didn't authenticate.
        }
        catch (Exception e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void displayUncommittedChanges(OWLOntology ontology, List<OWLOntologyChange> uncommitted) {
        String shortOntologyName = "";
        OWLOntologyID ontologyId = ontology.getOntologyID();
        if (!ontologyId.isAnonymous()) {
            shortOntologyName = ontology.getOntologyID().getOntologyIRI().get().getRemainder().get();
        }
        if (shortOntologyName.isEmpty()) {
            shortOntologyName = ontologyId.toString();
        }
        ChangeListTableModel tableModel = new ChangeListTableModel(uncommitted);
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(getOWLEditorKit()));
        JScrollPane pane = new JScrollPane(table);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, getOWLWorkspace()));
        dialog.setTitle("Uncommitted changes for " + shortOntologyName);
        dialog.getContentPane().add(pane);
        dialog.setResizable(true);
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }
}
