package org.protege.editor.owl.client.admin.exception;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PolicyEntryAlreadyExistsException extends Exception {
    private static final long serialVersionUID = 8740624376943148823L;

    public PolicyEntryAlreadyExistsException(String message) {
        super(message);
    }
}
