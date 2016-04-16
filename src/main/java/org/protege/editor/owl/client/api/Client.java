package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.ClientRequestException;

import java.util.List;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.User;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Client extends ClientRequests, PolicyMediator {

    AuthToken getAuthToken();

    User getUser();

    /**
     * Gets a list of project owned by the user of this client
     */
    List<Project> getProjects() throws ClientRequestException;

    /**
     * Gets a list of role assigned to the user of this client, considering the current active project
     */
    List<Role> getActiveRoles() throws ClientRequestException;

    /**
     * Gets a list of operation assigned to the use of this client, considering the current active project
     */
    List<Operation> getActiveOperations() throws ClientRequestException;
}
