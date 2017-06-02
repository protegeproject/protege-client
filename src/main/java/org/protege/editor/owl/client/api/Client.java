package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.Config;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.util.List;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Client {
	
	Config getConfig();

    /**
     * Gets the authentication token owned by this client
     */
    AuthToken getAuthToken();

    /**
     * Gets the client details
     */
    UserInfo getUserInfo();

    /**
     * Gets a list of project owned by the user of this client
     */
    List<Project> getProjects() throws ClientRequestException;
    

   
    /**
     * Deleting an existing project from the server.
     *
     * @param projectId
     *            The project to remove identified by its ID.
     * @param includeFile
     *            Remove the associated files
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void deleteProject(ProjectId projectId, boolean includeFile)
            throws AuthorizationException, ClientRequestException;

    

    /**
     * Opening a project from the server. The server will return the
     * {@code ProjectResource} that can be used to construct the project
     * ontology.
     *
     * @param projectId
     *            The project to open identified by its ID
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    ServerDocument openProject(ProjectId projectId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Gets a list of role assigned to the user of this client, considering the current active project
     */
    List<Role> getActiveRoles() throws ClientRequestException;

    /**
     * Gets a list of operation assigned to the use of this client, considering the current active project
     */
    List<Operation> getActiveOperations() throws ClientRequestException;

	
	
	/**
     * Committing the given ontology changes to be applied in the server.
     *
     * @param projectId
     *            The target project for such changes
     * @param commitBundle
     *            A list of changes coming from the client
     * @return Returns the change history that contains the changes that are
     *         accepted by the server.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws SynchronizationException
     *             If the incoming changes are out-of-date.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, SynchronizationException, ClientRequestException;

    List<Project> classifiableProjects();
}
