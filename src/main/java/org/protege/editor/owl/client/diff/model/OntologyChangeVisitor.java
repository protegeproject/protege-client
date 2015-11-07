package org.protege.editor.owl.client.diff.model;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import java.util.Optional;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OntologyChangeVisitor implements OWLAxiomVisitor {
    private static final Logger log = Logger.getLogger(OntologyChangeVisitor.class);

    private OWLObject subject, property;
    private String object;
    private ChangeType changeType;

    /**
     * No-args constructor
     */
    public OntologyChangeVisitor() { }

    public OWLObject getChangeSubject() {
        return subject;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Optional<OWLObject> getProperty() {
        return Optional.ofNullable(property);
    }

    public Optional<String> getChangeObject() {
        return Optional.ofNullable(object);
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        subject = axiom.getEntity();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        subject = axiom.getSubClass();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        subject = axiom.getSubject();
        property = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        subject = getChangeSubjectFromNaryAxiom(axiom);
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        Set<OWLSubObjectPropertyOfAxiom> subOpAxs = axiom.asSubObjectPropertyOfAxioms();
        OWLSubObjectPropertyOfAxiom subOp = subOpAxs.iterator().next();
        if (!subOp.getSubProperty().isAnonymous()) {
            subject = subOp.getSubProperty();
        } else if (!subOp.getSuperProperty().isAnonymous()) {
            subject = subOp.getSuperProperty();
        } else { // no subsumption contains atomic LHS or RHS, so just pick one
            subject = subOp.getSubProperty();
        }
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        subject = axiom.getSubject();
        property = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        subject = getChangeSubjectFromNaryAxiom(axiom);
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        subject = getChangeSubjectFromNaryPropertyAxiom(axiom);
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        subject = getChangeSubjectFromNaryPropertyAxiom(axiom);
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        subject = axiom.getSubject();
        property = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        subject = axiom.getSubProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        subject = axiom.getOWLClass();
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        Set<OWLSubDataPropertyOfAxiom> subDpAxs = axiom.asSubDataPropertyOfAxioms();
        OWLSubDataPropertyOfAxiom subDp = subDpAxs.iterator().next();
        if (!subDp.getSubProperty().isAnonymous()) {
            subject = subDp.getSubProperty();
        } else if (!subDp.getSuperProperty().isAnonymous()) {
            subject = subDp.getSuperProperty();
        } else { // no subsumption contains atomic LHS or RHS, so just pick one
            subject = subDp.getSubProperty();
        }
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        subject = axiom.getIndividual();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        Set<OWLSubClassOfAxiom> subcAxs = axiom.asOWLSubClassOfAxioms();
        OWLSubClassOfAxiom subc = subcAxs.iterator().next();
        if (!subc.getSubClass().isAnonymous()) {
            subject = subc.getSubClass();
        } else if (!subc.getSuperClass().isAnonymous()) {
            subject = subc.getSuperClass();
        } else { // no subsumption contains atomic LHS or RHS, so just pick one
            subject = subc.getSubClass();
        }
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        subject = axiom.getSubject();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        subject = axiom.getSubProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        subject = getChangeSubjectFromNaryAxiom(axiom);
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        subject = axiom.getFirstProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        subject = axiom.getClassExpression();
        log.info("Incomplete visitor data for this axiom (of type: " + axiom.getAxiomType() + "): " + axiom); // TODO
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLDatatypeDefinitionAxiom axiom) {
        subject = axiom.getDatatype();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(SWRLRule rule) {
        subject = rule;
        changeType = getChangeType(rule);
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        AnnotationSubjectVisitor visitor = new AnnotationSubjectVisitor();
        axiom.getSubject().accept(visitor);
        subject = visitor.getIri();
        property = axiom.getProperty();
        object = axiom.getValue().toString();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        subject = axiom.getSubProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    @Override
    public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        subject = axiom.getProperty();
        changeType = getChangeType(axiom);
    }

    private BuiltInChangeType getChangeType(OWLAxiom axiom) {
        if(axiom.isAnnotationAxiom()) {
            return BuiltInChangeType.ANNOTATION;
        }
        else if(axiom.isLogicalAxiom()) {
            return BuiltInChangeType.LOGICAL;
        }
        else if(axiom.isOfType(AxiomType.DECLARATION)) {
            return BuiltInChangeType.SIGNATURE;
        }
        else {
            log.warn("Axiom category could not be determined for axiom: " + axiom);
            return null;
        }
    }

    private OWLObject getChangeSubjectFromNaryAxiom(OWLSubClassOfAxiomSetShortCut axiom) {
        Set<OWLSubClassOfAxiom> subcAxs = axiom.asOWLSubClassOfAxioms();
        for(OWLSubClassOfAxiom ax : subcAxs) {
            if(!ax.getSubClass().isAnonymous()) {
                return ax.getSubClass();
            }
            else if(!ax.getSuperClass().isAnonymous()) {
                return ax.getSuperClass();
            }
        }
        return subcAxs.iterator().next().getSubClass(); // no atomic side found
    }

    private OWLObject getChangeSubjectFromNaryPropertyAxiom(OWLNaryPropertyAxiom<? extends OWLPropertyExpression> axiom) {
        Set<? extends OWLPropertyExpression> props = axiom.getProperties();
        for(OWLPropertyExpression expr : props) {
            if(!expr.isAnonymous()) {
                return expr;
            }
        }
        return props.iterator().next();
    }


    public class AnnotationSubjectVisitor implements OWLAnnotationSubjectVisitor {
        private IRI iri;

        public IRI getIri() {
            return iri;
        }

        @Override
        public void visit(IRI iri) {
            this.iri = iri;
        }

        @Override
        public void visit(OWLAnonymousIndividual individual) {
            // do nothing
        }
    }
}
