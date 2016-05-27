package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<User> getAllUsers() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void createUser(User newUser, Optional<SaltedPasswordDigest> password)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Updating information of an exiting user in the server.
     * 
     * @param userId
     *            The target user to modify identified by the ID
     * @param updatedUser
     *            The new updated user to replace with
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void updateUser(UserId userId, User updatedUser)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Project> getProjects(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Getting all project known by the server.
     *
     * @return A list of {@code Project}.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Project> getAllProjects() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
            Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
                    throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void deleteProject(ProjectId projectId, boolean includeFile)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void updateProject(ProjectId projectId, Project updatedProject)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    ServerDocument openProject(ProjectId projectId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    Map<ProjectId, List<Role>> getRoles(UserId userId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Role> getRoles(UserId userId, ProjectId projectId)
            throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Getting all roles known by the server.
     *
     * @return A list of {@code Role}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Role> getAllRoles() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void createRole(Role newRole) throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void updateRole(RoleId roleId, Role updatedRole)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    Map<ProjectId, List<Operation>> getOperations(UserId userId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Operation> getOperations(UserId userId, ProjectId projectId)
            throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Getting all operations known by the server.
     *
     * @return A list of {@code Operation}
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void createOperation(Operation operation) throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void deleteOperation(OperationId operationId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void updateOperation(OperationId operationId, Operation updatedOperation)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    Host getHost() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void setSecondaryPort(int portNumber) throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Gets the root directory location.
     *
     * @return The root directory location string.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    String getRootDirectory() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void setRootDirectory(String rootDirectory) throws AuthorizationException, ClientRequestException, RemoteException;

    /**
     * Gets the map of user's server properties.
     *
     * @return The server property map.
     * @throws AuthorizationException
     *             If the user doesn't have the permission to request this
     *             service.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    Map<String, String> getServerProperties() throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void setServerProperty(String property, String value)
            throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    void unsetServerProperty(String property) throws AuthorizationException, ClientRequestException, RemoteException;

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
     * @throws OutOfSyncException
     *             If the incoming changes are out-of-date.
     * @throws ClientRequestException
     *             If the server failed to fulfill the user request.
     * @throws RemoteException
     *             If the remote method invocation fails for some reason, e.g.,
     *             communication problems, failure during parameter or return
     *             value marshalling or unmarshalling, protocol errors.
     */
    ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, OutOfSyncException, ClientRequestException, RemoteException;
}
