package org.protege.editor.owl.client.diff.model;

import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface ConflictDetector {

    /**
     * Get the set of changes that conflict with the specified one
     *
     * @param seed  Change
     * @return Set of changes in conflict with the given one
     */
    Set<Change> getConflictingChanges(Change seed);

}
