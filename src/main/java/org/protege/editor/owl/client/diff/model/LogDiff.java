package org.protege.editor.owl.client.diff.model;

import org.apache.log4j.Logger;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.OntologyDocumentRevision;
import org.protege.owl.server.api.UserId;
import org.protege.owl.server.api.client.VersionedOntologyDocument;
import org.semanticweb.owlapi.model.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LogDiff {
    private static final Logger log = Logger.getLogger(LogDiff.class);
    private final LogDiffManager diffManager;
    private final OWLModelManager modelManager;
    private List<Change> allChanges = new ArrayList<>();

    /**
     * Constructor
     *
     * @param diffManager   Diff manager
     * @param modelManager  OWL model manager
     */
    public LogDiff(LogDiffManager diffManager, OWLModelManager modelManager) {
        this.diffManager = checkNotNull(diffManager);
        this.modelManager = checkNotNull(modelManager);
    }

    public void initDiff() {
        findAllChanges();
        matchOldValues();
        findConflits(allChanges);
    }

    /**
     * Get all changes since the beginning of time
     */
    public void findAllChanges() {
        if(diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOntologyDocument vont = diffManager.getVersionedOntologyDocument().get();
            OWLOntology ontology = modelManager.getActiveOntology();
            ChangeHistory changes = vont.getLocalHistory();
            OntologyDocumentRevision rev = changes.getStartRevision();
            while (changes.getMetaData(rev) != null) {
                ChangeMetaData metaData = changes.getMetaData(rev);
                ChangeHistory hist = changes.cropChanges(rev, rev.next());
                allChanges.addAll(getChangeListFromHistory(hist.getChanges(ontology), metaData));
                rev = rev.next();
            }
        }
    }

    public List<Change> getChangesToDisplay(LogDiffEvent event) {
        List<Change> changesToDisplay = new ArrayList<>();
        if(diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOntologyDocument vont = diffManager.getVersionedOntologyDocument().get();
            ChangeHistory changes = vont.getLocalHistory();
            changesToDisplay = getChangesToDisplay(event, changes.getStartRevision(), changes.getEndRevision());
        }
        return changesToDisplay;
    }

    public List<Change> getChangesToDisplay(LogDiffEvent event, OntologyDocumentRevision start, OntologyDocumentRevision end) {
        List<Change> changesToDisplay = new ArrayList<>();
        OWLOntology ontology = modelManager.getActiveOntology();
        if(diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOntologyDocument vont = diffManager.getVersionedOntologyDocument().get();
            ChangeHistory changes = vont.getLocalHistory();
            OntologyDocumentRevision rev = start;
            while (changes.getMetaData(rev) != null) {
                ChangeMetaData metaData = changes.getMetaData(rev);
                if ((event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) && isCommitBySelectedAuthor(metaData)) ||
                        (event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED) && isCommitSelected(metaData)) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                    ChangeHistory hist = changes.cropChanges(rev, rev.next());
                    changesToDisplay.addAll(getChangeList(hist.getChanges(ontology)));
                }
                rev = rev.next();
                if(rev.equals(end)) {
                    break;
                }
            }
        }
        return changesToDisplay;
    }

    private boolean isCommitBySelectedAuthor(ChangeMetaData metaData) {
        return (diffManager.getSelectedAuthor() != null) &&
                (metaData.getUserId().equals(diffManager.getSelectedAuthor()) || diffManager.getSelectedAuthor().equals(diffManager.getAllAuthorsUserId()));
    }

    private boolean isCommitSelected(ChangeMetaData metaData) {
        return (diffManager.getSelectedCommit() != null) && (metaData.hashCode() == diffManager.getSelectedCommit().getHashcode());
    }

    private List<Change> getChangeList(List<OWLOntologyChange> ontologyChanges) {
        List<Change> changes = new ArrayList<>();
        for(OWLOntologyChange change : ontologyChanges) {
            for (Change parsedChange : allChanges) {
                if (parsedChange.getChanges().contains(change)) {
                    if(!changes.contains(parsedChange)) {
                        changes.add(parsedChange);
                    }
                    break;
                }
            }
        }
        return changes;
    }

    private List<Change> getChangeListFromHistory(List<OWLOntologyChange> ontChanges, ChangeMetaData metaData) {
        List<Change> changeList = new ArrayList<>();
        UserId author = metaData.getUserId();
        Date date = metaData.getDate();
        String comment = metaData.getCommitComment();
        if(comment == null) {
            comment = "";
        }
        for(OWLOntologyChange ontChange : ontChanges) {
            Change change = getChangeObject(ontChange, date, author, comment);
            if (change != null && !changeList.contains(change)) {
                changeList.add(change);
            }
        }
        return changeList;
    }

    private Change getChangeObject(OWLOntologyChange ontChange, Date date, UserId author, String comment) {
        Set<OWLOntologyChange> changeAxiomSet = new HashSet<>();
        changeAxiomSet.add(ontChange);
        Change change = null;
        if(ontChange.isAxiomChange()) {
            OWLAxiom axiom = ontChange.getAxiom();
            if(axiom.isAnnotated()) {
                // TODO: Look for annotations that are known to cast one or more axioms as a composite change, such as a split or merge
            }
            OntologyChangeVisitor visitor = new OntologyChangeVisitor();
            axiom.accept(visitor);
            OWLObject ce = visitor.getChangeSubject();
            if(ce != null) {
                change = new ChangeImpl(changeAxiomSet, getChangeMode(ontChange), date, author, visitor.getChangeSubject(), visitor.getChangeType(),
                        visitor.getProperty(), visitor.getChangeObject(), comment);
            }
        }
        else if(ontChange.isImportChange()) {
            ImportChange importChange = (ImportChange) ontChange;
            OWLImportsDeclaration importDecl = importChange.getImportDeclaration();
            change = new ChangeImpl(changeAxiomSet, getChangeMode(ontChange), date, author, ontChange.getOntology().getOntologyID().getOntologyIRI(),
                    BuiltInChangeType.IMPORT, Optional.empty(), Optional.of(getQuotedIri(importDecl.getIRI())), comment);
        }
        else if(ontChange instanceof AnnotationChange) { // possible OWLOntologyChange type not covered by OWLOntologyChange.isXXX() methods
            AnnotationChange annotationChange = (AnnotationChange) ontChange;
            OWLAnnotation annotation = annotationChange.getAnnotation();
            change = new ChangeImpl(changeAxiomSet, getChangeMode(ontChange), date, author, ontChange.getOntology().getOntologyID().getOntologyIRI(),
                    BuiltInChangeType.ONTOLOGY_ANNOTATION, Optional.of(annotation.getProperty()), Optional.of(annotation.getValue().toString()), comment);
        }
        else if(ontChange instanceof SetOntologyID) { // another possible OWLOntologyChange not covered by OWLOntologyChange.isXXX() methods
            SetOntologyID setOntologyID = (SetOntologyID) ontChange;
            IRI newIri = setOntologyID.getNewOntologyID().getOntologyIRI();
            change = new ChangeImpl(changeAxiomSet, ChangeMode.ONTOLOGY_IRI, date, author, setOntologyID.getNewOntologyID().getOntologyIRI(),
                    BuiltInChangeType.ONTOLOGY_IRI, Optional.empty(), Optional.of(getQuotedIri(newIri)), comment);
        }
        else {
            log.error("Unhandled ontology change type for change: " + ontChange);
        }
        return change;
    }

    private void findConflits(List<Change> changes) {
        ConflictDetector conflictDetector = new ConflictDetector(changes);
        for (Change change : changes) {
            Set<Change> conflicting = conflictDetector.getConflictingChanges(change);
            change.addConflictingChanges(conflicting);
        }
    }

    private void matchOldValues() {
        Set<Change> toRemove = new HashSet<>();
        for(Change c : allChanges) {
            // only modify addition; the corresponding removal will be the "baseline" for the (addition) change
            if(c.getType().isBuiltInType() && c.getChangeMode().equals(ChangeMode.ADDITION)) {
                Set<Change> matches = findMatchingChanges(c);
                if (matches.size() == 1) {
                    Change c2 = matches.iterator().next();
                    if ((c.isOfType(BuiltInChangeType.ANNOTATION) || c.isOfType(BuiltInChangeType.ONTOLOGY_ANNOTATION)) &&
                            c2.getAnnotationProperty().get().equals(c.getAnnotationProperty().get()) && axiomTypesMatch(c, c2)) {
                        c.setPriorValue(c2.getValue().get());
                        toRemove.add(c2);
                        c.setType(new ChangeTypeImpl(c.getType().getDisplayName(), Optional.of(GuiUtils.DEFAULT_CHANGE_COLOR)));
                    } else if (c.isOfType(BuiltInChangeType.LOGICAL) && axiomTypesMatch(c, c2)) {
                        c.setBaselineChange(c2.getChanges().iterator().next());
                        toRemove.add(c2);
                        c.setType(new ChangeTypeImpl(c.getType().getDisplayName(), Optional.of(GuiUtils.DEFAULT_CHANGE_COLOR)));
                    }
                }
            }
        }
        allChanges.removeAll(toRemove);
    }

    private boolean axiomTypesMatch(Change change1, Change change2) {
        if((change1.getChangeMode().equals(ChangeMode.ADDITION) && change2.getChangeMode().equals(ChangeMode.REMOVAL)) ||
                (change1.getChangeMode().equals(ChangeMode.REMOVAL) && change2.getChangeMode().equals(ChangeMode.ADDITION))) {
            // assuming single ontology change here, since these are built-in type changes
            OWLOntologyChange c1 = change1.getChanges().iterator().next();
            OWLOntologyChange c2 = change2.getChanges().iterator().next();
            if (c1.isAxiomChange() && c2.isAxiomChange()) {
                if (c1.getAxiom().getAxiomType().equals(c2.getAxiom().getAxiomType())) {
                    return true;
                }
            }
            else if(c1 instanceof AnnotationChange && c2 instanceof AnnotationChange) {
                return true;
            }
        }
        return false;
    }

    private Set<Change> findMatchingChanges(Change c) {
        Set<Change> matches = new HashSet<>();
        UserId author = c.getAuthor();
        Date date = c.getDate();
        OWLObject subject = c.getSubject();
        ChangeType type = c.getType();
        for (Change c2 : allChanges) {
            if (!c.equals(c2) && c2.getAuthor().equals(author) && c2.getDate().equals(date) && c2.getSubject().equals(subject) && c2.getType().equals(type)) {
                // TODO: match RHS expression types for axioms that can be reduced to SubClassOf axioms
                matches.add(c2);
            }
        }
        return matches;
    }

    private String getQuotedIri(IRI iri) {
        return "\"" + iri.toString() + "\"";
    }

    private ChangeMode getChangeMode(OWLOntologyChange change) {
        ChangeMode mode = null;
        if(change instanceof AddOntologyAnnotation || change instanceof AddImport || change instanceof AddAxiom) {
            mode = ChangeMode.ADDITION;
        }
        else if(change instanceof RemoveOntologyAnnotation || change instanceof RemoveImport || change instanceof RemoveAxiom) {
            mode = ChangeMode.REMOVAL;
        }
        return mode;
    }

    public List<Change> getChanges() {
        return allChanges;
    }

    public void clearChanges() {
        allChanges.clear();
    }
}
