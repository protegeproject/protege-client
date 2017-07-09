package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.SessionRecorder;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.CommitOperationEvent;
import org.protege.editor.owl.client.ui.*;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.model.history.HistoryManager;
import org.protege.editor.owl.model.history.UndoManagerListener;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.UIHelper;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.stanford.protege.metaproject.api.AuthToken;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 4601012273632698091L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    private List<OWLOntologyChange> localChanges = new ArrayList<>();

    private SessionRecorder sessionRecorder;

    private UndoManagerListener checkUncommittedChanges = new UndoManagerListener() {
        @Override
        public void stateChanged(HistoryManager source) {
            OWLOntology activeOntology = getOWLEditorKit().getOWLModelManager().getActiveOntology();
            if (activeVersionOntology.isPresent()) {
                ChangeHistory baseline = activeVersionOntology.get().getChangeHistory();
                localChanges = ClientUtils.getUncommittedChanges(source, activeOntology, baseline);
                setEnabled(!localChanges.isEmpty());
            }
        }
    };

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is disabled
        getClientSession().addListener(this);
        sessionRecorder = getSessionRecorder();
        sessionRecorder.addUndoManagerListener(checkUncommittedChanges);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        sessionRecorder.removeUndoManagerListener(checkUncommittedChanges);
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            /*
             * This method does not handle if version ontology is present because the menu item will
             * only be enabled if checkUncommittedChanges(...) listener senses changes in the ontology.
             */
            activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
            if (!activeVersionOntology.isPresent()) {
                setEnabled(false);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        CommitDialogPanel commitPanel = new CommitDialogPanel();
        int option = new UIHelper(getOWLEditorKit()).showValidatingDialog("Commit changes", commitPanel, null);
        if (option == JOptionPane.OK_OPTION) {
            String comment = commitPanel.getTextArea().getText().trim();
            if (!comment.isEmpty()) {
                performCommit(activeVersionOntology.get(), comment);
            }
        }
    }

    private void performCommit(VersionedOWLOntology vont, String comment) {
        try {
            Optional<ChangeHistory> acceptedChanges = commit(vont.getHeadRevision(), localChanges, comment);
            if (acceptedChanges.isPresent()) {
                ChangeHistory changes = acceptedChanges.get();
                vont.update(changes); // update the local ontology
                setEnabled(false); // disable the commit action after the changes got committed successfully
                sessionRecorder.reset();
                getClientSession().fireCommitPerformedEvent(new CommitOperationEvent(
                        changes.getHeadRevision(),
                        changes.getMetadataForRevision(changes.getHeadRevision()),
                        changes.getChangesForRevision(changes.getHeadRevision())));
                showInfoDialog("Commit", "Commit success (uploaded as revision " + changes.getHeadRevision() + ")");
            }
        }
        catch (InterruptedException e) {
            showErrorDialog("Commit error", "Internal error: " + e.getMessage(), e);
        }
    }

    private Optional<ChangeHistory> commit(DocumentRevision revision,
            List<OWLOntologyChange> localChanges, String comment) throws InterruptedException {
        Optional<ChangeHistory> acceptedChanges = Optional.empty();
        try {
            Client activeClient = getClientSession().getActiveClient();
            Future<?> task = submit(new DoCommit(revision, activeClient, comment, localChanges));
            acceptedChanges = Optional.ofNullable((ChangeHistory) task.get());
        }
        catch (ExecutionException e) {
            Throwable t = e.getCause();
            String originalMessage = t.getMessage();
            if (t instanceof LoginTimeoutException) {
                showErrorDialog("Commit error", originalMessage, t);
                Optional<AuthToken> authToken = UserLoginPanel.showDialog(getOWLEditorKit(), getEditorKit().getWorkspace());
                if (authToken.isPresent() && authToken.get().isAuthorized()) {
                    recommit(comment);
                }
            }
            else {
                showErrorDialog("Commit error", originalMessage, t);
            }
        }
        return acceptedChanges;
    }

    private void recommit(String comment) {
        performCommit(activeVersionOntology.get(), comment);
    }

    private class DoCommit implements Callable<ChangeHistory> {

        private DocumentRevision commitBaseRevision;
        private Client author;
        private String comment;
        private List<OWLOntologyChange> changes;

        public DoCommit(DocumentRevision commitBaseRevision, Client author, String comment,
                List<OWLOntologyChange> changes) {
            this.commitBaseRevision = commitBaseRevision;
            this.author = author;
            this.comment = comment;
            this.changes = changes;
        }

        @Override
        public ChangeHistory call() throws AuthorizationException, LoginTimeoutException,
                SynchronizationException, ClientRequestException {
            Commit commit = ClientUtils.createCommit(author, comment, changes);
            CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
            return author.commit(getClientSession().getActiveProject(), commitBundle);
        }
    }
}
