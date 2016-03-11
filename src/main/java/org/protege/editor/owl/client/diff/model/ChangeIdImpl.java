package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeIdImpl implements ChangeId {
    private final String id;

    /**
     * Constructor
     *
     * @param id    Identifier
     */
    public ChangeIdImpl(String id) {
        this.id = checkNotNull(id);
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeIdImpl changeId = (ChangeIdImpl) o;
        return Objects.equal(id, changeId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
