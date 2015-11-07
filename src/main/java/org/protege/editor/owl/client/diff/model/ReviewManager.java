package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ReviewManager {
    private Map<Change,ChangeReviewStatus> newReviewMap = new HashMap<>();
    private Map<Change,ChangeReviewStatus> initialReviewMap = new HashMap<>();

    /**
     * No-args constructor
     */
    public ReviewManager() { }

    public void addReview(Change c, ChangeReviewStatus status) {
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

    public boolean reviewChanged(Change c) {
        if(initialReviewMap.get(c) != null && newReviewMap.get(c) != null) {
            if (!initialReviewMap.get(c).equals(newReviewMap.get(c))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUncommittedReviews() {
        return !newReviewMap.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewManager that = (ReviewManager) o;
        return Objects.equal(newReviewMap, that.newReviewMap) &&
                Objects.equal(initialReviewMap, that.initialReviewMap);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newReviewMap, initialReviewMap);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("newReviewMap", newReviewMap)
                .add("initialReviewMap", initialReviewMap)
                .toString();
    }
}
