package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.ui.RequestFocusListener;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.model.OWLModelManagerImpl;
import org.protege.editor.owl.model.history.HistoryManager;
import org.protege.editor.owl.model.history.UndoManagerListener;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 4601012273632698091L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    private List<OWLOntologyChange> localChanges = new ArrayList<>();

    private UndoManagerListener checkUncommittedChanges = new UndoManagerListener() {
        
		@Override
		public void stateChanged(HistoryManager source) {
			OWLOntology activeOntology = getOWLEditorKit().getOWLModelManager().getActiveOntology();
            if (activeVersionOntology.isPresent()) {
                ChangeHistory baseline = activeVersionOntology.get().getChangeHistory();
                localChanges = ClientUtils.getUncommittedChanges(getOWLModelManager().getHistoryManager(), activeOntology, baseline);
                setEnabled(!localChanges.isEmpty());
            }			
		}
    };

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is disabled
        getClientSession().addListener(this);
        getOWLModelManager().getHistoryManager().addUndoManagerListener(checkUncommittedChanges);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        getOWLModelManager().getHistoryManager().removeUndoManagerListener(checkUncommittedChanges);
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
        JTextArea commentArea = new JTextArea(4, 45);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Commit message (do not leave blank):"), BorderLayout.NORTH);
        panel.add(commentArea, BorderLayout.CENTER);
        commentArea.addAncestorListener(new RequestFocusListener());
        int option = JOptionPane.showConfirmDialog(null, panel, "Commit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String comment = commentArea.getText().trim();
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
                ((OWLModelManagerImpl) this.getOWLModelManager()).resetHistory();
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
            if (t instanceof AuthorizationException) {
                showErrorDialog("Authorization error", t.getMessage(), t);
            }
            else if (t instanceof OutOfSyncException) {
                showErrorDialog("Synchronization error", t.getMessage(), t);
            }
            else if (t instanceof ClientRequestException) {
                showErrorDialog("Commit error", t.getMessage(), t);
            }
            else if (t instanceof RemoteException) {
                showErrorDialog("Network error", t.getMessage(), t);
            }
            else {
                showErrorDialog("Commit error", t.getMessage(), t);
            }
        }
        return acceptedChanges;
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
        public ChangeHistory call() throws AuthorizationException, OutOfSyncException,
                ClientRequestException, RemoteException {
            Commit commit = ClientUtils.createCommit(author, comment, changes);
            CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
            return author.commit(getClientSession().getActiveProject(), commitBundle);
        }
    }
}
