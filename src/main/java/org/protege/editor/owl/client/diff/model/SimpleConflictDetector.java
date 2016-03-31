package org.protege.editor.owl.client.diff.model;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class SimpleConflictDetector implements ConflictDetector {
    private static final Logger log = Logger.getLogger(SimpleConflictDetector.class);
    private static final Strategy DEFAULT_STRATEGY = Strategy.SAME_TYPE_AND_ANNOTATION_PROPERTY;

    /**
     * No-args constructor
     */
    public SimpleConflictDetector() { }

    @Override
    public Set<ChangeId> getConflictingChanges(Change seed, Collection<Change> searchSpace) {
        return getConflictingChanges(seed, DEFAULT_STRATEGY, searchSpace);
    }

    private Set<ChangeId> getConflictingChanges(Change seed, Strategy strategy, Collection<Change> searchSpace) {
        checkNotNull(seed);
        checkNotNull(strategy);
        checkNotNull(searchSpace);
        UserId userId = seed.getCommitMetadata().getAuthor();
        Set<ChangeId> conflictingChanges = new HashSet<>();
        // The search space is restricted to changes with the same change subject
        for (Change change : searchSpace) {
            if (!(change.getCommitMetadata().getAuthor().equals(userId))) {
                if (strategy.equals(Strategy.LOOSE) || (strategy.equals(Strategy.SAME_TYPE) && isSameType(seed, change))) {
                    conflictingChanges.add(change.getId());
                } else if (strategy.equals(Strategy.SAME_TYPE_AND_ANNOTATION_PROPERTY) && isSameType(seed, change)) {
                    if (seed.getDetails().getType().equals(BuiltInChangeType.ANNOTATION) ||
                            seed.getDetails().getType().equals(BuiltInChangeType.ONTOLOGY_ANNOTATION)) {
                        if (seed.getDetails().getProperty().isPresent() && change.getDetails().getProperty().isPresent()) {
                            if (seed.getDetails().getProperty().get().equals(change.getDetails().getProperty().get())) {
                                conflictingChanges.add(change.getId());
                            }
                        }
                    } else {
                        conflictingChanges.add(change.getId());
                    }
                }
            }
        }
        return conflictingChanges;
    }

    private boolean isSameType(Change c1, Change c2) {
        return c1.getDetails().getType().equals(c2.getDetails().getType()) && axiomTypesMatch(c1, c2);
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

    public enum Strategy {
        LOOSE, SAME_TYPE, SAME_TYPE_AND_ANNOTATION_PROPERTY
    }
}
