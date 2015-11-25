package org.protege.editor.owl.client.diff.model;

import org.protege.owl.server.api.UserId;

import java.util.Date;
import java.util.Optional;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Review {

    /**
     * Get the change review status, i.e., whether a change is accepted, rejected or pending review
     *
     * @return Review status
     */
    ReviewStatus getStatus();

    /**
     * Get the identifier of the author of the review
     *
     * @return User identifier
     */
    Optional<UserId> getAuthor();

    /**
     * Get the date of the review
     *
     * @return Review date
     */
    Optional<Date> getDate();

    /**
     * Get the review comment
     *
     * @return Review comment
     */
    Optional<String> getComment();

    /**
     * Check whether the review has been committed to the server
     *
     * @return true if review is committed to the server, false otherwise
     */
    boolean isCommitted();

    /**
     * Set the author of the review by its user identifier
     *
     * @param author    User identifier of review author
     */
    void setAuthor(UserId author);

    /**
     * Set the date of the review
     *
     * @param date  Review date
     */
    void setDate(Date date);

    /**
     * Set the status of the review
     *
     * @param status    Review status
     */
    void setStatus(ReviewStatus status);

    /**
     * Set the comment of review
     *
     * @param comment   Review comment
     */
    void setComment(String comment);

    /**
     * Set whether the review has been committed to the server
     *
     * @param committed true if review is committed, false otherwise
     */
    void setCommitted(boolean committed);

}
