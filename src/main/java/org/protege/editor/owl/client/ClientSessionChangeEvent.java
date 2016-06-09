package org.protege.editor.owl.client;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientSessionChangeEvent {

    private ClientSession source;

    public ClientSessionChangeEvent(ClientSession source) {
        this.source = source;
    }

    public ClientSession getSource() {
        return source;
    }
}
