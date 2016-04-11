package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.owl.server.changes.ChangeMetaData;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CommitAction extends AbstractClientAction {

    private static final long serialVersionUID = 4601012273632698091L;

    private OWLModelManagerListener checkVersionOntology = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            updateEnabled();
        }
    };

    @Override
    public void initialise() throws Exception {
        super.initialise();
        getOWLModelManager().addListener(checkVersionOntology);
    }

    private void updateEnabled() {
        setEnabled(getOntologyResource().isPresent());
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        getOWLModelManager().removeListener(checkVersionOntology);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Container container = SwingUtilities.getAncestorOfClass(Frame.class, getOWLWorkspace());
        try {
            final VersionedOntologyDocument vont = getActiveVersionedOntology();
            String commitComment = JOptionPane.showInputDialog(container, "Commit comment: ", "Commit", JOptionPane.PLAIN_MESSAGE);
            if (commitComment != null && !commitComment.isEmpty()) {
                submit(new DoCommit(vont, commitComment));
            }
        }
        catch (SynchronizationException e) {
            showSynchronizationErrorDialog(e.getMessage(), e);
        }
    }

    private class DoCommit implements Runnable {
        private VersionedOntologyDocument versionOntology;
        private String commitcomment;

        public DoCommit(VersionedOntologyDocument vont, String commitComment) {
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
                showSynchronizationErrorDialog("Commit failed: " + e.getMessage(), e);
            }
        }
    }
}
