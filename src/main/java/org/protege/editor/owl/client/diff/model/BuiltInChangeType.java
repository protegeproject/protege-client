package org.protege.editor.owl.client.diff.model;

import java.awt.*;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public enum BuiltInChangeType implements ChangeType {
    LOGICAL("Logical"),
    SIGNATURE("Signature"),
    ANNOTATION("Annotation"),
    IMPORT("Import"),
    ONTOLOGY_ANNOTATION("Ontology Annotation"),
    ONTOLOGY_IRI("Ontology IRI", new Color(242, 248, 255));

    private String type;
    private Color color;

    BuiltInChangeType(String type) {
        this.type = checkNotNull(type);
    }

    BuiltInChangeType(String type, Color color) {
        this.type = checkNotNull(type);
        this.color = checkNotNull(color);
    }

    @Override
    public String toString() {
        return type;
    }

    @Override
    public String getDisplayName() {
        return type;
    }

    @Override
    public boolean isBuiltInType() {
        return true;
    }

    @Override
    public Optional<Color> getDisplayColor() {
        return Optional.ofNullable(color);
    }
}
