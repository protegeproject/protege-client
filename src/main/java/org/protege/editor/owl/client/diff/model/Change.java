package org.protege.editor.owl.client.diff.model;

import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.Optional;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Change extends Comparable<Change> {

    /**
     * Get the change identifier
     *
     * @return Change identifier
     */
    ChangeId getId();

    /**
     * Get the set of ontology changes involved in this change
     *
     * @return Set of OWLOntologyChange
     */
    Set<OWLOntologyChange> getChanges();

    /**
     * Get the change details object containing further information
     * about this change, such as its author, type, subject, ...
     *
     * @return Change details
     */
    ChangeDetails getDetails();

    /**
     * Get the commit metadata, such as author, date and comment
     *
     * @return Commit metadata
     */
    CommitMetadata getCommitMetadata();

    /**
     * Get the change mode, i.e., addition, removal or ontology IRI change
     *
     * @return Change mode
     */
    ChangeMode getMode();

    /**
     * Set the change mode
     *
     * @param mode  Change mode
     */
    void setMode(ChangeMode mode);

    /**
     * Check whether this change is of the specified type
     *
     * @param type  Change type
     * @return true if this change is of the type specified, false otherwise
     */
    boolean isOfType(ChangeType type);

    /**
     * Add a change that is in conflict with this one
     *
     * @param conflictingChange    Conflicting change
     */
    void addConflictingChange(ChangeId conflictingChange);

    /**
     * Get the set of changes that conflict with this one
     *
     * @return Set of conflicting changes
     */
    Set<ChangeId> getConflictingChanges();

    /**
     * Check if change is in conflict with some other change(s)
     *
     * @return true if change conflicts with others, false otherwise
     */
    boolean isConflicting();

    /**
     * Get the review of this change, i.e., whether it's pending review,
     * is accepted, or is rejected, the review author, date and comment
     *
     * @return Review
     */
    Review getReview();

    /**
     * Set the review for this change
     *
     * @param review    Review
     */
    void setReview(Review review);

    /**
     * Convenience method to set the review status of the change's review
     *
     * @param status    Review status
     */
    void setReviewStatus(ReviewStatus status);

    /**
     * Get the review status of this change, i.e., whether review is pending,
     * the change was accepted or rejected
     *
     * @return Change review status
     */
    ReviewStatus getReviewStatus();

    /**
     * Get the baseline ontology change for this change, if one exists
     *
     * @return Baseline ontology change
     */
    Optional<OWLOntologyChange> getBaselineChange();

    /**
     * Set the baseline ontology change for this change
     *
     * @param change OWL ontology change
     */
    void setBaselineChange(OWLOntologyChange change);

}
