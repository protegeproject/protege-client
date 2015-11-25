package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;

import java.awt.*;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class CustomChangeType implements ChangeType {
    private final String displayName;
    private final Color color;

    /**
     * Constructor
     *
     * @param displayName   Change type display name
     * @param color Change type display color
     */
    public CustomChangeType(String displayName, Optional<Color> color) {
        this.displayName = checkNotNull(displayName);
        this.color = (color.isPresent() ? checkNotNull(color.get()) : null);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isBuiltInType() {
        return false;
    }

    @Override
    public Optional<Color> getDisplayColor() {
        return Optional.ofNullable(color);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomChangeType that = (CustomChangeType) o;
        return Objects.equal(displayName, that.displayName) &&
                Objects.equal(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(displayName, color);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
