package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class ChangeDetailsImpl implements ChangeDetails {
    private final RevisionTag revisionTag;
    private final OWLObject changeSubject, property;
    private final ChangeType changeType;
    private final String newValue;

    /**
     * Constructor
     *
     * @param revisionTag   Revision tag
     * @param changeSubject OWL entity that is the change subject
     * @param changeType    Change type
     * @param property  Annotation property whose value has changed
     * @param newValue  New value of the annotation property
     */
    public ChangeDetailsImpl(RevisionTag revisionTag, OWLObject changeSubject, ChangeType changeType, Optional<OWLObject> property, Optional<String> newValue) {
        this.revisionTag = checkNotNull(revisionTag);
        this.changeSubject = checkNotNull(changeSubject);
        this.changeType = checkNotNull(changeType);
        this.property = (property.isPresent() ? checkNotNull(property.get()) : null);
        this.newValue = (newValue.isPresent() ? checkNotNull(newValue.get()) : null);
    }

    @Override
    public RevisionTag getRevisionTag() {
        return revisionTag;
    }

    @Override
    public OWLObject getSubject() {
        return changeSubject;
    }

    @Override
    public ChangeType getType() {
        return changeType;
    }

    @Override
    public Optional<OWLObject> getProperty() {
        return Optional.ofNullable(property);
    }

    @Override
    public Optional<String> getNewValue() {
        return Optional.ofNullable(newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeDetailsImpl that = (ChangeDetailsImpl) o;
        return Objects.equal(revisionTag, that.revisionTag) &&
                Objects.equal(changeSubject, that.changeSubject) &&
                Objects.equal(property, that.property) &&
                Objects.equal(changeType, that.changeType) &&
                Objects.equal(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(revisionTag, changeSubject, property, changeType, newValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("revisionTag", revisionTag)
                .add("changeSubject", changeSubject)
                .add("property", property)
                .add("changeType", changeType)
                .add("newValue", newValue)
                .toString();
    }
}
