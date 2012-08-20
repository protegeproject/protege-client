package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.client.panel.HistoryPanel;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.OntologyDocumentRevision;
import org.protege.owl.server.api.RevisionPointer;
import org.protege.owl.server.api.VersionedOntologyDocument;
import org.protege.owl.server.util.ClientUtilities;
import org.semanticweb.owlapi.model.OWLOntology;

public class ShowHistoryAction extends ProtegeOWLAction {
    private static final long serialVersionUID = -7628375950917155764L;

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {

    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            ServerConnectionManager connectionManager = ServerConnectionManager.get(getOWLEditorKit());
            OWLOntology ontology = getOWLModelManager().getActiveOntology();
            VersionedOntologyDocument vont = connectionManager.getVersionedOntology(ontology);
            Client client = connectionManager.createClient(ontology);
            if (vont != null) {
                ChangeHistory changes = ClientUtilities.getChanges(client, vont, OntologyDocumentRevision.START_REVISION.asPointer(), RevisionPointer.HEAD_REVISION);
                HistoryPanel historyPanel = new HistoryPanel(getOWLEditorKit(), changes);
                historyPanel.initialise();
                JOptionPane.showMessageDialog(getOWLWorkspace(), historyPanel);
            }
            else {
                JOptionPane.showMessageDialog(getOWLWorkspace(), "Active Ontology is not connected to a server.");
            }
        }
        catch (Exception e) {
            ProtegeApplication.getErrorLog().logError(e);
        }
    }

}
