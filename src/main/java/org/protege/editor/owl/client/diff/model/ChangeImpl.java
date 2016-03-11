package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
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
    private final ChangeId id;
    private final Set<OWLOntologyChange> changes;
    private final CommitMetadata commitMetadata;
    private ChangeMode mode;
    private ChangeDetails details;
    private Set<ChangeId> conflictingChanges = new HashSet<>();
    private OWLOntologyChange baselineChange;
    private Review review;


    /**
     * Constructor
     *
     * @param id  Change identifier
     * @param changes    Set of changes
     * @param details Change details
     * @param commitMetadata    Commit metadata
     */
    public ChangeImpl(ChangeId id, Set<OWLOntologyChange> changes, ChangeDetails details, CommitMetadata commitMetadata, ChangeMode mode) {
        this.id = checkNotNull(id);
        this.changes = checkNotNull(changes);
        this.details = checkNotNull(details);
        this.commitMetadata = checkNotNull(commitMetadata);
        this.mode = checkNotNull(mode);
    }

    @Override
    public ChangeId getId() {
        return id;
    }

    @Override
    public Set<OWLOntologyChange> getChanges() {
        return changes;
    }

    public ChangeDetails getDetails() {
        return details;
    }

    @Override
    public CommitMetadata getCommitMetadata() {
        return commitMetadata;
    }

    @Override
    public ChangeMode getMode() {
        return mode;
    }

    @Override
    public void setMode(ChangeMode mode) {
        this.mode = checkNotNull(mode);
    }

    @Override
    public boolean isOfType(ChangeType type) {
        return details.getType().equals(type);
    }

    @Override
    public void addConflictingChange(ChangeId conflictingChange) {
        this.conflictingChanges.add(checkNotNull(conflictingChange));
    }

    @Override
    public Set<ChangeId> getConflictingChanges() {
        return conflictingChanges;
    }

    @Override
    public boolean isConflicting() {
        return !conflictingChanges.isEmpty();
    }

    @Override
    public void setReviewStatus(ReviewStatus reviewStatus) {
        review = LogDiffManager.getDiffFactory().createReview(reviewStatus, Optional.empty(), Optional.empty(), Optional.empty(), false);
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
        return Objects.equal(id, change.id) &&
                Objects.equal(changes, change.changes) &&
                Objects.equal(commitMetadata, change.commitMetadata) &&
                mode == change.mode &&
                Objects.equal(details, change.details) &&
                Objects.equal(conflictingChanges, change.conflictingChanges) &&
                Objects.equal(baselineChange, change.baselineChange) &&
                Objects.equal(review, change.review);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, changes, commitMetadata, mode, details, conflictingChanges, baselineChange, review);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("changes", changes)
                .add("commitMetadata", commitMetadata)
                .add("mode", mode)
                .add("details", details)
                .add("conflictingChanges", conflictingChanges)
                .add("baselineChange", baselineChange)
                .add("review", review)
                .toString();
    }

    @Override
    public int compareTo(Change that) {
        return ComparisonChain.start()
                .compare(this.details.getSubject(), that.getDetails().getSubject())
                .result();
    }
}
