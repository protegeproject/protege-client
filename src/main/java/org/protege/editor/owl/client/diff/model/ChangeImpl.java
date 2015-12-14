package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeImpl implements Change {
    private final OWLObject subject, property;
    private final String newValue;
    private final RevisionTag revisionTag;
    private final CommitMetadata commitMetadata;
    private final Set<OWLOntologyChange> changes;
    private Set<Change> conflictingChanges = new HashSet<>();
    private OWLOntologyChange baselineChange;
    private ChangeMode mode;
    private ChangeType type;
    private Review review;

    /**
     * Constructor
     *
     * @param changes    Set of changes
     * @param revisionTag   Revision tag
     * @param commitMetadata    Commit metadata
     * @param mode  Change mode
     * @param subject   Change subject
     * @param type  Change type
     * @param property  Property
     * @param newValue  New value
     */
    public ChangeImpl(Set<OWLOntologyChange> changes, RevisionTag revisionTag, CommitMetadata commitMetadata, ChangeMode mode, OWLObject subject, ChangeType type,
                      Optional<OWLObject> property, Optional<String> newValue) {
        this.changes = checkNotNull(changes);
        this.revisionTag = checkNotNull(revisionTag);
        this.commitMetadata = checkNotNull(commitMetadata);
        this.mode = checkNotNull(mode);
        this.subject = checkNotNull(subject);
        this.type = checkNotNull(type);
        this.property = (property.isPresent() ? checkNotNull(property.get()) : null);
        this.newValue = (newValue.isPresent() ? checkNotNull(newValue.get()) : null);
    }

    public ChangeImpl(Set<OWLOntologyChange> changes, ChangeDetails details, CommitMetadata commitMetadata) {
        this(changes, details.getRevisionTag(), commitMetadata, details.getChangeMode(), details.getChangeSubject(), details.getChangeType(), details.getProperty(), details.getNewValue());
    }

    @Override
    public Set<OWLOntologyChange> getChanges() {
        return changes;
    }

    @Override
    public RevisionTag getRevisionTag() {
        return revisionTag;
    }

    @Override
    public CommitMetadata getCommitMetadata() {
        return commitMetadata;
    }

    @Override
    public ChangeMode getChangeMode() {
        return mode;
    }

    @Override
    public OWLObject getSubject() {
        return subject;
    }

    @Override
    public ChangeType getType() {
        return type;
    }

    @Override
    public void setType(ChangeType type) {
        this.type = checkNotNull(type);
    }

    @Override
    public void setMode(ChangeMode mode) {
        this.mode = checkNotNull(mode);
    }

    @Override
    public boolean isOfType(ChangeType type) {
        return this.type.equals(type);
    }

    @Override
    public Optional<OWLObject> getProperty() {
        return Optional.ofNullable(property);
    }

    @Override
    public void addConflictingChanges(Set<Change> conflictingChanges) {
        for(Change c : conflictingChanges) {
            this.conflictingChanges.add(checkNotNull(c));
        }
    }

    @Override
    public Set<Change> getConflictingChanges() {
        return conflictingChanges;
    }

    @Override
    public boolean isConflicting() {
        return !conflictingChanges.isEmpty();
    }

    @Override
    public void setReviewStatus(ReviewStatus reviewStatus) {
        review = new ReviewImpl(reviewStatus, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    @Override
    public ReviewStatus getReviewStatus() {
        if(review == null) {
            return ReviewStatus.PENDING;
        }
        else {
            return review.getStatus();
        }
    }

    @Override
    public Review getReview() {
        return review;
    }

    @Override
    public void setReview(Review review) {
        this.review = checkNotNull(review);
    }

    @Override
    public Optional<OWLOntologyChange> getBaselineChange() {
        return Optional.ofNullable(baselineChange);
    }

    @Override
    public void setBaselineChange(OWLOntologyChange change) {
        this.baselineChange = checkNotNull(change);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeImpl change = (ChangeImpl) o;
        return Objects.equal(subject, change.subject) &&
                Objects.equal(property, change.property) &&
                Objects.equal(newValue, change.newValue) &&
                Objects.equal(revisionTag, change.revisionTag) &&
                Objects.equal(commitMetadata, change.commitMetadata) &&
                mode == change.mode &&
                Objects.equal(type, change.type) &&
                Objects.equal(changes, change.changes) &&
                Objects.equal(conflictingChanges, change.conflictingChanges) &&
                Objects.equal(review, change.review) &&
                Objects.equal(baselineChange, change.baselineChange);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subject, revisionTag, commitMetadata, mode, type, changes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("subject", subject)
                .add("property", property)
                .add("newValue", newValue)
                .add("revisionTag", revisionTag)
                .add("commitMetadata", commitMetadata)
                .add("mode", mode)
                .add("type", type)
                .add("changes", changes)
                .add("conflictingChanges", conflictingChanges)
                .add("review", review)
                .add("baselineChange", baselineChange)
                .toString();
    }

    @Override
    public int compareTo(Change that) {
        return ComparisonChain.start()
                .compare(this.subject, that.getSubject())
                .result();
    }
}
