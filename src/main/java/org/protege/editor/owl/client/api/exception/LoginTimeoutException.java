package org.protege.editor.owl.client.api.exception;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LoginTimeoutException extends ClientRequestException {

    private static final long serialVersionUID = 8669216395728221592L;

    public LoginTimeoutException() {
        super();
    }

    public LoginTimeoutException(String message) {
        super(message);
    }

    public LoginTimeoutException(Throwable t) {
        super(t);
    }

    public LoginTimeoutException(String message, Throwable t) {
        super(message, t);
    }
}
