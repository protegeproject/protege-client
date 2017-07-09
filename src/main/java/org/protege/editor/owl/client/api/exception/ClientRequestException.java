package org.protege.editor.owl.client.api.exception;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientRequestException extends OWLClientException {

    private static final long serialVersionUID = -366523715475334812L;

    public ClientRequestException() {
        // NO-OP
    }

    public ClientRequestException(String message) {
        super(message);
    }

    public ClientRequestException(Throwable t) {
        super(t);
    }

    public ClientRequestException(String message, Throwable t) {
        super(message, t);
    }
}
