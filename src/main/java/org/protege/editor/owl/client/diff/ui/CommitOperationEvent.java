package org.protege.editor.owl.client.diff.ui;

import java.util.Collections;
import java.util.List;

import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.semanticweb.owlapi.model.OWLOntologyChange;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitOperationEvent {

    private final DocumentRevision revision;
    private final RevisionMetadata metadata;
    private final List<OWLOntologyChange> changes;

    public CommitOperationEvent(DocumentRevision revision, RevisionMetadata metadata, List<OWLOntologyChange> changes) {
        this.revision = revision;
        this.metadata = metadata;
        this.changes = changes;
    }

    /**
     * Get the revision number assigned for the commit
     */
    public DocumentRevision getCommitRevision() {
        return revision;
    }

    /**
     * Get the metadata about the commit (e.g., commiter name, email, comment)
     */
    public RevisionMetadata getCommitMetadata() {
        return metadata;
    }

    /**
     * Get the list of changes associated with the commit.
     */
    public List<OWLOntologyChange> getCommitChanges() {
        return Collections.unmodifiableList(changes);
    }
}
