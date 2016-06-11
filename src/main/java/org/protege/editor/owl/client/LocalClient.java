package org.protege.editor.owl.client;

import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.IdAlreadyInUseException;
import edu.stanford.protege.metaproject.impl.Operations;

import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.transport.rmi.RemoteServer;
import org.protege.editor.owl.server.transport.rmi.RmiServer;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LocalClient implements Client, ClientSessionListener {

    private AuthToken authToken;
    private String serverAddress;
    private int registryPort;

    private ProjectId projectId;
    private UserId userId;

    private UserInfo userInfo; // cache
    private RemoteServer server;

    public LocalClient(AuthToken authToken, String serverAddress, int registryPort) {
        this.authToken = authToken;
        this.serverAddress = serverAddress;
        this.registryPort = registryPort;
        userId = authToken.getUser().getId();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            projectId = event.getSource().getActiveProject();
        }
    }

    /**
     * @deprecated Better to use the ClientSessionListener approach to set the
     * current active project.
     */
    @Override
    @Deprecated
    public void setActiveProject(ProjectId projectId) {
        this.projectId = projectId;
    }

    private Optional<ProjectId> getRemoteProject() {
        return Optional.ofNullable(projectId);
    }

    @Override
    public AuthToken getAuthToken() {
        return authToken;
    }

    @Override
    public UserInfo getUserInfo() {
        if (userInfo == null) {
            User user = authToken.getUser();
            userInfo = new UserInfo(user.getId().get(), user.getName().get(), user.getEmailAddress().get());
        }
        return userInfo;
    }

    @Override
    public List<Project> getProjects() throws ClientRequestException {
        try {
            return getProjects(userId);
        }
        catch (AuthorizationException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Role> getActiveRoles() throws ClientRequestException {
        try {
            List<Role> activeRoles = new ArrayList<>();
            if (getRemoteProject().isPresent()) {
                activeRoles = getRoles(userId, getRemoteProject().get());
            }
            return activeRoles;
        }
        catch (AuthorizationException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Operation> getActiveOperations() throws ClientRequestException {
        try {
            List<Operation> activeOperations = new ArrayList<>();
            if (getRemoteProject().isPresent()) {
                activeOperations = getOperations(userId, getRemoteProject().get());
            }
            return activeOperations;
        }
        catch (AuthorizationException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    protected void connect() throws RemoteException {
        if (server == null) {
            server = (RemoteServer) ServerUtils.getRemoteService(serverAddress, registryPort, RmiServer.SERVER_SERVICE);
        }
    }

    public void disconnect() {
        server = null;
    }

    @Override
    public void createUser(User newUser, Optional<? extends Password> password)
            throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            Password passwordValue = (password.isPresent()) ? password.get() : null;
            server.createUser(authToken, newUser, passwordValue);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteUser(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
            throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            Password passwordValue = (updatedPassword.isPresent()) ? updatedPassword.get() : null;
            server.updateUser(authToken, userId, updatedUser, passwordValue);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public ServerDocument createProject(ProjectId projectId, Name projectName, Description description,
            UserId owner, Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
                    throws AuthorizationException, ClientRequestException, RemoteException {
        ServerDocument serverDocument = null;
        try {
            connect();
            ProjectOptions projectOptions = (options.isPresent()) ? options.get() : null;
            serverDocument = server.createProject(authToken, projectId, projectName, description, owner, projectOptions);
        }
        catch (ServerServiceException e) {
            Throwable t = e.getCause();
            if (t instanceof IdAlreadyInUseException) {
                throw new ClientRequestException("Project ID is already used. Please use different name.", t);
            }
            else {
                try {
                    server.deleteProject(authToken, projectId, true);
                }
                catch (ServerServiceException ee) {
                    throw new ClientRequestException("Failed to create a new project. Please try again.", ee.getCause());
                }
                throw new ClientRequestException("Failed to create a new project. Please try again.", e.getCause());
            }
        }
        
        // Do initial commit if the commit bundle is not empty
        if (initialCommit.isPresent()) {
            try {
                server.commit(authToken, projectId, initialCommit.get());
            }
            catch (ServerServiceException | OutOfSyncException e) {
                try {
                    server.deleteProject(authToken, projectId, true);
                }
                catch (ServerServiceException ee) {
                    throw new ClientRequestException("Failed to create a new project. Please try again.", ee.getCause());
                }
                throw new ClientRequestException("Failed to create a new project. Please try again.", e.getCause());
            }
        }
        return serverDocument;
    }

    @Override
    public void deleteProject(ProjectId projectId, boolean includeFile) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteProject(authToken, projectId, includeFile);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateProject(ProjectId projectId, Project updatedProject) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateProject(authToken, projectId, updatedProject);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public ServerDocument openProject(ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            ServerDocument serverDocument = server.openProject(authToken, projectId);
            return serverDocument;
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void createRole(Role newRole) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.createRole(authToken, newRole);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteRole(authToken, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateRole(RoleId roleId, Role updatedRole) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateRole(authToken, roleId, updatedRole);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void createOperation(Operation operation) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.createOperation(authToken, operation);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteOperation(OperationId operationId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteOperation(authToken, operationId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateOperation(OperationId operationId, Operation updatedOperation) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateOperation(authToken, operationId, updatedOperation);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.assignRole(authToken, userId, projectId, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.retractRole(authToken, userId, projectId, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Host getHost() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getHost(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setHostAddress(authToken, hostAddress);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public void setSecondaryPort(int portNumber) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setSecondaryPort(authToken, portNumber);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public String getRootDirectory() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRootDirectory(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public void setRootDirectory(String rootDirectory) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setRootDirectory(authToken, rootDirectory);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public Map<String, String> getServerProperties() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getServerProperties(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public void setServerProperty(String property, String value) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setServerProperty(authToken, property, value);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public void unsetServerProperty(String property) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.unsetServerProperty(authToken, property);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, OutOfSyncException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.commit(authToken, projectId, commitBundle);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<User> getAllUsers() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllUsers(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public List<Project> getProjects(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getProjects(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Project> getAllProjects() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllProjects(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public Map<ProjectId, List<Role>> getRoles(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRoles(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Role> getRoles(UserId userId, ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRoles(authToken, userId, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Role> getAllRoles() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllRoles(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    @Override
    public Map<ProjectId, List<Operation>> getOperations(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getOperations(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Operation> getOperations(UserId userId, ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getOperations(authToken, userId, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Operation> getOperations(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getOperations(authToken, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllOperations(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage());
        }
    }

    /*
     * Utility methods for querying the client permissions. All these methods will initially check if the client
     * is linked to a remote project before sending the query. All the methods will return false as the default
     * answer.
     */

    @Override
    public boolean canAddAxiom() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.ADD_AXIOM.getId());
    }

    @Override
    public boolean canRemoveAxiom() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.REMOVE_AXIOM.getId());
    }

    @Override
    public boolean canAddAnnotation() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.ADD_ONTOLOGY_ANNOTATION.getId());
    }

    @Override
    public boolean canRemoveAnnotation() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.REMOVE_ONTOLOGY_ANNOTATION.getId());
    }

    @Override
    public boolean canAddImport() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.ADD_IMPORT.getId());
    }

    @Override
    public boolean canRemoveImport() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.REMOVE_IMPORT.getId());
    }

    @Override
    public boolean canModifyOntologyId() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.MODIFY_ONTOLOGY_IRI.getId());
    }

    @Override
    public boolean canCreateUser() {
        return queryAdminPolicy(userId, Operations.ADD_USER.getId());
    }

    @Override
    public boolean canDeleteUser() {
        return queryAdminPolicy(userId, Operations.REMOVE_USER.getId());
    }

    @Override
    public boolean canUpdateUser() {
        return queryAdminPolicy(userId, Operations.MODIFY_USER.getId());
    }

    @Override
    public boolean canCreateProject() {
        return queryAdminPolicy(userId, Operations.ADD_PROJECT.getId());
    }

    @Override
    public boolean canDeleteProject() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.REMOVE_PROJECT.getId());
    }

    @Override
    public boolean canUpdateProject() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.MODIFY_PROJECT.getId());
    }

    @Override
    public boolean canOpenProject() {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), Operations.OPEN_PROJECT.getId());
    }

    @Override
    public boolean canCreateRole() {
        return queryAdminPolicy(userId, Operations.ADD_ROLE.getId());
    }

    @Override
    public boolean canDeleteRole() {
        return queryAdminPolicy(userId, Operations.REMOVE_ROLE.getId());
    }

    @Override
    public boolean canUpdateRole() {
        return queryAdminPolicy(userId, Operations.MODIFY_ROLE.getId());
    }

    @Override
    public boolean canCreateOperation() {
        return queryAdminPolicy(userId, Operations.ADD_OPERATION.getId());
    }

    @Override
    public boolean canDeleteOperation() {
        return queryAdminPolicy(userId, Operations.REMOVE_OPERATION.getId());
    }

    @Override
    public boolean canUpdateOperation() {
        return queryAdminPolicy(userId, Operations.MODIFY_OPERATION.getId());
    }

    @Override
    public boolean canAssignRole() {
        return queryAdminPolicy(userId, Operations.ASSIGN_ROLE.getId());
    }

    @Override
    public boolean canRetractRole() {
        return queryAdminPolicy(userId, Operations.RETRACT_ROLE.getId());
    }

    @Override
    public boolean canStopServer() {
        return queryAdminPolicy(userId, Operations.STOP_SERVER.getId());
    }

    @Override
    public boolean canUpdateServerConfig() {
        return queryAdminPolicy(userId, Operations.MODIFY_SERVER_SETTINGS.getId());
    }

    @Override
    public boolean canPerformProjectOperation(OperationId operationId) {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryProjectPolicy(userId, getRemoteProject().get(), operationId);
    }

    @Override
    public boolean canPerformAdminOperation(OperationId operationId) {
        if (!getRemoteProject().isPresent()) {
            return false;
        }
        return queryAdminPolicy(userId, operationId);
    }

    /*
     * Utility methods
     */

    public boolean queryProjectPolicy(UserId userId, ProjectId projectId, OperationId operationId) {
        boolean isAllowed = false;
        try {
            connect();
            isAllowed = server.isOperationAllowed(authToken, operationId, projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            // TODO Add logging
        }
        return isAllowed;
    }

    private boolean queryAdminPolicy(UserId userId, OperationId operationId) {
        boolean isAllowed = false;
        try {
            connect();
            isAllowed = server.isOperationAllowed(authToken, operationId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            // TODO Add logging
        }
        return isAllowed;
    }
}
