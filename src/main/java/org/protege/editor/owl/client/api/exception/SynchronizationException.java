package org.protege.editor.owl.client.api.exception;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class SynchronizationException extends ClientRequestException {

    private static final long serialVersionUID = 1145694593110048161L;

    public SynchronizationException() {
        super();
    }

    public SynchronizationException(String message) {
        super(message);
    }

    public SynchronizationException(Throwable t) {
        super(t);
    }

    public SynchronizationException(String message, Throwable t) {
        super(message, t);
    }
}
