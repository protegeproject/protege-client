package org.protege.editor.owl.client.admin.exception;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PropertyAlreadyExistsException extends Exception {
    private static final long serialVersionUID = 1254469768061208839L;

    public PropertyAlreadyExistsException(String message) {
        super(message);
    }
}
