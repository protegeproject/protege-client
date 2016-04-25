package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.ClientRequestException;

import edu.stanford.protege.metaproject.api.OperationId;

/**
 * Represents the utility methods that will check the user's permission to
 * perform a particular operation.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public interface PolicyMediator {

    // ------------------------------------------------
    // All methods related to ontology operations
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to create a new axiom to the
     * ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddAxiom() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing axiom from an
     * ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveAxiom() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new annotation to the
     * ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddAnnotatation() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing annotation
     * from an ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveAnnotation() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new import declaration
     * to an ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddImport() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existin import
     * declaration from an ontology.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveImport() throws ClientRequestException;

    /**
     * Checks if the user has the permission to modify the ontology ID.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyOntologyId() throws ClientRequestException;

    // ------------------------------------------------
    // All methods related to metaproject operations
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to update the server configuration.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyServerConfig() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new user to the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddUser() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing user from the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveUser() throws ClientRequestException;

    /**
     * Checks if the user has the permission to update an existing user.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyUser() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new project to the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddProject() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing project from
     * the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveProject() throws ClientRequestException;

    /**
     * Checks if the user has the permission to update an existing project.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyProject() throws ClientRequestException;

    /**
     * Checks if the user has the permission to open a project from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canOpenProject() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new user's role to the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddRole() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing user's role
     * from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveRole() throws ClientRequestException;

    /**
     * Checks if the user has the permission to update an existing user's role.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyRole() throws ClientRequestException;

    /**
     * Checks if the user has the permission to create a new user's operation to
     * the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAddOperation() throws ClientRequestException;

    /**
     * Checks if the user has the permission to remove an existing user's
     * operation from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRemoveOperation() throws ClientRequestException;

    /**
     * Checks if the user has the permission to update an existing user's
     * operation.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canModifyOperation() throws ClientRequestException;

    /**
     * Checks if the user has the permission to assign roles to other users.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canAssignRole() throws ClientRequestException;

    /**
     * Checks if the user has the permission to retract roles from other users.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRetractRole() throws ClientRequestException;

    // ------------------------------------------------
    // All methods related to server operations
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to stop the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canStopServer() throws ClientRequestException;

    /**
     * Checks if the user has the permission to restart the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canRestartServer() throws ClientRequestException;

    // ------------------------------------------------
    // A method to serve other checking operation query
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to perform the given operation.
     *
     * @param operationId
     *            The target operation for the query
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     * @throws ClientRequestException
     *             If a failure happened when answering this query request.
     */
    boolean canPerformOperation(OperationId operationId) throws ClientRequestException;
}
