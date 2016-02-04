package org.protege.editor.owl.client.diff.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class AxiomChangeAnnotator implements OwlOntologyChangeAnnotator {
    private static final Logger log = Logger.getLogger(AxiomChangeAnnotator.class);
    public static final IRI PROPERTY_IRI = IRI.create("http://protege.stanford.edu/ontology/hasChangeType");
    public static final String SEPARATOR = "|:|", ALT_SEPARATOR = "]:[";
    private final OWLDataFactory df = OWLManager.getOWLDataFactory();

    /**
     * Constructor
     */
    public AxiomChangeAnnotator() { }

    @Override
    public List<OWLOntologyChange> getAnnotatedChange(List<OWLOntologyChange> changes, RevisionTag revisionTag, OWLEntity changeSubject, ChangeType changeType, Optional<OWLEntity> p, Optional<String> newValue) {
        checkNotNull(changes);
        checkNotNull(revisionTag);
        checkNotNull(changeSubject);
        checkNotNull(changeType);
        List<OWLOntologyChange> annotatedChanges = new ArrayList<>();
        OWLAnnotationProperty property = df.getOWLAnnotationProperty(PROPERTY_IRI);
        for(OWLOntologyChange change : changes) {
            if(change.isAxiomChange()) {
                OWLAxiom axiom = change.getAxiom();
                OWLAnnotation annotation = df.getOWLAnnotation(property,
                        df.getOWLLiteral(
                                revisionTag.getTag() + SEPARATOR +
                                        changeSubject.getIRI().toString() + SEPARATOR +
                                        changeType.getDisplayName() + (changeType.getDisplayColor().isPresent() ? ALT_SEPARATOR + changeType.getDisplayColor().get().getRGB() : "") + SEPARATOR +
                                        (p.isPresent() ? p.get().getIRI() : "") + SEPARATOR +
                                        (newValue.isPresent() ? newValue.get() : ""),
                                OWL2Datatype.XSD_STRING
                        ));
                Set<OWLAnnotation> annotations = new HashSet<>();
                annotations.add(annotation);

                OWLOntologyChange annotatedChange;
                if(change.isAddAxiom()) {
                    annotatedChange = new AddAxiom(change.getOntology(), axiom.getAnnotatedAxiom(annotations));
                }
                else {
                    annotatedChange = new RemoveAxiom(change.getOntology(), axiom.getAnnotatedAxiom(annotations));
                }
                annotatedChanges.add(annotatedChange);
            }
            else {
                log.error("Non-axiom change ignored (non-axiom changes are not allowed in the axiom change annotator):\n\t" + change);
            }
        }
        return annotatedChanges;
    }

    public static String getSeparatorRegex() {
        return SEPARATOR.replaceAll("\\|", "\\\\|");
    }

    public static String getAltSeparatorRegex() {
        return ALT_SEPARATOR.replaceAll("\\[", "\\\\[");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AxiomChangeAnnotator that = (AxiomChangeAnnotator) o;
        return Objects.equal(df, that.df);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(df);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("df", df)
                .toString();
    }
}
