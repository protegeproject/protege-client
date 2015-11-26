package org.protege.editor.owl.client.diff.model;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public enum ChangeMode {
    ADDITION("+"),
    REMOVAL("-"),
    ALIGNED("±"),
    ONTOLOGY_IRI("@"),
    CUSTOM("*");

    private String mode;

    ChangeMode(String mode) {
        this.mode = checkNotNull(mode);
    }

    @Override
    public String toString() {
        return mode;
    }
}
