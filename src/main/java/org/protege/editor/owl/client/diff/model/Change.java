package org.protege.editor.owl.client.diff.model;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.Optional;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Change extends Comparable<Change> {

    /**
     * Get the set of ontology changes involved in this change
     *
     * @return Set of OWLOntologyChange
     */
    Set<OWLOntologyChange> getChanges();

    /**
     * Get the revision tag for this change
     *
     * @return Revision tag
     */
    RevisionTag getRevisionTag();

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
    ChangeMode getChangeMode();

    /**
     * Get the change subject, that is (for most axioms), the left-hand side of the axiom. For example,
     * for A SubClassOf B the change subject is A. This may be an OWL entity or class expression
     *
     * @return Change subject as an OWLObject
     */
    OWLObject getSubject();

    /**
     * Get the change type, which could be one of the built-in types such as logical, signature or
     * annotation, or a custom type
     *
     * @return Change type
     */
    ChangeType getType();

    /**
     * Set the type of this change
     *
     * @param type  Change type
     */
    void setType(ChangeType type);

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
     * Get the (annotation) property involved in the change, if any
     *
     * @return Annotation property changed
     */
    Optional<OWLObject> getProperty();

    /**
     * Add a set of changes that are in conflict with this one
     *
     * @param conflictingChanges    Set of conflicting changes
     */
    void addConflictingChanges(Set<Change> conflictingChanges);

    /**
     * Get the set of changes that conflict with this one
     *
     * @return Set of conflicting changes
     */
    Set<Change> getConflictingChanges();

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
