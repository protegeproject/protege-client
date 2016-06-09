package org.protege.editor.owl.client;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientSessionChangeEvent {

    public enum EventCategory {
        REGISTER_USER, UNREGISTER_USER, SWITCH_ONTOLOGY
    }

    private ClientSession source;
    private EventCategory category;

    public ClientSessionChangeEvent(ClientSession source, EventCategory category) {
        this.source = source;
        this.category = category;
    }

    public ClientSession getSource() {
        return source;
    }

    public EventCategory getCategory() {
        return category;
    }

    public boolean hasCategory(EventCategory category) {
        return this.category.equals(category);
    }
}
