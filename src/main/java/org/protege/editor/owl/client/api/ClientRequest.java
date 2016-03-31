package org.protege.editor.owl.client.api;

import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

public interface ClientRequest {

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
     *            The user to remove identified by the ID
     * @throws ServerRequestException
     */
    void removeUser(UserId userId) throws ServerRequestException;

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
     * Viewing only the project (similar to read-only permission) from the
     * server.
     *
     * @param projectId
     *            The project to view identified by its ID
     * @throws ServerRequestException
     */
    void viewProject(ProjectId projectId) throws ServerRequestException;

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
