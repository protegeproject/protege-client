package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class ChangeDetails {
    private final RevisionTag revisionTag;
    private final OWLEntity changeSubject, property;
    private final ChangeType changeType;
    private final ChangeMode changeMode;
    private final String newValue;

    public ChangeDetails(RevisionTag revisionTag, OWLEntity changeSubject, ChangeType changeType, ChangeMode changeMode, Optional<OWLEntity> property, Optional<String> newValue) {
        this.revisionTag = checkNotNull(revisionTag);
        this.changeSubject = checkNotNull(changeSubject);
        this.changeType = checkNotNull(changeType);
        this.changeMode = checkNotNull(changeMode);
        this.property = (property.isPresent() ? checkNotNull(property.get()) : null);
        this.newValue = (newValue.isPresent() ? checkNotNull(newValue.get()) : null);
    }

    public RevisionTag getRevisionTag() {
        return revisionTag;
    }

    public OWLEntity getChangeSubject() {
        return changeSubject;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public ChangeMode getChangeMode() {
        return changeMode;
    }

    public Optional<OWLObject> getProperty() {
        return Optional.ofNullable(property);
    }

    public Optional<String> getNewValue() {
        return Optional.ofNullable(newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeDetails that = (ChangeDetails) o;
        return Objects.equal(revisionTag, that.revisionTag) &&
                Objects.equal(changeSubject, that.changeSubject) &&
                Objects.equal(property, that.property) &&
                Objects.equal(changeType, that.changeType) &&
                changeMode == that.changeMode &&
                Objects.equal(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(revisionTag, changeSubject, property, changeType, changeMode, newValue);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("revisionTag", revisionTag)
                .add("changeSubject", changeSubject)
                .add("property", property)
                .add("changeType", changeType)
                .add("changeMode", changeMode)
                .add("newValue", newValue)
                .toString();
    }
}
