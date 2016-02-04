package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ReviewManagerImpl implements ReviewManager {
    private Map<Change,ReviewStatus> newReviewMap = new HashMap<>();
    private Map<Change,ReviewStatus> initialReviewMap = new HashMap<>();

    /**
     * No-args constructor
     */
    public ReviewManagerImpl() { }

    @Override
    public void setReviewStatus(Change c, ReviewStatus status) {
        checkNotNull(c); checkNotNull(status);

        // gather initial status of the newly reviewed change
        if(!newReviewMap.containsKey(c)) {
            initialReviewMap.put(c, c.getReviewStatus());
        }

        // if new review does not differ from initial state, no real review change occurred
        if(initialReviewMap.get(c).equals(status)) {
            newReviewMap.remove(c);
        } else {
            newReviewMap.put(c, status);
        }
        c.setReviewStatus(status);
    }

    @Override
    public boolean reviewChanged(Change c) {
        if(initialReviewMap.get(c) != null && newReviewMap.get(c) != null) {
            if (!initialReviewMap.get(c).equals(newReviewMap.get(c))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUncommittedReviews() {
        return !newReviewMap.isEmpty();
    }

    @Override
    public List<OWLOntologyChange> getReviewOntologyChanges() {
        List<OWLOntologyChange> changes = new ArrayList<>();
        for(Change c : newReviewMap.keySet()) {
            if(newReviewMap.get(c).equals(ReviewStatus.REJECTED)) {
                Set<OWLOntologyChange> ontChanges = c.getChanges();
                if (c.getBaselineChange().isPresent()) {
                    OWLOntologyChange baseline = c.getBaselineChange().get();
                    changes.add(getReverseChange(baseline));
                }
                changes.addAll(ontChanges.stream().map(this::getReverseChange).collect(Collectors.toList()));
            }
        }
        return changes;
    }

    private OWLOntologyChange getReverseChange(OWLOntologyChange change) {
        if(change.isAxiomChange()) {
            if (change.isAddAxiom()) {
                return new RemoveAxiom(change.getOntology(), change.getAxiom());
            }
            else if(change.isRemoveAxiom()) {
                return new AddAxiom(change.getOntology(), change.getAxiom());
            }
        }
        else if(change.isImportChange()) {
            if(change instanceof AddImport) {
                return new RemoveImport(change.getOntology(), ((ImportChange)change).getImportDeclaration());
            }
            else if(change instanceof RemoveImport) {
                return new AddImport(change.getOntology(), ((ImportChange)change).getImportDeclaration());
            }
        }
        else if (change instanceof AnnotationChange) {
            if(change instanceof AddOntologyAnnotation) {
                return new RemoveOntologyAnnotation(change.getOntology(), ((AnnotationChange)change).getAnnotation());
            }
            else if(change instanceof RemoveOntologyAnnotation) {
                return new AddOntologyAnnotation(change.getOntology(), ((AnnotationChange)change).getAnnotation());
            }
        }
        else if(change instanceof SetOntologyID) {
            return new SetOntologyID(change.getOntology(), ((SetOntologyID)change).getOriginalOntologyID().getOntologyIRI().get());
        }
        return null;
    }

    @Override
    public void clearUncommittedReviews() {
        for(Change c : newReviewMap.keySet()) {
            c.setReviewStatus(ReviewStatus.PENDING);
        }
        newReviewMap.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewManagerImpl that = (ReviewManagerImpl) o;
        return Objects.equal(newReviewMap, that.newReviewMap) &&
                Objects.equal(initialReviewMap, that.initialReviewMap);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newReviewMap, initialReviewMap);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("newReviewMap", newReviewMap)
                .add("initialReviewMap", initialReviewMap)
                .toString();
    }
}
