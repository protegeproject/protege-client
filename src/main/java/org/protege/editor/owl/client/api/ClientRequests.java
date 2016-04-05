package org.protege.editor.owl.client.api;

import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

public interface ClientRequests {

    /**
     * Adding a new user to the server.
     *
     * @param newUser
     *            The new user to add.
     * @throws ServerRequestException
     */
    void addUser(User newUser) throws ServerRequestException;

    /**
     * Removing an existing user from the server.
     *
     * @param userId
     *            The user to remove identified by the ID
     * @throws ServerRequestException
     */
    void removeUser(UserId userId) throws ServerRequestException;

    /**
     * Modifying an exiting user in the server.
     * 
     * @param userId
     *          The target user to modify identified by the ID
     * @param newUser
     *          The new user to replace with
     * @throws ServerRequestException
     */
    void modifyUser(UserId userId, User user) throws ServerRequestException;

    /**
     * Adding a new project to the server.
     *
     * @param newProject
     *            The new project to add.
     * @throws ServerRequestException
     */
    void addProject(Project newProject) throws ServerRequestException;

    /**
     * Removing an existing project from the server.
     *
     * @param projectId
     *            The project to remove identified by its ID.
     * @throws ServerRequestException
     */
    void removeProject(ProjectId projectId) throws ServerRequestException;

    /**
     * Modifying an existing project in the server.
     *
     * @param projectId
     *            The target project to modify identified by its ID.
     * @param newProject
     *            The new project to replace with.
     * @throws ServerRequestException
     */
    void modifyProject(ProjectId projectId, Project newProject) throws ServerRequestException;

    /**
     * Viewing only the project (similar to read-only permission) from the
     * server.
     *
     * @param projectId
     *            The project to view identified by its ID
     * @throws ServerRequestException
     */
    void viewProject(ProjectId projectId) throws ServerRequestException;

    /**
     * Adding a new role to the server.
     *
     * @param newRole
     *          The new role to add.
     * @throws ServerRequestException
     */
    void addRole(Role newRole) throws ServerRequestException;

    /**
     * Removing an existing role from the server.
     *
     * @param roleId
     *          The role to remove identified by its ID.
     * @throws ServerRequestException
     */
    void removeRole(RoleId roleId) throws ServerRequestException;

    /**
     * Modifying an existing role at the server.
     *
     * @param roleId
     *          The target role to modify identified by its ID.
     * @param newRole
     *          The new role to replace with.
     * @throws ServerRequestException
     */
    void modifyRole(RoleId roleId, Role newRole) throws ServerRequestException;

    /**
     * Adding a new operation to the server.
     *
     * @param operation
     *          The new operation to add.
     * @throws ServerRequestException
     */
    void addOperation(Operation operation) throws ServerRequestException;

    /**
     * Removing an existing operation from the server.
     *
     * @param operationId
     *          The operation to remove identified by its ID.
     * @throws ServerRequestException
     */
    void removeOperation(OperationId operationId) throws ServerRequestException;

    /**
     * Modifying an existing operation at the server.
     *
     * @param operationId
     *          The target operation to modify identified by its ID.
     * @param newOperation
     *          The new operation to replace with.
     * @throws ServerRequestException
     */
    void modifyOperation(OperationId operationId, Operation newOperation) throws ServerRequestException;

    /**
     * Assigning a role to a user for a particular project.
     *
     * @param userId
     *          The target user
     * @param projectId
     *          The target project
     * @param roleId
     *          The role to assign
     * @throws ServerRequestException
     */
    void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws ServerRequestException;

    /**
     * Retracting a role from a user for a particular project.
     *
     * @param userId
     *          The target user
     * @param projectId 
     *          The target project
     * @param roleId
     *          The role to retract
     * @throws ServerRequestException
     */
    void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws ServerRequestException;

    /**
     * Modifying an existing server property by replacing the value.
     *
     * @param property
     *          The target property name
     * @param value
     *          The new property value
     * @throws ServerRequestException
     */
    void modifyServerConfiguration(String property, String value) throws ServerRequestException;

    /**
     * Committing the given ontology changes to be applied in the server.
     *
     * @param project
     *            The target project for such changes
     * @param changes
     *            A list of changes coming from the client
     * @throws ServerRequestException
     */
    void commit(Project project, CommitBundle changes) throws ServerRequestException;
}
