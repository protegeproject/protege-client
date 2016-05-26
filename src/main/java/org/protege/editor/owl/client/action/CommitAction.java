package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

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
        try {
            VersionedOWLOntology versionOntology = getActiveVersionOntology();
            String comment = "";
            while (true) {
                JTextArea commentArea = new JTextArea();
                Object[] message = { "Commit message (do not leave blank):", commentArea };
                int option = JOptionPane.showConfirmDialog(null, message, "Commit", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.CANCEL_OPTION) {
                    break;
                }
                else if (option == JOptionPane.OK_OPTION) {
                    comment = commentArea.getText().trim();
                    if (!comment.isEmpty()) {
                        break;
                    }
                }
            }
            Client activeClient = getClientRegistry().getActiveClient();
            submit(new DoCommit(versionOntology, activeClient, comment));
        }
        catch (SynchronizationException e) {
            showErrorDialog("Synchronization error", e.getMessage(), e);
        }
    }

    private class DoCommit implements Runnable {

        private VersionedOWLOntology vont;
        private Client author;
        private String comment;

        public DoCommit(VersionedOWLOntology vont, Client author, String comment) {
            this.vont = vont;
            this.author = author;
            this.comment = comment;
        }

        @Override
        public void run() {
            try {
                List<OWLOntologyChange> localChanges = ClientUtils.getUncommittedChanges(vont.getOntology(), vont.getChangeHistory());
                Commit commit = ClientUtils.createCommit(author, comment, localChanges);
                DocumentRevision commitBaseRevision = vont.getHeadRevision();
                CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
                author.commit(getClientRegistry().getActiveProject(), commitBundle);
            }
            catch (AuthorizationException e) {
                showErrorDialog("Authorization error", e.getMessage(), e);
            }
            catch (OutOfSyncException e) {
                showErrorDialog("Synchronization error", e.getMessage(), e);
            }
            catch (ClientRequestException e) {
                showErrorDialog("Commit error", e.getMessage(), e);
            }
            catch (RemoteException e) {
                showErrorDialog("Network error", e.getMessage(), e);
            }
        }
    }
}
