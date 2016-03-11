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
    private Map<ChangeId,ReviewStatus> newReviews = new HashMap<>();
    private Map<ChangeId,ReviewStatus> reviews = new HashMap<>();
    private LogDiff diff;

    /**
     * Constructor
     *
     * @param diff  Log diff engine
     */
    public ReviewManagerImpl(LogDiff diff) {
        this.diff = checkNotNull(diff);
    }

    @Override
    public void setReviewStatus(Change c, ReviewStatus status) {
        checkNotNull(c); checkNotNull(status);

        // gather initial status of the newly reviewed change
        if(!newReviews.containsKey(c.getId())) {
            reviews.put(c.getId(), c.getReviewStatus());
        }

        // if new review does not differ from initial state, no real review change occurred
        if(reviews.get(c.getId()).equals(status)) {
            newReviews.remove(c.getId());
        } else {
            newReviews.put(c.getId(), status);
        }
        c.setReviewStatus(status);
    }

    @Override
    public boolean reviewChanged(Change c) {
        if(reviews.get(c.getId()) != null && newReviews.get(c.getId()) != null) {
            if (!reviews.get(c.getId()).equals(newReviews.get(c.getId()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUncommittedReviews() {
        return !newReviews.isEmpty();
    }

    @Override
    public List<OWLOntologyChange> getReviewOntologyChanges() {
        List<OWLOntologyChange> changes = new ArrayList<>();
        for(ChangeId id : newReviews.keySet()) {
            Change c = diff.getChange(id);
            if(newReviews.get(c.getId()).equals(ReviewStatus.REJECTED)) {
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
        for(ChangeId id : newReviews.keySet()) {
            Change c = diff.getChange(id);
            c.setReviewStatus(ReviewStatus.PENDING);
        }
        newReviews.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewManagerImpl that = (ReviewManagerImpl) o;
        return Objects.equal(newReviews, that.newReviews) &&
                Objects.equal(reviews, that.reviews);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newReviews, reviews);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("newReviews", newReviews)
                .add("reviews", reviews)
                .toString();
    }
}
