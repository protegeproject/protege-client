package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import org.apache.log4j.Logger;
import org.protege.owl.server.api.UserId;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class SimpleConflictDetector implements ConflictDetector {
    private static final Logger log = Logger.getLogger(SimpleConflictDetector.class);
    private final Strategy DEFAULT_STRATEGY = Strategy.SAME_TYPE_AND_ANNOTATION_PROPERTY;
    private final List<Change> changes;

    /**
     * Constructor
     *
     * @param changes Change set
     */
    public SimpleConflictDetector(List<Change> changes) {
        this.changes = checkNotNull(changes);
    }

    @Override
    public Set<Change> getConflictingChanges(Change seed) {
        return getConflictingChanges(seed, DEFAULT_STRATEGY);
    }

    private Set<Change> getConflictingChanges(Change seed, Strategy strategy) {
        UserId userId = seed.getCommitMetadata().getAuthor();
        OWLObject changeSubject = seed.getSubject();
        Set<Change> conflictingChanges = new HashSet<>();
        for(Change change : changes) {
            if(changeSubject.equals(change.getSubject()) && !(change.getCommitMetadata().getAuthor().equals(userId))) {
                if(strategy.equals(Strategy.LOOSE) || (strategy.equals(Strategy.SAME_TYPE) && isSameType(seed, change))) {
                    conflictingChanges.add(change);
                }
                else if (strategy.equals(Strategy.SAME_TYPE_AND_ANNOTATION_PROPERTY) && isSameType(seed, change)) {
                    if (seed.getType().equals(BuiltInChangeType.ANNOTATION) || seed.getType().equals(BuiltInChangeType.ONTOLOGY_ANNOTATION)) {
                        if (seed.getProperty().isPresent() && change.getProperty().isPresent()) {
                            if (seed.getProperty().get().equals(change.getProperty().get())) {
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
        return conflictingChanges;
    }

    private boolean isSameType(Change c1, Change c2) {
        return c1.getType().equals(c2.getType()) && axiomTypesMatch(c1, c2);
    }

    private boolean axiomTypesMatch(Change c1, Change c2) {
        if(c1.getChanges().size() == 1 && c2.getChanges().size() == 1) {
            OWLOntologyChange oc1 = c1.getChanges().iterator().next();
            OWLOntologyChange oc2 = c2.getChanges().iterator().next();
            if(oc1.isAxiomChange() && oc2.isAxiomChange()) {
                OWLAxiom ax1 = oc1.getAxiom(), ax2 = oc2.getAxiom();
                return ax1.getAxiomType().equals(ax2.getAxiomType());
            }
        }
        else {
            log.error("Cannot find conflicts for composite changes (i.e., changes that involve multiple OWL ontology changes)");
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleConflictDetector that = (SimpleConflictDetector) o;
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
        LOOSE, SAME_TYPE, SAME_TYPE_AND_ANNOTATION_PROPERTY
    }
}
