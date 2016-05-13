package org.protege.editor.owl.client.diff;

import org.protege.editor.owl.client.diff.model.Change;
import org.protege.editor.owl.client.diff.model.ChangeDetails;
import org.protege.editor.owl.client.diff.model.ChangeId;
import org.protege.editor.owl.client.diff.model.ChangeMode;
import org.protege.editor.owl.client.diff.model.ChangeType;
import org.protege.editor.owl.client.diff.model.CommitId;
import org.protege.editor.owl.client.diff.model.CommitMetadata;
import org.protege.editor.owl.client.diff.model.Review;
import org.protege.editor.owl.client.diff.model.ReviewStatus;
import org.protege.editor.owl.client.diff.model.RevisionTag;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface DiffFactory {

    /**
     * Create an instance of a Change
     *
     * @param changeId  Change unique identifier
     * @param changes   Set of OWL ontology changes
     * @param details   Change details
     * @param commitMetadata    Commit metadata
     * @param changeMode    Change mode
     * @return Change instance
     */
    Change createChange(ChangeId changeId, Set<OWLOntologyChange> changes, ChangeDetails details, CommitMetadata commitMetadata, ChangeMode changeMode);

    /**
     * Create an instance of a Change where the change identifier is an automatically generated UUID
     *
     * @param changes   Set of OWL ontology changes
     * @param details   Change details
     * @param commitMetadata    Commit metadata
     * @param changeMode    Change mode
     * @return Change instance
     */
    Change createChange(Set<OWLOntologyChange> changes, ChangeDetails details, CommitMetadata commitMetadata, ChangeMode changeMode);

    /**
     * Create a commit identifier
     *
     * @param commitId  Commit identifier
     * @return Commit identifier string
     */
    CommitId createCommitId(String commitId);

    /**
     * Create an instance of commit metadata
     *
     * @param commitId  Commit identifier
     * @param userId    User identifier
     * @param date  Commit date
     * @param comment   Commit comment
     * @return Commit metadata instance
     */
    CommitMetadata createCommitMetadata(CommitId commitId, String userId, Date date, String comment);

    /**
     * Create an instance of change details
     *
     * @param revisionTag   Revision tag
     * @param changeSubject Change subject
     * @param changeType    Change type
     * @param property  Annotation property that has been changed (optional)
     * @param newValue  New value of the annotation property (optional)
     * @return Change details instance
     */
    ChangeDetails createChangeDetails(RevisionTag revisionTag, OWLObject changeSubject, ChangeType changeType,
                                      Optional<OWLObject> property, Optional<String> newValue);

    /**
     * Create an instance of a revision tag
     *
     * @param revisionTag   Revision tag
     * @return Revision tag instance
     */
    RevisionTag createRevisionTag(String revisionTag);

    /**
     * Generate a random change identifier that is a UUID
     *
     * @return Change identifier
     */
    ChangeId createChangeId();

    /**
     * Generate a change identifier
     *
     * @param changeId  Change identifier
     * @return Change identifier
     */
    ChangeId createChangeId(String changeId);

    /**
     * Create a change review instance
     *
     * @param status    Review status
     * @param author    User identifier of reviewer
     * @param date  Review date
     * @param comment   Review-commit comment
     * @param isCommitted   true of the review has been committed, false otherwise
     * @return Review instance
     */
    Review createReview(ReviewStatus status, Optional<UserId> author, Optional<Date> date, Optional<String> comment, boolean isCommitted);
}
