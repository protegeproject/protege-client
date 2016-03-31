package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.model.OWLModelManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsPanel extends JPanel implements Disposable {
    private OWLEditorKit editorKit;
    private LogDiffManager diffManager;

    /**
     * Constructor
     *
     * @param modelManager OWL model manager
     * @param editorKit    OWL editor kit
     */
    public ChangeDetailsPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);

        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);

        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        setBackground(GuiUtils.WHITE_BACKGROUND);

        createContents();
    }

    private LogDiffListener diffListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if (event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if(!diffManager.getSelectedChanges().isEmpty()) {
                    removeAll();
                    createContents();
                }
            }
            else if(event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) || event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                removeAll();
                repaint();
            }
            // rpc: when review view is active and a change is reviewed, recreate the review panel
        }
    };

    private void createContents() {
        if(!diffManager.getSelectedChanges().isEmpty()) {
            Change change = diffManager.getFirstSelectedChange();
            if (change != null) {
                addDetailsTable(change);
                revalidate();
            }
        }
    }

    private void addDetailsTable(Change change) {
        ChangeDetailsTableModel tableModel;
        if(change.getBaselineChange().isPresent()) {
            tableModel = new MatchingChangeDetailsTableModel();
        }
        else {
            tableModel = new MultipleChangeDetailsTableModel();
        }
        tableModel.setChange(change);
        ChangeDetailsTable table = new ChangeDetailsTable(tableModel, editorKit);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        add(scrollPane, BorderLayout.CENTER);
    }

    @SuppressWarnings("unused") // rpc
    private void addReview(Change change) {
        JPanel reviewPanel = new JPanel(new BorderLayout());
        JLabel reviewLbl = new JLabel();
        Icon icon = null;
        String statusStr = "";
        ReviewStatus status = change.getReviewStatus();
        switch(status) {
            case ACCEPTED:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_ACCEPTED_ICON_FILENAME, 40, 40);
                statusStr = "Accepted"; break;
            case REJECTED:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_REJECTED_ICON_FILENAME, 40, 40);
                statusStr = "Rejected"; break;
            case PENDING:
                icon = GuiUtils.getIcon(GuiUtils.REVIEW_PENDING_ICON_FILENAME, 40, 40);
                statusStr = "Pending Review"; break;
        }
        if(icon != null) {
            reviewLbl.setIcon(icon);
            reviewLbl.setIconTextGap(10);
        }
        reviewLbl.setBorder(new EmptyBorder(10, 13, 10, 1));
        reviewLbl.setText(getReviewText(change.getReview(), statusStr));
        reviewPanel.add(reviewLbl);
        add(reviewPanel, BorderLayout.SOUTH);
    }

    private String getReviewText(Review review, String statusStr) {
        String dateStr = "", author = "", comment = "";
        if(review != null) {
            if (review.getDate().isPresent()) {
                dateStr = GuiUtils.getShortenedFormattedDate(review.getDate().get());
            }
            author = (review.getAuthor().isPresent() ? review.getAuthor().get().toString() : ""); // TODO: To review later
            comment = (review.getComment().isPresent() ? review.getComment().get() : "");
        }
        String reviewText = "<html><p style=\"font-size:14\"><strong><i><u>" + statusStr + "</u></i></strong></p>";
        if(!author.isEmpty() && !dateStr.isEmpty()) {
            reviewText += "<p style=\"padding-top:7px\">Reviewed by <strong>" + author + "</strong> on <strong>" + dateStr + "</strong></p>";
        }
        if(!comment.isEmpty()) {
            reviewText += "<p style=\"padding-top:4px\">Comment: \"" + comment + "\"</p>";
        }
        reviewText += "</html>";
        return reviewText;
    }

    @Override
    public void dispose() {
        diffManager.removeListener(diffListener);
    }
}
