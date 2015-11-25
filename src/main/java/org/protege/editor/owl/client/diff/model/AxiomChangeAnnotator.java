package org.protege.editor.owl.client.diff.model;

import com.google.common.base.Objects;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class AxiomChangeAnnotator implements OwlOntologyChangeAnnotator {
    private static final Logger log = Logger.getLogger(AxiomChangeAnnotator.class);
    public static final IRI PROPERTY_IRI = IRI.create("http://protege.stanford.edu/ontology/hasChangeType");
    private final String separator = "|:|";
    private final OWLAnnotationProperty property;
    private final OWLDataFactory df = OWLManager.getOWLDataFactory();

    /**
     * Constructor
     */
    public AxiomChangeAnnotator() {
        this.property = df.getOWLAnnotationProperty(PROPERTY_IRI);
    }

    @Override
    public List<OWLOntologyChange> getAnnotatedChange(List<OWLOntologyChange> changes, RevisionTag revisionTag, OWLEntity changeSubject, ChangeType changeType, Optional<OWLEntity> p, Optional<String> newValue) {
        checkNotNull(changes);
        checkNotNull(changeSubject);
        checkNotNull(changeType);
        List<OWLOntologyChange> annotatedChanges = new ArrayList<>();
        for(OWLOntologyChange change : changes) {
            if(change.isAxiomChange()) {
                OWLAxiom axiom = change.getAxiom();
                OWLAnnotation annotation = df.getOWLAnnotation(property,
                        df.getOWLLiteral(
                                revisionTag.getTag() + separator +
                                changeSubject.getIRI().toString() + separator +
                                changeType.getDisplayName() + separator +
                                        (p.isPresent() ? p.get().getIRI() : "") + separator +
                                        (newValue.isPresent() ? newValue.get() : "")
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

    public String getSeparator() {
        return separator;
    }

    public OWLAnnotationProperty getAnnotationProperty() {
        return property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AxiomChangeAnnotator that = (AxiomChangeAnnotator) o;
        return Objects.equal(separator, that.separator) &&
                Objects.equal(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(separator, property, df);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("separator", separator)
                .add("property", property)
                .toString();
    }
}
