package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.ClientRegistry;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.changes.ChangeMetaData;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CommitAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 4601012273632698091L;

    private ExecutorService service = Executors.newSingleThreadExecutor();

    private ClientRegistry clientRegistry;

    private OWLModelManagerListener checkVersionOntology = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            updateEnabled();
        }
    };

    @Override
    public void initialise() throws Exception {
        clientRegistry = ClientRegistry.getInstance(getOWLEditorKit());
        getOWLModelManager().addListener(checkVersionOntology);
    }

    private void updateEnabled() {
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        VersionedOntologyDocument vont = clientRegistry.getVersionedOntology(ontology);
        if (vont == null) {
            setEnabled(false);
        }
        else {
            setEnabled(true);
        }
    }

    @Override
    public void dispose() throws Exception {
        getOWLModelManager().removeListener(checkVersionOntology);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Container container = SwingUtilities.getAncestorOfClass(Frame.class, getOWLWorkspace());
        try {
            VersionedOntologyDocument vont = findActiveVersionedOntology();
            String commitComment = JOptionPane.showInputDialog(container, "Commit comment: ", "Commit", JOptionPane.PLAIN_MESSAGE);
            if (commitComment != null && !commitComment.isEmpty()) {
                Client client = clientRegistry.getActiveClient();
                service.submit(new DoCommit(client, vont, commitComment));
            }
        }
        catch (Exception e) {
            handleError(e);
        }
    }

    private VersionedOntologyDocument findActiveVersionedOntology() throws Exception {
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        VersionedOntologyDocument vont = clientRegistry.getVersionedOntology(ontology);
        if (vont == null) {
            String template = "The ontology <%s> does not link to the server";
            String message = String.format(template, ontology.getOntologyID().getOntologyIRI());
            throw new Exception(message);
        }
        return vont;
    }

    private class DoCommit implements Runnable {
        private Client client;
        private VersionedOntologyDocument versionOntology;
        private String commitcomment;

        public DoCommit(Client client, VersionedOntologyDocument vont, String commitComment) {
            this.client = client;
            this.versionOntology = vont;
            this.commitcomment = commitComment;
        }

        @Override
        public void run() {
            ChangeMetaData metaData = new ChangeMetaData(commitcomment);
            try {
//                DocumentFactory factory = new DocumentFactoryImpl();
//                RemoteOntologyDocument serverDoc = versionOntology.getServerDocument();
//                OntologyDocumentRevision revision = versionOntology.getRevision();
                List<OWLOntologyChange> uncommittedChanges = ChangeUtils.getUncommittedChanges(versionOntology);
//                CommitBundle commits = new CommitBundles(uncommittedChanges);
//                client.commit(project, commits);
            }
            catch (Exception e) {
                handleError(e);
            }
        }
    }

    private void handleError(Throwable t) {
        ErrorLogPanel.showErrorDialog(t);
        UIHelper ui = new UIHelper(getOWLEditorKit());
        ui.showDialog("Error connecting to server", new JLabel("Commit failed - " + t.getMessage()));
    }
}
