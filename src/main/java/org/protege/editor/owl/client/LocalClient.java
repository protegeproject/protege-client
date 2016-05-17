package org.protege.editor.owl.client;

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
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.AuthToken;
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
import edu.stanford.protege.metaproject.impl.Operations;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class LocalClient implements Client {

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

    public void setActiveProject(ProjectId projectId) {
        this.projectId = projectId;
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
            return getRoles(userId, projectId);
        }
        catch (AuthorizationException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Operation> getActiveOperations() throws ClientRequestException {
        try {
            return getOperations(userId, projectId);
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
    public void createUser(User newUser, Optional<SaltedPasswordDigest> password) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            SaltedPasswordDigest passwordValue = (password.isPresent()) ? password.get() : null;
            server.createUser(authToken, newUser, passwordValue);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteUser(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void updateUser(UserId userId, User updatedUser) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateUser(authToken, userId, updatedUser);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
            Optional<ProjectOptions> options) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            ProjectOptions optionsValue = (options.isPresent()) ? options.get() : null;
            return server.createProject(authToken, projectId, projectName, description, owner, optionsValue);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteProject(ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteProject(authToken, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void updateProject(ProjectId projectId, Project updatedProject) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateProject(authToken, projectId, updatedProject);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public ServerDocument openProject(ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.openProject(authToken, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void createRole(Role newRole) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.createRole(authToken, newRole);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteRole(authToken, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void updateRole(RoleId roleId, Role updatedRole) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateRole(authToken, roleId, updatedRole);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void createOperation(Operation operation) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.createOperation(authToken, operation);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteOperation(OperationId operationId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.deleteOperation(authToken, operationId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void updateOperation(OperationId operationId, Operation updatedOperation) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.updateOperation(authToken, operationId, updatedOperation);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.assignRole(authToken, userId, projectId, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.retractRole(authToken, userId, projectId, roleId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public Host getHost() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getHost(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setHostAddress(authToken, hostAddress);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void setSecondaryPort(int portNumber) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setSecondaryPort(authToken, portNumber);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public String getRootDirectory() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRootDirectory(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void setRootDirectory(String rootDirectory) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setRootDirectory(authToken, rootDirectory);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getServerProperties() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getServerProperties(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void setServerProperty(String property, String value) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.setServerProperty(authToken, property, value);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void unsetServerProperty(String property) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            server.unsetServerProperty(authToken, property);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public void commit(Project project, CommitBundle commits)
            throws AuthorizationException, OutOfSyncException, ClientRequestException, RemoteException {
        try {
            connect();
            server.commit(authToken, project, commits);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAllUsers() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllUsers(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Project> getProjects(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getProjects(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Project> getAllProjects() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllProjects(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public Map<ProjectId, List<Role>> getRoles(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRoles(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Role> getRoles(UserId userId, ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getRoles(authToken, userId, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Role> getAllRoles() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllRoles(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public Map<ProjectId, List<Operation>> getOperations(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getOperations(authToken, userId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Operation> getOperations(UserId userId, ProjectId projectId) throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getOperations(authToken, userId, projectId);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException, RemoteException {
        try {
            connect();
            return server.getAllOperations(authToken);
        }
        catch (ServerServiceException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }



    @Override
    public boolean canAddAxiom() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_AXIOM.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canRemoveAxiom() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_AXIOM.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canAddAnnotation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_ONTOLOGY_ANNOTATION.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canRemoveAnnotation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_ONTOLOGY_ANNOTATION.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canAddImport() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_IMPORT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canRemoveImport() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_IMPORT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canModifyOntologyId() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_ONTOLOGY_IRI.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canCreateUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_USER.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canDeleteUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_USER.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canUpdateUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_USER.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canCreateProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_PROJECT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canDeleteProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_PROJECT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canUpdateProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_PROJECT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canOpenProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.OPEN_PROJECT.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canCreateRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_ROLE.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canDeleteRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_ROLE.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canUpdateRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_ROLE.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canCreateOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_OPERATION.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canDeleteOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_OPERATION.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canUpdateOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_OPERATION.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canAssignRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ASSIGN_ROLE.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canRetractRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.RETRACT_ROLE.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canStopServer() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.STOP_SERVER.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canRestartServer() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.RESTART_SERVER.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canUpdateServerConfig() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_SERVER_CONFIG.getId(), projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }

    @Override
    public boolean canPerformOperation(OperationId operationId) throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, operationId, projectId, userId);
        }
        catch (AuthorizationException | ServerServiceException | RemoteException e) {
            throw new ClientRequestException(e.getMessage(), e);
        }
    }
}
