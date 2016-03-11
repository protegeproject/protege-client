package org.protege.editor.owl.client.diff.model;

import java.util.Collection;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface ConflictDetector {

    /**
     * Get the set of change identifiers within the given {@code searchSpace} changes corresponding
     * to changes in conflict with the specified one.
     *
     * @param seed  Change
     * @param searchSpace   Collection of changes on the same change subject as {@code seed} within which to search for conflicts
     * @return Set of change identifiers
     */
    Set<ChangeId> getConflictingChanges(Change seed, Collection<Change> searchSpace);

}
