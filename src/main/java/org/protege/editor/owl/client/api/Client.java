package org.protege.editor.owl.client.api;

import edu.stanford.protege.metaproject.api.AuthToken;

public interface Client extends ClientRequests, PolicyMediator, RegistryMediator {

    AuthToken getAuthToken();
}
