package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import org.protege.owl.server.api.UserId;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.Date;
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
    private final String commitComment, newValue;
    private final Date date;
    private final UserId author;
    private ChangeMode changeMode;
    private ChangeType type;
    private Set<OWLOntologyChange> changes;
    private Set<OWLAxiom> axioms;
    private Set<Change> conflictingChanges = new HashSet<>();
    private String oldValue;
    private ChangeReview review;
    private OWLOntologyChange baselineChange;

    /**
     * Constructor
     *
     * @param changes    Set of changes
     * @param changeMode  Change mode
     * @param date  Commit date
     * @param author    Commit author
     * @param subject   Change subject
     * @param type  Change type
     * @param property  Property
     * @param newValue  New value
     * @param commitComment Commit comment
     */
    public ChangeImpl(Set<OWLOntologyChange> changes, ChangeMode changeMode, Date date, UserId author, OWLObject subject, ChangeType type,
                      Optional<OWLObject> property, Optional<String> newValue, String commitComment) {
        this.changes = checkNotNull(changes);
        this.changeMode = checkNotNull(changeMode);
        this.date = checkNotNull(date);
        this.author = checkNotNull(author);
        this.subject = checkNotNull(subject);
        this.type = checkNotNull(type);
        this.property = (property.isPresent() ? checkNotNull(property.get()) : null);
        this.newValue = (newValue.isPresent() ? checkNotNull(newValue.get()) : null);
        this.commitComment = checkNotNull(commitComment);
    }

    @Override
    public Set<OWLOntologyChange> getChanges() {
        return changes;
    }

    @Override
    public Set<OWLAxiom> getChangeAxioms() {
        if(axioms == null) {
            axioms = new HashSet<>();
            for (OWLOntologyChange c : changes) {
                if (c.isAxiomChange() && c.getAxiom() != null) {
                    axioms.add(c.getAxiom());
                }
            }
        }
        return axioms;
    }

    @Override
    public ChangeMode getChangeMode() {
        return changeMode;
    }

    @Override
    public void setChangeMode(ChangeMode changeMode) {
        this.changeMode = checkNotNull(changeMode);
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public UserId getAuthor() {
        return author;
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
    public boolean isOfType(ChangeType type) {
        return this.type.equals(type);
    }

    @Override
    public Optional<OWLObject> getAnnotationProperty() {
        return Optional.ofNullable(property);
    }

    @Override
    public Optional<String> getPriorValue() {
        return Optional.ofNullable(oldValue);
    }

    @Override
    public Optional<String> getValue() {
        return Optional.ofNullable(newValue);
    }

    @Override
    public String getCommitComment() {
        return commitComment;
    }

    @Override
    public void setPriorValue(String previousValue) {
        this.oldValue = checkNotNull(previousValue);
    }

    @Override
    public void addConflictingChanges(Set<Change> conflictingChanges) {
        conflictingChanges.forEach(this::addConflictingChange);
    }

    @Override
    public void addConflictingChange(Change change) {
        conflictingChanges.add(checkNotNull(change));
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
    public void setReviewStatus(ChangeReviewStatus reviewStatus) {
        if(review == null) {
            review = new ChangeReview(reviewStatus, Optional.empty(), Optional.empty(), Optional.empty());
        }
        else {
            review.setStatus(reviewStatus);
        }
    }

    @Override
    public ChangeReviewStatus getReviewStatus() {
        if(review == null) {
            return ChangeReviewStatus.PENDING;
        }
        else {
            return review.getStatus();
        }
    }

    @Override
    public ChangeReview getReview() {
        return review;
    }

    @Override
    public void setReview(ChangeReview review) {
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
                Objects.equal(commitComment, change.commitComment) &&
                Objects.equal(type, change.type) &&
                Objects.equal(date, change.date) &&
                Objects.equal(author, change.author) &&
                Objects.equal(changeMode, change.changeMode) &&
                Objects.equal(changes, change.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subject, commitComment, type, date, author, changeMode, changes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("subject", subject)
                .add("property", property)
                .add("newValue", newValue)
                .add("commitComment", commitComment)
                .add("type", type)
                .add("date", date)
                .add("author", author)
                .add("changeMode", changeMode)
                .add("changes", changes)
                .toString();
    }

    @Override
    public int compareTo(Change that) {
        return ComparisonChain.start()
                .compare(this.subject, that.getSubject())
                .result();
    }
}
