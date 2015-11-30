package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.model.OWLModelManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ReviewPanel extends JPanel implements Disposable {
    private LogDiffManager diffManager;
    private ReviewManager reviewManager;
    private JButton rejectBtn, clearBtn, acceptBtn, commitBtn;

    public ReviewPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        diffManager = LogDiffManager.get(modelManager, editorKit);
        reviewManager = diffManager.getReviewManager();
        setLayout(new FlowLayout(FlowLayout.CENTER));
        addButtons();
    }

    private void addButtons() {
        rejectBtn = getButton(GuiUtils.REVIEW_REJECTED_ICON_FILENAME, rejectBtnListener, 22, 22);
        rejectBtn.setToolTipText("Reject selected change(s); rejected changes are undone");

        clearBtn = getButton(GuiUtils.REVIEW_CLEAR_ICON_FILENAME, clearBtnListener, 22, 22);
        clearBtn.setToolTipText("Modify selected change(s) to review-pending status");

        acceptBtn = getButton(GuiUtils.REVIEW_ACCEPTED_ICON_FILENAME, acceptBtnListener, 22, 22);
        acceptBtn.setToolTipText("Accept selected change(s)");

        commitBtn = getButton(GuiUtils.REVIEW_COMMIT_ICON_FILENAME, commitBtnListener, 26, 26);
        commitBtn.setToolTipText("Commit all change reviews");

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(15, 0));

        add(rejectBtn); add(clearBtn); add(acceptBtn); add(separator); add(commitBtn);
        diffManager.addListener(changeSelectionListener);
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
            else if(event.equals(LogDiffEvent.CHANGE_REVIEWED)) {
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
                reviewManager.addReview(c, ReviewStatus.REJECTED);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener acceptBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.addReview(c, ReviewStatus.ACCEPTED);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener clearBtnListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(Change c : diffManager.getSelectedChanges()) {
                reviewManager.addReview(c, ReviewStatus.PENDING);
            }
            diffManager.statusChanged(LogDiffEvent.CHANGE_REVIEWED);
        }
    };

    private ActionListener commitBtnListener = e -> JOptionPane.showMessageDialog(null, "Changes shall not be committed (i.e., this is not implemented yet)");

    private JButton getButton(String iconFileName, ActionListener listener, int iconWidth, int iconHeight) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(75, 35));
        button.setIcon(GuiUtils.getIcon(iconFileName, iconWidth, iconHeight));
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
        diffManager.removeListener(changeSelectionListener);
    }
}
