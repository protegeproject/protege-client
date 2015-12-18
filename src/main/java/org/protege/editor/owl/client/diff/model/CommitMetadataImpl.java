package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.protege.owl.server.api.UserId;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class CommitMetadataImpl implements CommitMetadata {
    private final UserId userId;
    private final Date date;
    private final String comment;
    private final int hashcode;

    /**
     * Constructor
     *
     * @param userId    User identifier
     * @param date  Commit date
     * @param comment   Commit comment
     * @param hashcode  Commit hashcode
     */
    public CommitMetadataImpl(UserId userId, Date date, String comment, int hashcode) {
        this.userId = checkNotNull(userId);
        this.date = checkNotNull(date);
        this.comment = checkNotNull(comment);
        this.hashcode = checkNotNull(hashcode);
    }

    @Override
    public UserId getAuthor() {
        return userId;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public int getHashcode() {
        return hashcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitMetadataImpl commit = (CommitMetadataImpl) o;
        return Objects.equal(hashcode, commit.hashcode) &&
                Objects.equal(userId, commit.userId) &&
                Objects.equal(date, commit.date) &&
                Objects.equal(comment, commit.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, date, comment, hashcode);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("date", date)
                .add("comment", comment)
                .add("hashcode", hashcode)
                .toString();
    }

    @Override
    public int compareTo(CommitMetadata that) {
        return that.getDate().compareTo(this.date); // compare w.r.t. date only
    }
}
