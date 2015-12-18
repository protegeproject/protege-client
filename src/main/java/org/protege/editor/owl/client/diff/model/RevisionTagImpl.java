package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RevisionTagImpl implements RevisionTag {
    private String tag;

    /**
     * Constructor
     * @param tag   Revision tag string
     */
    public RevisionTagImpl(String tag) {
        this.tag = checkNotNull(tag);
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RevisionTagImpl that = (RevisionTagImpl) o;
        return Objects.equal(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tag);
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tag", tag)
                .toString();
    }
}
