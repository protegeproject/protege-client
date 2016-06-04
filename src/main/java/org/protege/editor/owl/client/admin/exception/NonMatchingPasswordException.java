package org.protege.editor.owl.client.admin.exception;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class NonMatchingPasswordException extends Exception {
    private static final long serialVersionUID = -6233392963762821557L;

    public NonMatchingPasswordException(String message) {
        super(message);
    }
}
