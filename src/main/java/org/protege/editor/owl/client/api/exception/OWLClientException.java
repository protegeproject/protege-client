package org.protege.editor.owl.client.api.exception;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OWLClientException extends Exception {

    private static final long serialVersionUID = -7231858889044093310L;

    public OWLClientException() {
        super();
    }

    public OWLClientException(String message) {
        super(message);
    }

    public OWLClientException(Throwable t) {
        super(t);
    }

    public OWLClientException(String message, Throwable t) {
        super(message, t);
    }
}
