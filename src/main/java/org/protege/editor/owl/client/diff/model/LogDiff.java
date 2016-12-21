package org.protege.editor.owl.client.diff.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.diff.DiffFactory;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LogDiff {
    private static final Logger logger = LoggerFactory.getLogger(LogDiff.class.getName());
    private static final DocumentRevision INITIAL_COMMIT_REVISION = DocumentRevision.create(1);
    private final LogDiffManager diffManager;
    private final OWLModelManager modelManager;
    private Map<ChangeId, Change> changeMap = new HashMap<>();
    private Multimap<String,ChangeId> changesByUser = HashMultimap.create();
    private Multimap<Date,ChangeId> changesByDate = HashMultimap.create();
    private Multimap<OWLObject,ChangeId> changesBySubject = HashMultimap.create();
    private DiffFactory diffFactory;

    /**
     * Constructor
     *
     * @param diffManager  Diff manager
     * @param modelManager OWL model manager
     */
    public LogDiff(LogDiffManager diffManager, OWLModelManager modelManager) {
        this.diffManager = checkNotNull(diffManager);
        this.modelManager = checkNotNull(modelManager);
        this.diffFactory = LogDiffManager.getDiffFactory();
    }

    /**
     * Process all ontology revisions and compute changes
     */
    public void initDiff() {
        if (diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOWLOntology vont = diffManager.getVersionedOntologyDocument().get();
            OWLOntology ontology = diffManager.getActiveOntology();
            ChangeHistory changes = vont.getChangeHistory();
            DocumentRevision base = changes.getBaseRevision();
            DocumentRevision head = changes.getHeadRevision();
            for (DocumentRevision rev = base.next(); rev.behindOrSameAs(head); rev = rev.next()) {
                ChangeHistory hist = ChangeHistoryUtils.crop(changes, rev.previous(), 1);
                RevisionMetadata metaData = hist.getMetadataForRevision(rev);
                findRevisionChanges(ChangeHistoryUtils.getOntologyChanges(hist, ontology), metaData);
                if(!rev.equals(INITIAL_COMMIT_REVISION)) {
                    findBaselineMatches(changeMap.values());
                    findConflits(changeMap.values());
                }
            }
        }
    }

    /**
     * Compute the changes that occurred in the given commit
     *
     * @param ontChanges    List of OWL ontology changes
     * @param metaData  Metadata regarding the commit
     */
    private void findRevisionChanges(List<OWLOntologyChange> ontChanges, RevisionMetadata metaData) {
        String commitComment = (metaData.getComment() != null ? metaData.getComment() : "");
        // produce a revision tag that uses the hashcode of the commit metadata
        RevisionTag revisionTag = getRevisionTag(metaData.hashCode() + "");
        CommitMetadata commitMetadata = diffFactory.createCommitMetadata(diffFactory.createCommitId(metaData.hashCode()+""), metaData.getAuthorId(), metaData.getDate(), commitComment);
        Multimap<ChangeDetails, OWLOntologyChange> multimap = HashMultimap.create();
        ontChanges.stream().filter(ontChange -> !isCustomPropertyDeclaration(ontChange)).forEach(ontChange -> {
            if (isAnnotated(ontChange)) { // custom change
                ChangeDetails details = getChangeDetailsFromAnnotatedAxiom(ontChange.getAxiom());
                multimap.put(details, ontChange);
            } else {
                Optional<Change> change = getChangeObject(ontChange, commitMetadata, revisionTag);
                if (change.isPresent()) {
                    add(change.get());
                }
            }
        });
        for (ChangeDetails details : multimap.keySet()) {
            Set<OWLOntologyChange> changeList = (Set<OWLOntologyChange>) multimap.get(details);
            Change c = diffFactory.createChange(changeList, details, commitMetadata, ChangeMode.CUSTOM);
            add(c);
        }
    }

    /**
     * Get a change object based on the given OWL ontology change, commit metadata and revision tag
     *
     * @param ontChange OWL ontology change
     * @param commitMetadata    Commit metadata
     * @param revisionTag   Revision tag
     * @return Change instance
     */
    private Optional<Change> getChangeObject(OWLOntologyChange ontChange, CommitMetadata commitMetadata, RevisionTag revisionTag) {
        Set<OWLOntologyChange> changeAxiomSet = new HashSet<>();
        changeAxiomSet.add(ontChange);
        Change change = null;
        if (ontChange.isAxiomChange()) {
            OWLAxiom axiom = ontChange.getAxiom();
            OntologyChangeVisitor visitor = new OntologyChangeVisitor();
            axiom.accept(visitor);
            OWLObject ce = visitor.getChangeSubject();
            if (ce != null) {
                ChangeDetails changeDetails = diffFactory.createChangeDetails(revisionTag, visitor.getChangeSubject(),
                        visitor.getChangeType(), visitor.getProperty(), visitor.getChangeObject());
                change = diffFactory.createChange(changeAxiomSet, changeDetails, commitMetadata, getChangeMode(ontChange));
            }
        } else if (ontChange.isImportChange()) {
            ImportChange importChange = (ImportChange) ontChange;
            OWLImportsDeclaration importDecl = importChange.getImportDeclaration();
            ChangeDetails changeDetails = diffFactory.createChangeDetails(revisionTag, ontChange.getOntology().getOntologyID().getOntologyIRI().get(),
                    BuiltInChangeType.IMPORT, Optional.empty(), Optional.of(getQuotedIri(importDecl.getIRI())));
            change = diffFactory.createChange(changeAxiomSet, changeDetails, commitMetadata, getChangeMode(ontChange));
        } else if (ontChange instanceof AnnotationChange) { // possible OWLOntologyChange type not covered by OWLOntologyChange.isXXX() methods
            AnnotationChange annotationChange = (AnnotationChange) ontChange;
            OWLAnnotation annotation = annotationChange.getAnnotation();
            ChangeDetails changeDetails = diffFactory.createChangeDetails(revisionTag, ontChange.getOntology().getOntologyID().getOntologyIRI().get(),
                    BuiltInChangeType.ONTOLOGY_ANNOTATION, Optional.of(annotation.getProperty()), Optional.of(annotation.getValue().toString()));
            change = diffFactory.createChange(changeAxiomSet, changeDetails, commitMetadata, getChangeMode(ontChange));
        } else if (ontChange instanceof SetOntologyID) { // another possible OWLOntologyChange not covered by OWLOntologyChange.isXXX() methods
            SetOntologyID setOntologyID = (SetOntologyID) ontChange;
            IRI newIri = setOntologyID.getNewOntologyID().getOntologyIRI().get();
            ChangeDetails changeDetails = diffFactory.createChangeDetails(revisionTag, setOntologyID.getNewOntologyID().getOntologyIRI().get(),
                    BuiltInChangeType.ONTOLOGY_IRI, Optional.empty(), Optional.of(getQuotedIri(newIri)));
            change = diffFactory.createChange(changeAxiomSet, changeDetails, commitMetadata, ChangeMode.ONTOLOGY_IRI);
        } else {
            logger.error("Unhandled ontology change type for change: " + ontChange);
        }
        return Optional.ofNullable(change);
    }

    /**
     * Get the instance of ChangeDetails for the given (annotated) axiom
     *
     * @param axiom OWL axiom
     * @return Change details
     */
    private ChangeDetails getChangeDetailsFromAnnotatedAxiom(OWLAxiom axiom) {
        ChangeDetails details = null;
        for (OWLAnnotation annotation : axiom.getAnnotations()) {
            if (annotation.getProperty().getIRI().equals(AxiomChangeAnnotator.PROPERTY_IRI)) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    String value = ((OWLLiteral) annotation.getValue()).getLiteral();
                    String[] tokens = value.split(AxiomChangeAnnotator.getSeparatorRegex());
                    RevisionTag tag = diffFactory.createRevisionTag(tokens[0]);
                    OWLEntity changeSubject = getEntityFromIri(IRI.create(tokens[1]));
                    String changeType = tokens[2];
                    Color color = null;
                    if (changeType.contains(AxiomChangeAnnotator.ALT_SEPARATOR)) {
                        String[] typeTokens = changeType.split(AxiomChangeAnnotator.getAltSeparatorRegex());
                        changeType = typeTokens[0];
                        color = new Color(Integer.parseInt(typeTokens[1]));
                    }
                    ChangeType type = new CustomChangeType(changeType, Optional.ofNullable(color));
                    OWLEntity property = null;
                    if (tokens.length > 2) {
                        IRI iri = null;
                        try {
                            iri = IRI.create(tokens[3]);
                        } catch (Exception ignored) {
                            // ignore
                        }
                        property = (iri != null ? getEntityFromIri(iri) : null);
                    }
                    String newValue = (tokens.length > 3 ? tokens[4] : null);
                    details = diffFactory.createChangeDetails(tag, changeSubject, type, Optional.ofNullable(property), Optional.ofNullable(newValue));
                    break; // expecting only a single custom annotation per axiom
                }
            }
        }
        return details;
    }

    /**
     * Get list of changes to be displayed for the given event
     *
     * @param event Event
     * @return List of changes
     */
    public List<Change> getChangesToDisplay(LogDiffEvent event) {
        List<Change> changes = new ArrayList<>();
        if (event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED)) {
            String userId = diffManager.getSelectedAuthor();
            changes = getChangesForUser(userId);
        }
        else if(event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED)) {
            CommitMetadata metadata = diffManager.getSelectedCommit();
            Collection<ChangeId> dateChangeIds = changesByDate.get(metadata.getDate());
            changes = getChangesForCommit(metadata, dateChangeIds);
        }
        else if(event.equals(LogDiffEvent.ONTOLOGY_UPDATED) || event.equals(LogDiffEvent.COMMIT_OCCURRED)) {
            clear();
            initDiff();
            changes = changeMap.values().stream().collect(Collectors.toList());
        }
        return changes;
    }

    /**
     * Verify whether the given OWL ontology change is a declaration axiom for the
     * annotation property used internally by the Protege client-server
     *
     * @param change    OWL ontology change
     * @return true if ontology change is the declaration of custom annotation property, false otherwise
     */
    private boolean isCustomPropertyDeclaration(OWLOntologyChange change) {
        if (change.isAxiomChange() && change.getAxiom().isOfType(AxiomType.DECLARATION)) {
            if (((OWLDeclarationAxiom) change.getAxiom()).getEntity().getIRI().equals(AxiomChangeAnnotator.PROPERTY_IRI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify whether the given OWL ontology change is annotated with the
     * annotation property used for custom changes
     *
     * @param change    OWL ontology change
     * @return true if change is annotated
     */
    private boolean isAnnotated(OWLOntologyChange change) {
        if (change.isAxiomChange() && change.getAxiom().isAnnotated()) {
            for (OWLAnnotation annotation : change.getAxiom().getAnnotations()) {
                if (annotation.getProperty().getIRI().equals(AxiomChangeAnnotator.PROPERTY_IRI)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get a revision tag by hashing the given string
     *
     * @param string    Revision tag string
     * @return Revision tag
     */
    private RevisionTag getRevisionTag(String string) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        md.update(string.getBytes());
        byte[] digestBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digestBytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return diffFactory.createRevisionTag(sb.toString().substring(0, 8));
    }

    /**
     * Get OWL entity that corresponds to the given IRI
     *
     * @param iri   IRI
     * @return OWL entity
     */
    private OWLEntity getEntityFromIri(IRI iri) {
        OWLEntity entity = null;
        Set<OWLEntity> sig = modelManager.getActiveOntology().getSignature();
        for (OWLEntity e : sig) {
            if (e.getIRI().equals(iri)) {
                entity = e;
                break;
            }
        }
        if (entity == null) {
            logger.error("The given IRI does not exist in active ontology (" + iri.toString() + ")");
        }
        return entity;
    }

    /**
     * Find conflicting changes within the specified collection of changes
     *
     * @param searchSpace   Collection of changes within which to search
     */
    private void findConflits(Collection<Change> searchSpace) {
        ConflictDetector conflictDetector = new SimpleConflictDetector();
        for (Change change : searchSpace) {
            List<Change> subjectChanges = getChangesForSubject(change.getDetails().getSubject());
            Set<ChangeId> conflicting = conflictDetector.getConflictingChanges(change, subjectChanges);
            conflicting.forEach(change::addConflictingChange);
            changeMap.put(change.getId(), change);
        }
    }

    public Change getChange(ChangeId changeId) {
        return changeMap.get(changeId);
    }

    /**
     * Get a list of changes (within the given change collection) that occurred in the specified commit
     *
     * @param metadata  Commit metadata
     * @param searchSpace   Collection of change identifiers that constitutes the search space within
     *                      which to look for changes
     * @return List of changes
     */
    private List<Change> getChangesForCommit(CommitMetadata metadata, Collection<ChangeId> searchSpace) {
        List<Change> changes = new ArrayList<>();
        for (ChangeId id : searchSpace) {
            Change c = changeMap.get(id);
            if (c.getCommitMetadata().getCommitId().equals(metadata.getCommitId())) {
                changes.add(c);
            }
        }
        return changes;
    }

    /**
     * Get the collection of changes carried out on the specified OWL object
     *
     * @param owlObject OWL object
     * @return Collection of changes
     */
    public List<Change> getChangesForSubject(OWLObject owlObject) {
        Collection<ChangeId> changeIds = changesBySubject.get(owlObject);
        return changeIds.stream().map(id -> changeMap.get(id)).collect(Collectors.toList());
    }

    /**
     * Get the collection of changes carried out by the user with the specified identifier
     *
     * @param userId    User identifier
     * @return List of changes
     */
    public List<Change> getChangesForUser(String userId) {
        if (userId.equals(LogDiffManager.ALL_AUTHORS)) {
            return changeMap.keySet().stream().map(id -> changeMap.get(id)).collect(Collectors.toList());
        } else {
            Collection<ChangeId> changeIds = changesByUser.get(userId);
            return changeIds.stream().map(id -> changeMap.get(id)).collect(Collectors.toList());
        }
    }

    /**
     * Search for previous values of the given changes
     *
     * @param changes   Set of changes
     */
    private void findBaselineMatches(Collection<Change> changes) {
        Set<Change> toRemove = new HashSet<>();
        for (Change c : changes) {
            // only modify addition; the corresponding removal will be the "baseline" for the (addition) change,
            // and will get removed after an alignment is established
            if (c.getDetails().getType().isBuiltInType() && c.getMode().equals(ChangeMode.ADDITION)) {
                Set<Change> matches = getMatchingChanges(c);
                if (matches.size() == 1) {
                    Change c2 = matches.iterator().next();
                    if (((c.isOfType(BuiltInChangeType.ANNOTATION) || c.isOfType(BuiltInChangeType.ONTOLOGY_ANNOTATION))
                            && c2.getDetails().getProperty().isPresent()
                            && c2.getDetails().getProperty().get().equals(c.getDetails().getProperty().get())) ||
                            c.isOfType(BuiltInChangeType.LOGICAL)) {
                        c.setBaselineChange(c2.getChanges().iterator().next()); // non-custom changes have 1 OWL ontology change
                        c.setMode(ChangeMode.ALIGNED);
                        toRemove.add(c2);
                    }
                }
            }
        }
        toRemove.forEach(this::remove);
    }

    /**
     * Search for changes that can be aligned with the given one
     *
     * @param c Change
     * @return Set of changes that match with given one
     */
    private Set<Change> getMatchingChanges(Change c) {
        Set<Change> matches = new HashSet<>();
        CommitMetadata commitMetadata = c.getCommitMetadata();
        ChangeDetails changeDetails = c.getDetails();
        Collection<ChangeId> changes = changesBySubject.get(changeDetails.getSubject());
        for (Change change : getChangesForCommit(commitMetadata, changes)) {
            if (change.getDetails().getType().equals(changeDetails.getType())) {
                if (isMatchable(c, change)) {
                    // TODO: match RHS expression types for axioms that can be reduced to SubClassOf axioms
                    if (c.getDetails().getProperty().isPresent() && change.getDetails().getProperty().isPresent()) {
                        if (c.getDetails().getProperty().get().equals(change.getDetails().getProperty().get()) && !c.equals(change)) {
                            matches.add(change);
                        }
                    } else if (!c.equals(change)) {
                        matches.add(change);
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Verify whether the given pair of changes stand for opposite actions (i.e., one is addition the other a removal, or vice-versa),
     * and have the same axiom type (if axiom changes) or are both ontology annotations
     *
     * @param change1   Change
     * @param change2   Change
     * @return true if changes have the same type
     */
    private boolean isMatchable(Change change1, Change change2) {
        if ((change1.getMode().equals(ChangeMode.ADDITION) && change2.getMode().equals(ChangeMode.REMOVAL)) ||
                (change1.getMode().equals(ChangeMode.REMOVAL) && change2.getMode().equals(ChangeMode.ADDITION))) {
            // assuming single ontology change here, since these are built-in type changes
            OWLOntologyChange c1 = change1.getChanges().iterator().next();
            OWLOntologyChange c2 = change2.getChanges().iterator().next();
            if (c1.isAxiomChange() && c2.isAxiomChange()) {
                if (c1.getAxiom().getAxiomType().equals(c2.getAxiom().getAxiomType())) {
                    return true;
                }
            } else if (c1 instanceof AnnotationChange && c2 instanceof AnnotationChange) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a string that corresponds to the given IRI quoted
     *
     * @param iri   IRI
     * @return String that is the quoted IRI
     */
    private String getQuotedIri(IRI iri) {
        return "\"" + iri.toString() + "\"";
    }

    /**
     * Get the change mode for the given OWL ontology change, that is, whether it is an added or removed axiom,
     * an ontology IRI change, or a custom change.
     *
     * @param change    OWL ontology change
     * @return Change mode
     */
    public static ChangeMode getChangeMode(OWLOntologyChange change) {
        if (change instanceof AddOntologyAnnotation || change instanceof AddImport || change instanceof AddAxiom) {
            return ChangeMode.ADDITION;
        } else if (change instanceof RemoveOntologyAnnotation || change instanceof RemoveImport || change instanceof RemoveAxiom) {
            return ChangeMode.REMOVAL;
        } else if (change instanceof SetOntologyID) {
            return ChangeMode.ONTOLOGY_IRI;
        } else {
            return ChangeMode.CUSTOM;
        }
    }

    /**
     * Get the collection of all changes
     *
     * @return Collection of changes
     */
    public Collection<Change> getChanges() {
        return changeMap.values();
    }

    /**
     * Add a change to change data structures
     *
     * @param change Change
     */
    private void add(Change change) {
        CommitMetadata commitMetadata = change.getCommitMetadata();
        changeMap.put(change.getId(), change);
        changesByUser.put(commitMetadata.getAuthor(), change.getId());
        changesByDate.put(commitMetadata.getDate(), change.getId());
        changesBySubject.put(change.getDetails().getSubject(), change.getId());
    }

    /**
     * Remove a change from the change data structures
     *
     * @param change Change
     */
    private void remove(Change change) {
        ChangeId id = change.getId();
        CommitMetadata commitMetadata = change.getCommitMetadata();
        changeMap.remove(id);
        changesByUser.remove(commitMetadata.getAuthor(), id);
        changesByDate.remove(commitMetadata.getDate(), id);
        changesBySubject.remove(change.getDetails().getSubject(), id);
    }

    /**
     * Clear all changes
     */
    public void clear() {
        changeMap.clear();
        changesByUser.clear();
        changesByDate.clear();
        changesBySubject.clear();
    }
}