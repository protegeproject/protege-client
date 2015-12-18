package org.protege.editor.owl.client.diff.model;

import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface ReviewManager {

    /**
     * Set the specified review status for the given change
     *
     * @param c Change
     * @param status    Review status
     */
    void setReviewStatus(Change c, ReviewStatus status);

    /**
     * Check whether the review of the specified change has changed since its initial state
     *
     * @param c Change
     * @return true if change review has changed since its initial state, false otherwise
     */
    boolean reviewChanged(Change c);

    /**
     * Check whether there are change reviews that have not been uncommitted
     *
     * @return true if there are uncommitted reviews, false otherwise
     */
    boolean hasUncommittedReviews();

    /**
     * Get the list of ontology changes derived from the uncommitted reviews
     *
     * @return List of OWL ontology changes derived from uncommitted reviews
     */
    List<OWLOntologyChange> getReviewOntologyChanges();

    /**
     * Clear any uncommitted reviews (and set their review status to pending)
     */
    void clearUncommittedReviews();

}
