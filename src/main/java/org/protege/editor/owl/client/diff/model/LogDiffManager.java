package org.protege.editor.owl.client.diff.model;

import org.protege.editor.core.Disposable;
import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.connect.ServerConnectionManager;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.owl.server.api.UserId;
import org.protege.owl.server.api.client.Client;
import org.protege.owl.server.api.client.VersionedOntologyDocument;
import org.protege.owl.server.api.exception.OWLServerException;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LogDiffManager implements Disposable {
    public static final UserId ALL_AUTHORS = new UserId("All authors");
    private Set<LogDiffListener> listeners = new HashSet<>();
    private List<Change> selectedChanges = new ArrayList<>();
    private OWLModelManager modelManager;
    private ReviewManager reviewManager;
    private OWLEditorKit editorKit;

    private UserId selectedAuthor;
    private CommitMetadata selectedCommit;
    private LogDiff diff;

    public static LogDiffManager get(OWLModelManager modelManager, OWLEditorKit editorKit) {
        LogDiffManager diffManager = modelManager.get(LogDiffManager.class);
        if(diffManager == null) {
            diffManager = new LogDiffManager(modelManager, editorKit);
            modelManager.put(LogDiffManager.class, diffManager);
        }
        return diffManager;
    }

    /**
     * Private constructor
     */
    private LogDiffManager(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.modelManager = checkNotNull(modelManager);
        this.editorKit = checkNotNull(editorKit);
        this.reviewManager = new ReviewManager();
    }

    public Optional<VersionedOntologyDocument> getVersionedOntologyDocument() {
        ServerConnectionManager connectionManager = ServerConnectionManager.get(editorKit);
        VersionedOntologyDocument vont = connectionManager.getVersionedOntology(getActiveOntology());
        return Optional.ofNullable(vont);
    }

    public Optional<Client> getCurrentClient() {
        Client client = null;
        try {
            client = ServerConnectionManager.get(editorKit).createClient(getActiveOntology());
        } catch (OWLServerException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(client);
    }

    public OWLOntology getActiveOntology() {
        return modelManager.getActiveOntology();
    }

    public Change getFirstSelectedChange() {
        checkNotNull(selectedChanges);
        return selectedChanges.get(0);
    }

    public List<Change> getSelectedChanges() {
        return selectedChanges;
    }

    public void setSelectedChanges(List<Change> selectedChanges) {
        this.selectedChanges = checkNotNull(selectedChanges);
        statusChanged(LogDiffEvent.CHANGE_SELECTION_CHANGED);
    }

    public void clearSelectedChanges() {
        selectedChanges.clear();
        statusChanged(LogDiffEvent.CHANGE_SELECTION_CHANGED);
    }

    public UserId getSelectedAuthor() {
        return selectedAuthor;
    }

    public void setSelectedAuthor(UserId userId) {
        this.selectedAuthor = userId;
        statusChanged(LogDiffEvent.AUTHOR_SELECTION_CHANGED);
    }

    public CommitMetadata getSelectedCommit() {
        return selectedCommit;
    }

    public void setSelectedCommit(CommitMetadata selectedCommit) {
        this.selectedCommit = selectedCommit;
        statusChanged(LogDiffEvent.COMMIT_SELECTION_CHANGED);
    }

    public void clearSelections() {
        selectedAuthor = null;
        selectedCommit = null;
        selectedChanges.clear();
    }

    public LogDiff getDiffEngine() {
        if(diff == null) {
            diff = new LogDiff(this, modelManager);
        }
        return diff;
    }

    public void addListener(LogDiffListener listener) {
        listeners.add(checkNotNull(listener));
    }

    public void removeListener(LogDiffListener listener) {
        listeners.remove(checkNotNull(listener));
    }

    public void statusChanged(LogDiffEvent event) {
        checkNotNull(event);
        for(LogDiffListener listener : listeners) {
            try {
                listener.statusChanged(event);
            } catch(Exception e) {
                ProtegeApplication.getErrorLog().logError(e);
            }
        }
    }

    public ReviewManager getReviewManager() {
        return reviewManager;
    }

    public UserId getAllAuthorsUserId() {
        return ALL_AUTHORS;
    }

    @Override
    public void dispose() throws Exception {

    }
}