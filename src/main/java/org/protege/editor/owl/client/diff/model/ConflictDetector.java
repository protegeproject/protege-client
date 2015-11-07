package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import org.protege.owl.server.api.UserId;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class ConflictDetector {
    private final Strategy DEFAULT_STRATEGY = Strategy.SAME_CATEGORIES_AND_ANNOTATION_PROPERTIES;
    private final List<Change> changes;

    /**
     * Constructor
     *
     * @param changes Change set
     */
    public ConflictDetector(List<Change> changes) {
        this.changes = checkNotNull(changes);
    }

    public Set<Change> getConflictingChanges(Change seed) {
        return getConflictingChanges(seed, DEFAULT_STRATEGY);
    }

    public Set<Change> getConflictingChanges(Change seed, Strategy strategy) {
        UserId userId = seed.getAuthor();
        OWLObject changeSubject = seed.getSubject();
        Set<Change> conflictingChanges = new HashSet<>();
        for(Change change : changes) {
            // same change subject but different author
            if(changeSubject.equals(change.getSubject()) && !(change.getAuthor().equals(userId))) {
                if(strategy.equals(Strategy.LOOSE) ||
                        (strategy.equals(Strategy.SAME_CATEGORIES) && seed.getType().equals(change.getType()))) {
                    conflictingChanges.add(change);
                }
                else if(strategy.equals(Strategy.SAME_CATEGORIES_AND_ANNOTATION_PROPERTIES)) {
                    if(seed.getType().equals(change.getType())) {
                        if(seed.getType().equals(BuiltInChangeType.ANNOTATION) || seed.getType().equals(BuiltInChangeType.ONTOLOGY_ANNOTATION)) {
                            if(seed.getAnnotationProperty().isPresent() && change.getAnnotationProperty().isPresent()) {
                                if (seed.getAnnotationProperty().get().equals(change.getAnnotationProperty().get())) {
                                    conflictingChanges.add(change);
                                }
                            }
                        }
                        else {
                            conflictingChanges.add(change);
                        }
                    }
                }
            }
        }
        return conflictingChanges;
    }

    public List<Change> getChanges() {
        return changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictDetector that = (ConflictDetector) o;
        return Objects.equal(changes, that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(changes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("changes", changes)
                .toString();
    }

    public enum Strategy {
        LOOSE, SAME_CATEGORIES, SAME_CATEGORIES_AND_ANNOTATION_PROPERTIES;
    }
}
