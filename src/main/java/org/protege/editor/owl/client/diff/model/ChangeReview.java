package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import org.protege.owl.server.api.UserId;

import java.util.Date;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeReview {
    private ChangeReviewStatus status;
    private UserId author;
    private Date date;
    private String comment;

    /**
     * Constructor
     *
     * @param status    Change review status
     * @param author    Review author
     * @param date  Review date
     * @param comment Review comment
     */
    public ChangeReview(ChangeReviewStatus status, Optional<UserId> author, Optional<Date> date, Optional<String> comment) {
        this.status = checkNotNull(status);
        this.author = (author.isPresent() ? checkNotNull(author.get()) : null);
        this.date = (date.isPresent() ? checkNotNull(date.get()) : null);
        this.comment = (comment.isPresent() ? checkNotNull(comment.get()) : null);
    }

    public Optional<UserId> getAuthor() {
        return Optional.ofNullable(author);
    }

    public Optional<Date> getDate() {
        return Optional.ofNullable(date);
    }

    public ChangeReviewStatus getStatus() {
        return status;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public void setAuthor(UserId author) {
        this.author = checkNotNull(author);
    }

    public void setDate(Date date) {
        this.date = checkNotNull(date);
    }

    public void setComment(String comment) {
        this.comment = checkNotNull(comment);
    }

    public void setStatus(ChangeReviewStatus status) {
        this.status = checkNotNull(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeReview that = (ChangeReview) o;
        return Objects.equal(author, that.author) &&
                Objects.equal(date, that.date) &&
                Objects.equal(status, that.status) &&
                Objects.equal(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(author, date, status, comment);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("author", author)
                .add("date", date)
                .add("status", status)
                .add("comment", comment)
                .toString();
    }
}
