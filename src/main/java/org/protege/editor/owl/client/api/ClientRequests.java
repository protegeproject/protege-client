package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.versioning.ServerDocument;

import java.util.List;
import java.util.Map;

import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
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
     * @throws ClientRequestException
     */
    List<User> getAllUsers() throws ClientRequestException;

    /**
     * Creating a new user to the server.
     *
     * @param newUser
     *            The new user to add.
     * @throws ClientRequestException
     */
    void createUser(User newUser) throws ClientRequestException;

    /**
     * Deleting an existing user from the server.
     *
     * @param userId
     *            The user to remove identified by the ID
     * @throws ClientRequestException
     */
    void deleteUser(UserId userId) throws ClientRequestException;

    /**
     * Updating information of an exiting user in the server.
     * 
     * @param userId
     *          The target user to modify identified by the ID
     * @param updatedUser
     *          The new updated user to replace with
     * @throws ClientRequestException
     */
    void updateUser(UserId userId, User updatedUser) throws ClientRequestException;

    /**
     * Getting all projects the given the user id.
     *
     * @param userId
     *          The target user identified by the ID
     * @return A list of {@code Project}
     * @throws ClientRequestException
     */
    List<Project> getProjects(UserId userId) throws ClientRequestException;

    /**
     * Getting all project known by the server.
     *
     * @return A list of {@code Project}.
     * @throws ClientRequestException
     */
    List<Project> getAllProjects() throws ClientRequestException;

    /**
     * Creating a new project to the server.
     *
     * @param newProject
     *            The new project to add.
     * @throws ClientRequestException
     */
    void createProject(Project newProject) throws ClientRequestException;

    /**
     * Deleting an existing project from the server.
     *
     * @param projectId
     *            The project to remove identified by its ID.
     * @throws ClientRequestException
     */
    void deleteProject(ProjectId projectId) throws ClientRequestException;

    /**
     * Updating information of an existing project in the server.
     *
     * @param projectId
     *            The target project to modify identified by its ID.
     * @param updatedProject
     *            The new updated project to replace with.
     * @throws ClientRequestException
     */
    void updateProject(ProjectId projectId, Project updatedProject) throws ClientRequestException;

    /**
     * Opening a project from the server. The server will return the {@code ProjectResource} that
     * can be used to construct the project ontology.
     *
     * @param projectId
     *            The project to open identified by its ID
     * @throws ClientRequestException
     */
    ServerDocument openProject(ProjectId projectId) throws ClientRequestException;

    /**
     * Getting all roles given the user id, categorized for each owned project.
     *
     * @param userId
     *          The target user identified by the ID
     * @return A map of {@code ProjectId} with a list of corresponding {@code Role}
     * @throws ClientRequestException
     */
    Map<ProjectId, List<Role>> getRoles(UserId userId) throws ClientRequestException;

    /**
     * Getting all roles given the user id and the project id.
     *
     * @param userId
     *          The target user identified by the ID
     * @param projectId
     *          The target project identified by the ID
     * @return A list of {@code Role}
     * @throws ClientRequestException
     */
    List<Role> getRoles(UserId userId, ProjectId projectId) throws ClientRequestException;

    /**
     * Getting all roles known by the server.
     *
     * @return A list of {@code Role}
     * @throws ClientRequestException
     */
    List<Role> getAllRoles() throws ClientRequestException;

    /**
     * Creating a new role to the server.
     *
     * @param newRole
     *          The new role to add.
     * @throws ClientRequestException
     */
    void createRole(Role newRole) throws ClientRequestException;

    /**
     * Deleting an existing role from the server.
     *
     * @param roleId
     *          The role to remove identified by its ID.
     * @throws ClientRequestException
     */
    void deleteRole(RoleId roleId) throws ClientRequestException;

    /**
     * Updating information of an existing role at the server.
     *
     * @param roleId
     *          The target role to modify identified by its ID.
     * @param updatedRole
     *          The new updated role to replace with.
     * @throws ClientRequestException
     */
    void updateRole(RoleId roleId, Role updatedRole) throws ClientRequestException;

    /**
     * Getting all operations given the user id, categorized for each owned project.
     *
     * @param userId
     *          The target user identified by the ID
     * @return A map of {@code ProjectId} with a list of corresponding {@code Operation}
     * @throws ClientRequestException
     */
    Map<ProjectId, List<Operation>> getOperations(UserId userId) throws ClientRequestException;

    /**
     * Getting all operations given the user id and the project id.
     *
     * @param userId
     *          The target user identified by the ID
     * @param projectId
     *          The target project identified by the ID
     * @return A list of {code Operation}
     * @throws ClientRequestException
     */
    List<Operation> getOperations(UserId userId, ProjectId projectId) throws ClientRequestException;

    /**
     * Getting all operations known by the server.
     *
     * @return A list of {@code Operation}
     * @throws ClientRequestException
     */
    List<Operation> getAllOperations() throws ClientRequestException;

    /**
     * Creating a new operation to the server.
     *
     * @param operation
     *          The new operation to add.
     * @throws ClientRequestException
     */
    void createOperation(Operation operation) throws ClientRequestException;

    /**
     * Deleting an existing operation from the server.
     *
     * @param operationId
     *          The operation to remove identified by its ID.
     * @throws ClientRequestException
     */
    void deleteOperation(OperationId operationId) throws ClientRequestException;

    /**
     * Updating an existing operation at the server.
     *
     * @param operationId
     *          The target operation to modify identified by its ID.
     * @param updatedOperation
     *          The new updated operation to replace with.
     * @throws ClientRequestException
     */
    void updateOperation(OperationId operationId, Operation updatedOperation) throws ClientRequestException;

    /**
     * Assigning a role to a user for a particular project.
     *
     * @param userId
     *          The target user
     * @param projectId
     *          The target project
     * @param roleId
     *          The role to assign
     * @throws ClientRequestException
     */
    void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws ClientRequestException;

    /**
     * Retracting a role from a user for a particular project.
     *
     * @param userId
     *          The target user
     * @param projectId 
     *          The target project
     * @param roleId
     *          The role to retract
     * @throws ClientRequestException
     */
    void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws ClientRequestException;


    /**
     * Gets the host information (including the host address and secondary port, if any)
     *
     * @return The {@code Host} object to represent such information
     * @throws Exception
     */
    Host getHost() throws Exception;

    /**
     * Sets the host server address.
     *
     * @param hostAddress
     *          The host address string.
     * @throws Exception
     */
    void setHostAddress(String hostAddress) throws Exception;

    /**
     * Sets the secondary port number.
     *
     * @param portNumber
     *          The port number.
     * @throws Exception
     */
    void setSecondaryPort(int portNumber) throws Exception;

    /**
     * Gets the root directory location.
     *
     * @return The root directory location string.
     * @throws Exception
     */
    String getRootDirectory() throws Exception;

    /**
     * Sets the root directory location.
     *
     * @param rootDirectory
     *          The root directory location using the absolute path.
     * @throws Exception
     */
    void setRootDirectory(String rootDirectory) throws Exception;

    /**
     * Gets the map of user's server properties.
     *
     * @return The server property map.
     * @throws Exception
     */
    Map<String, String> getServerProperties() throws Exception;

    /**
     * Setting a server property by specifying the property name and the value.
     *
     * @param property
     *          The target property name
     * @param value
     *          The property value
     * @throws ClientRequestException
     */
    void setServerProperty(String property, String value) throws ClientRequestException;

    /**
     * Unsets a server property by specifying the property name.
     *
     * @param property
     *          The target property name
     * @throws Exception
     */
    void unsetServerProperty(String property) throws Exception;

    /**
     * Committing the given ontology changes to be applied in the server.
     *
     * @param project
     *            The target project for such changes
     * @param changes
     *            A list of changes coming from the client
     * @throws ClientRequestException
     */
    void commit(Project project, CommitBundle changes) throws ClientRequestException;
}
