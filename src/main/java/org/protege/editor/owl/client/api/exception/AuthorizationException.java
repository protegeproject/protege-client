package org.protege.editor.owl.client.api.exception;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorizationException extends OWLClientException {

    private static final long serialVersionUID = 7099215251242729799L;

    public AuthorizationException() {
        // NO-OP
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(Throwable t) {
        super(t);
    }

    public AuthorizationException(String message, Throwable t) {
        super(message, t);
    }
}
