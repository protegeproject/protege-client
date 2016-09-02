package org.protege.editor.owl.client.diff.ui;

import edu.stanford.protege.metaproject.api.AuthToken;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.client.event.CommitOperationEvent;
import org.protege.editor.owl.client.ui.UserLoginPanel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ReviewButtonsPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -5575738883023751161L;
    private LogDiffManager diffManager;
    private ReviewManager reviewManager;
    private OWLEditorKit editorKit;
    private JButton rejectBtn, clearBtn, acceptBtn, commitBtn, downloadBtn;

    /**
     * Constructor
     *
     * @param modelManager  OWL model manager
     * @param editorKit OWL editor kit
     */
    public ReviewButtonsPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        this.diffManager = LogDiffManager.get(modelManager, editorKit);
        this.reviewManager = diffManager.getReviewManager();
        setLayout(new FlowLayout(FlowLayout.CENTER, 2, 3));
        addButtons();
    }

    private void addButtons() {
        acceptBtn = getButton("Accept", acceptBtnListener);
        acceptBtn.setToolTipText("Accept selected change(s)");

        rejectBtn = getButton("Reject", rejectBtnListener);
        rejectBtn.setToolTipText("Reject selected change(s); rejected changes are undone");

        clearBtn = getButton("Clear", clearBtnListener);
        clearBtn.setToolTipText("Reset selected change(s) to review-pending status");

        commitBtn = getButton("Commit", commitBtnListener);
        commitBtn.setToolTipText("Commit all change reviews");

        downloadBtn = getButton("Download", downloadBtnListener);
        downloadBtn.setToolTipText("Download the ontology without custom Protégé annotations");

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(20, 0));

        add(rejectBtn); add(clearBtn); add(acceptBtn); add(separator); add(commitBtn); add(downloadBtn);
        diffManager.addListener(changeSelectionListener);
        enable(true, downloadBtn);
    }

    private LogDiffListener changeSelectionListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if(event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if(!diffManager.getSelectedChanges().isEmpty()) {
                    enable(true, acceptBtn, clearBtn, rejectBtn);
                }
                else {
                    enable(false, acceptBtn, clearBtn, rejectBtn);
                }
            }
            if(event.equals(LogDiffEvent.CHANGE_REVIEWED) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                if(reviewManager.hasUncommittedReviews()) {
                    enable(true, commitBtn);
                }
                else {
                    enable(false, commitBtn);
                }
            }
        }
    };

    private ActionListener rejectBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.setReviewStatus(c, ReviewStatus.REJECTED);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener acceptBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.setReviewStatus(c, ReviewStatus.ACCEPTED);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener clearBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.setReviewStatus(c, ReviewStatus.PENDING);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener downloadBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<OWLOntologyChange> changes = diffManager.removeCustomAnnotations();
            try {
                editorKit.handleSaveAs();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            diffManager.addCustomAnnotations(changes);
        }
    };

    private ActionListener commitBtnListener = e -> {
        Container owner = SwingUtilities.getAncestorOfClass(Frame.class, editorKit.getOWLWorkspace());
        int answer = JOptionPane.showOptionDialog(owner, "Committing these reviews may involve undoing or redoing previous changes.\n" +
                "Are you sure you would like to proceed?", "Confirm reviews", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if(answer == JOptionPane.OK_OPTION) {
            List<OWLOntologyChange> changes = reviewManager.getReviewOntologyChanges();
            reviewManager.clearUncommittedReviews();
            enable(false, commitBtn);
            if(!changes.isEmpty()) {
                VersionedOWLOntology vont = diffManager.getVersionedOntologyDocument().get();
                String commitComment = JOptionPane.showInputDialog(owner, "Comment for the review: ", "Commit reviews");
                if (vont == null) {
                    JOptionPane.showMessageDialog(owner, "Commit ignored because the ontology is not associated with a server");
                    return;
                }
                if (commitComment == null) {
                    return; // user pressed cancel
                }
                boolean success = commit(vont, changes, commitComment);
                if(success) {
                    JOptionPane.showMessageDialog(owner, "The reviews have been successfully committed", "Reviews committed", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    };

    private boolean commit(VersionedOWLOntology vont, List<OWLOntologyChange> changes, String commitComment) {
        try {
            ClientSession clientSession = ClientSession.getInstance(editorKit);
            Client client = clientSession.getActiveClient();
            RevisionMetadata metaData = new RevisionMetadata(
                    client.getUserInfo().getId(),
                    client.getUserInfo().getName(),
                    client.getUserInfo().getEmailAddress(), "[Review] " + commitComment);

            diffManager.applyOntologyChanges(changes); // apply changes to active ontology
            Commit commit = new Commit(metaData, changes);
            CommitBundle bundle = new CommitBundleImpl(vont.getHeadRevision(), commit);
            ChangeHistory history = client.commit(clientSession.getActiveProject(), bundle);
            vont.update(history);
            clientSession.fireCommitPerformedEvent(new CommitOperationEvent(
                    history.getHeadRevision(),
                    history.getMetadataForRevision(history.getHeadRevision()),
                    history.getChangesForRevision(history.getHeadRevision())));
            return true;
        } catch (LoginTimeoutException ex1) {
            // TODO timeouts would ideally be dealt with in a central client handler, rather than forcing all client code to deal with them
            Optional<AuthToken> authToken = UserLoginPanel.showDialog(editorKit, this);
            return authToken.isPresent() && authToken.get().isAuthorized() && commit(vont, changes, commitComment);
        } catch (AuthorizationException | ClientRequestException ex2) {
            ErrorLogPanel.showErrorDialog(ex2);
            return false;
        }
    }

    private JButton getButton(String text, ActionListener listener) {
        JButton button = new JButton();
        button.setText(text);
        button.setPreferredSize(new Dimension(95, 32));
        button.setFocusable(false);
        button.addActionListener(listener);
        button.setEnabled(false);
        return button;
    }

    private void enable(boolean enable, JComponent... components) {
        for(JComponent c : components) {
            c.setEnabled(enable);
        }
    }

    @Override
    public void dispose() {
        rejectBtn.removeActionListener(rejectBtnListener);
        clearBtn.removeActionListener(clearBtnListener);
        acceptBtn.removeActionListener(acceptBtnListener);
        commitBtn.removeActionListener(commitBtnListener);
        downloadBtn.removeActionListener(downloadBtnListener);
        diffManager.removeListener(changeSelectionListener);
    }
}
