package org.protege.editor.owl.client.diff.model;

import java.awt.*;
import java.util.Optional;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface ChangeType {

    /**
     * Get the display name of this change type
     *
     * @return Display name
     */
    String getDisplayName();

    /**
     * Check whether this change type is a built-in type
     *
     * @return true if type is built-in, false otherwise
     */
    boolean isBuiltInType();

    /**
     * Get the change color for this change type
     *
     * @return Color
     */
    Optional<Color> getDisplayColor();

}
