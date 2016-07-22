package org.protege.editor.owl.client.diff.model;

import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.diff.DiffFactory;
import org.protege.editor.owl.client.diff.DiffFactoryImpl;
import org.protege.editor.owl.client.event.CommitOperationListener;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.protege.editor.owl.client.diff.model.LogDiffEvent.COMMIT_OCCURRED;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LogDiffManager implements Disposable {
    public static final String ALL_AUTHORS = "All Authors";
    private static DiffFactory diffFactory = new DiffFactoryImpl();
    private Set<LogDiffListener> listeners = new HashSet<>();
    private List<Change> selectedChanges = new ArrayList<>();
    private List<CommitMetadata> commits = new ArrayList<>();
    private ReviewManager reviewManager;
    private OWLModelManager modelManager;
    private OWLEditorKit editorKit;
    private String selectedAuthor;
    private CommitMetadata selectedCommit;
    private LogDiff diff;

    /**
     * Get the LogDiff manager
     *
     * @param modelManager  Model manager
     * @param editorKit Protege OWL editor kit
     * @return Log diff manager
     */
    public static LogDiffManager get(OWLModelManager modelManager, OWLEditorKit editorKit) {
        LogDiffManager diffManager = modelManager.get(LogDiffManager.class);
        if(diffManager == null) {
            diffManager = new LogDiffManager(modelManager, editorKit);
            modelManager.put(LogDiffManager.class, diffManager);
        }
        return diffManager;
    }

    /**
     * Get instance of a diff object factory
     *
     * @return Diff factory
     */
    public static DiffFactory getDiffFactory() {
        return diffFactory;
    }

    /**
     * Private constructor
     */
    private LogDiffManager(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.modelManager = checkNotNull(modelManager);
        this.editorKit = checkNotNull(editorKit);

        // add listeners
        ClientSession.getInstance(editorKit).addCommitOperationListener(commitListener);
        modelManager.getOWLOntologyManager().addOntologyChangeListener(ontologyChangeListener);
        modelManager.addListener(ontologyLoadListener);
    }

    public Optional<VersionedOWLOntology> getVersionedOntologyDocument() {
        VersionedOWLOntology vont = ClientSession.getInstance(editorKit).getActiveVersionOntology();
        return Optional.ofNullable(vont);
    }

    public OWLOntology getActiveOntology() {
        return modelManager.getActiveOntology();
    }

    private CommitOperationListener commitListener = event -> {
        statusChanged(COMMIT_OCCURRED);
    };

    private OWLOntologyChangeListener ontologyChangeListener = changes -> {
        clearSelections();
        statusChanged(LogDiffEvent.ONTOLOGY_UPDATED);
    };

    private OWLModelManagerListener ontologyLoadListener = event -> {
        if (event.isType(EventType.ONTOLOGY_LOADED) || event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED)) {
            clearSelections();
            statusChanged(LogDiffEvent.ONTOLOGY_UPDATED);
        }
    };

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

    public String getSelectedAuthor() {
        return selectedAuthor;
    }

    public void setSelectedAuthor(String userId) {
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

    public void setSelectedCommitToLatest() {
        setSelectedCommit(commits.get(0));
    }

    public List<CommitMetadata> getCommits(LogDiffEvent event) {
        VersionedOWLOntology vont = getVersionedOntologyDocument().get();
        ChangeHistory changes = vont.getChangeHistory();
        DocumentRevision base = changes.getBaseRevision();
        DocumentRevision head = changes.getHeadRevision();
        commits = new ArrayList<>();
        for (DocumentRevision rev = base.next(); rev.behindOrSameAs(head); rev = rev.next()) {
            RevisionMetadata metaData = changes.getMetadataForRevision(rev);
            if (event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) && getSelectedAuthor() != null &&
                    (metaData.getAuthorId().equals(getSelectedAuthor()) || getSelectedAuthor().equals(LogDiffManager.ALL_AUTHORS)) ||
                    event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                CommitMetadata c = diffFactory.createCommitMetadata(diffFactory.createCommitId(metaData.hashCode() + ""),
                        metaData.getAuthorId(), metaData.getDate(), metaData.getComment());
                commits.add(c);
            }
        }
        Collections.sort(commits);
        return commits;
    }

    public void clearSelections() {
        selectedAuthor = null;
        selectedCommit = null;
        selectedChanges.clear();
    }

    public LogDiff getDiffEngine() {
        if(diff == null) {
            diff = new LogDiff(this, modelManager);
            reviewManager = new ReviewManagerImpl(diff);
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
                ErrorLogPanel.showErrorDialog(e);
            }
        }
    }

    public ReviewManager getReviewManager() {
        return reviewManager;
    }

    public void commitChanges(List<OWLOntologyChange> changes) {
        modelManager.applyChanges(changes);
    }

    public List<OWLOntologyChange> removeCustomAnnotations() {
        OWLOntology ont = getActiveOntology();
        OWLAnnotationProperty property = modelManager.getOWLDataFactory().getOWLAnnotationProperty(AxiomChangeAnnotator.PROPERTY_IRI);
        Set<OWLAxiom> toRemove = new HashSet<>(), toAdd = new HashSet<>();
        for(OWLAxiom axiom : ont.getReferencingAxioms(property)) {
            if(axiom.isAnnotated()) {
                Set<OWLAnnotation> annotations = axiom.getAnnotations();
                for(OWLAnnotation ann : axiom.getAnnotations()) {
                    if(ann.getProperty().equals(property)) {
                        annotations.remove(ann);
                    }
                }
                toRemove.add(axiom);
                OWLAxiom axiomUnAnnotated = axiom.getAxiomWithoutAnnotations();
                OWLAxiom axiomReAnnottated = axiomUnAnnotated.getAnnotatedAxiom(annotations);
                toAdd.add(axiomReAnnottated);
            }
        }
        toRemove.addAll(ont.getDeclarationAxioms(property));
        List<OWLOntologyChange> changes = toRemove.stream().map(ax -> new RemoveAxiom(ont, ax)).collect(Collectors.toList());
        changes.addAll(toAdd.stream().map(ax -> new AddAxiom(ont, ax)).collect(Collectors.toList()));
        commitChanges(changes);
        return changes;
    }

    public void addCustomAnnotations(List<OWLOntologyChange> changes) {
        List<OWLOntologyChange> changeList = new ArrayList<>();
        for(OWLOntologyChange change : changes) {
            if(change.isAddAxiom()) {
                changeList.add(new RemoveAxiom(change.getOntology(), change.getAxiom()));
            }
            else if(change.isRemoveAxiom()) {
                changeList.add(new AddAxiom(change.getOntology(), change.getAxiom()));
            }
        }
        commitChanges(changeList);
    }

    @Override
    public void dispose() throws Exception {
        ClientSession.getInstance(editorKit).removeCommitOperationListener(commitListener);
        modelManager.getOWLOntologyManager().removeOntologyChangeListener(ontologyChangeListener);
        modelManager.removeListener(ontologyLoadListener);
    }
}
