package org.protege.editor.owl.client.api;

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
    // All methods related to metaproject operations
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to update the server configuration.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canUpdateServerConfig();

    /**
     * Checks if the user has the permission to create a new user to the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canCreateUser();

    /**
     * Checks if the user has the permission to remove an existing user from the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canDeleteUser();

    /**
     * Checks if the user has the permission to update an existing user.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canUpdateUser();

    /**
     * Checks if the user has the permission to create a new project to the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canCreateProject();

    /**
     * Checks if the user has the permission to remove an existing project from
     * the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canDeleteProject();

    /**
     * Checks if the user has the permission to update an existing project.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canUpdateProject();

    /**
     * Checks if the user has the permission to open a project from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canOpenProject();

    /**
     * Checks if the user has the permission to create a new user's role to the
     * server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canCreateRole();

    /**
     * Checks if the user has the permission to remove an existing user's role
     * from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canDeleteRole();

    /**
     * Checks if the user has the permission to update an existing user's role.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canUpdateRole();

    /**
     * Checks if the user has the permission to create a new user's operation to
     * the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canCreateOperation();

    /**
     * Checks if the user has the permission to remove an existing user's
     * operation from the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canDeleteOperation();

    /**
     * Checks if the user has the permission to update an existing user's
     * operation.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canUpdateOperation();

    /**
     * Checks if the user has the permission to assign roles to other users.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canAssignRole();

    /**
     * Checks if the user has the permission to retract roles from other users.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canRetractRole();

    // ------------------------------------------------
    // All methods related to server operations
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to stop the server.
     *
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canStopServer();

    // ------------------------------------------------
    // A method to serve other checking operation query
    // ------------------------------------------------

    /**
     * Checks if the user has the permission to perform a project-related operation.
     *
     * @param operationId
     *            The target operation for the query
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canPerformProjectOperation(OperationId operationId);

    /**
     * Checks if the user has the permission to perform an admin-related operation.
     *
     * @param operationId
     *            The target operation for the query
     * @return Returns <code>true</code> if the user has the permission, or
     *         <code>false</code> otherwise.
     */
    boolean canPerformAdminOperation(OperationId operationId);
}
