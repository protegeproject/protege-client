package org.protege.editor.owl.client.api;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ClientConfiguration;

public interface Client extends ClientRequest {

    AuthToken getAuthToken();

    String getServerAddress();

    ClientConfiguration getClientConfiguration();
}
