package org.protege.editor.owl.client.api;

import edu.stanford.protege.metaproject.api.*;

import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents all operations that the client could request to the server.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public interface ClientRequests {

    /**
     * Getting all users known by the server.
     *
     * @return A list of {@code User}.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<User> getAllUsers() throws AuthorizationException, ClientRequestException;

    /**
     * Creating a new user to the server.
     *
     * @param newUser
     *            The new user to add.
     * @param password
     *            The password associated to the new user.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void createUser(User newUser, Optional<? extends Password> password)
            throws AuthorizationException, ClientRequestException;

    /**
     * Deleting an existing user from the server.
     *
     * @param userId
     *            The user to remove identified by the ID
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException;

    /**
     * Updating information of an exiting user in the server.
     * 
     * @param userId
     *            The target user to modify identified by the ID
     * @param updatedUser
     *            The new updated user to replace with
     * @param updatedPassword
     *            An optional new updated password
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all projects the given the user id.
     *
     * @param userId
     *            The target user identified by the ID
     * @return A list of {@code Project}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Project> getProjects(UserId userId) throws AuthorizationException, ClientRequestException;

    /**
     * Getting all project known by the server.
     *
     * @return A list of {@code Project}.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Project> getAllProjects() throws AuthorizationException, ClientRequestException;

    /**
     * Creating a new project to the server.
     * 
     * @param projectId
     *            The project identifier object
     * @param projectName
     *            The name of the project
     * @param description
     *            The description of the project
     * @param owner
     *            The owner of the project
     * @param options
     *            An optional of project options
     * @param initialCommit
     *            An optional initial commit bundle when creating this project
     * @return A server document that provide the link information to remote resources
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
            Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
                    throws AuthorizationException, ClientRequestException;

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
     * Updating information of an existing project in the server.
     *
     * @param projectId
     *            The target project to modify identified by its ID.
     * @param updatedProject
     *            The new updated project to replace with.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void updateProject(ProjectId projectId, Project updatedProject)
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
     * Getting all roles given the user id, categorized for each owned project.
     *
     * @param userId
     *            The target user identified by the ID
     * @return A map of {@code ProjectId} with a list of corresponding
     *         {@code Role}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    Map<ProjectId, List<Role>> getRoles(UserId userId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all roles given the user id and the project id.
     *
     * @param userId
     *            The target user identified by the ID
     * @param projectId
     *            The target project identified by the ID
     * @return A list of {@code Role}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Role> getRoles(UserId userId, ProjectId projectId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all roles known by the server.
     *
     * @return A list of {@code Role}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Role> getAllRoles() throws AuthorizationException, ClientRequestException;

    /**
     * Creating a new role to the server.
     *
     * @param newRole
     *            The new role to add.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void createRole(Role newRole) throws AuthorizationException, ClientRequestException;

    /**
     * Deleting an existing role from the server.
     *
     * @param roleId
     *            The role to remove identified by its ID.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException;

    /**
     * Updating information of an existing role at the server.
     *
     * @param roleId
     *            The target role to modify identified by its ID.
     * @param updatedRole
     *            The new updated role to replace with.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void updateRole(RoleId roleId, Role updatedRole)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all operations given the user id, categorized for each owned
     * project.
     *
     * @param userId
     *            The target user identified by the ID
     * @return A map of {@code ProjectId} with a list of corresponding
     *         {@code Operation}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    Map<ProjectId, List<Operation>> getOperations(UserId userId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all operations given the user id and the project id.
     *
     * @param userId
     *            The target user identified by the ID
     * @param projectId
     *            The target project identified by the ID
     * @return A list of {code Operation}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Operation> getOperations(UserId userId, ProjectId projectId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all operations given the role id
     *
     * @param roleId
     *            The target role identified by the ID
     * @return A list of {@code Operation}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Operation> getOperations(RoleId roleId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Getting all operations known by the server.
     *
     * @return A list of {@code Operation}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException;

    /**
     * Creating a new operation to the server.
     *
     * @param operation
     *            The new operation to add.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void createOperation(Operation operation) throws AuthorizationException, ClientRequestException;

    /**
     * Deleting an existing operation from the server.
     *
     * @param operationId
     *            The operation to remove identified by its ID.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void deleteOperation(OperationId operationId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Updating an existing operation at the server.
     *
     * @param operationId
     *            The target operation to modify identified by its ID.
     * @param updatedOperation
     *            The new updated operation to replace with.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void updateOperation(OperationId operationId, Operation updatedOperation)
            throws AuthorizationException, ClientRequestException;

    /**
     * Assigning a role to a user for a particular project.
     *
     * @param userId
     *            The target user
     * @param projectId
     *            The target project
     * @param roleId
     *            The role to assign
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Retracting a role from a user for a particular project.
     *
     * @param userId
     *            The target user
     * @param projectId
     *            The target project
     * @param roleId
     *            The role to retract
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ClientRequestException;

    /**
     * Gets the host information (including the host address and secondary port,
     * if any)
     *
     * @return The {@code Host} object to represent such information
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    Host getHost() throws AuthorizationException, ClientRequestException;

    /**
     * Sets the host server address.
     *
     * @param hostAddress
     *            The host address URI.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException;

    /**
     * Sets the secondary port number.
     *
     * @param portNumber
     *            The port number.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void setSecondaryPort(int portNumber) throws AuthorizationException, ClientRequestException;

    /**
     * Gets the root directory location.
     *
     * @return The root directory location string.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    String getRootDirectory() throws AuthorizationException, ClientRequestException;

    /**
     * Sets the root directory location.
     *
     * @param rootDirectory
     *            The root directory location using the absolute path.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void setRootDirectory(String rootDirectory) throws AuthorizationException, ClientRequestException;

    /**
     * Gets the map of user's server properties.
     *
     * @return The server property map.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    Map<String, String> getServerProperties() throws AuthorizationException, ClientRequestException;

    /**
     * Setting a server property by specifying the property name and the value.
     *
     * @param property
     *            The target property name
     * @param value
     *            The property value
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void setServerProperty(String property, String value) 
            throws AuthorizationException, ClientRequestException;

    /**
     * Unsets a server property by specifying the property name.
     *
     * @param property
     *            The target property name
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     */
    void unsetServerProperty(String property) throws AuthorizationException, ClientRequestException;

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
}
