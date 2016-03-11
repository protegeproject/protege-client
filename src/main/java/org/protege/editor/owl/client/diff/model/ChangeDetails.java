package org.protege.editor.owl.client.diff.model;

import org.semanticweb.owlapi.model.OWLObject;

import java.util.Optional;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface ChangeDetails {

    /**
     * Get the revision tag for this change
     *
     * @return Revision tag
     */
    RevisionTag getRevisionTag();

    /**
     * Get the change subject, that is (for most axioms), the left-hand side of the axiom. For example,
     * for A SubClassOf B the change subject is A. This may be an OWL entity or class expression
     *
     * @return Change subject as an OWLObject
     */
    OWLObject getSubject();

    /**
     * Get the change type, which could be one of the built-in types such as logical, signature or
     * annotation, or a custom type
     *
     * @return Change type
     */
    ChangeType getType();

    /**
     * Get the (annotation) property involved in the change, if any
     *
     * @return Annotation property changed
     */
    Optional<OWLObject> getProperty();

    /**
     * Get the new annotation property value
     *
     * @return Annotation property assertion literal value
     */
    Optional<String> getNewValue();

}
