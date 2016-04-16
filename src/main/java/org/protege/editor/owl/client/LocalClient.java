package org.protege.editor.owl.client;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;
import org.protege.owl.server.changes.ServerDocument;
import org.protege.owl.server.connect.RmiServer;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
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

    private ProjectId projectId;
    private UserId userId;

    private RmiServer server;

    public LocalClient(AuthToken authToken, String serverAddress) {
        this.authToken = authToken;
        this.serverAddress = serverAddress;
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
    public User getUser() {
        return authToken.getUser();
    }

    @Override
    public List<Project> getProjects() throws ClientRequestException {
        return getProjects(userId);
    }

    @Override
    public List<Role> getActiveRoles() throws ClientRequestException {
        return getRoles(userId, projectId);
    }

    @Override
    public List<Operation> getActiveOperations() throws ClientRequestException {
        return getOperations(userId, projectId);
    }

    protected void connect() throws ClientRequestException {
        if (server == null) {
            try {
                server = (RmiServer) ServerUtils.getRemoteService(serverAddress, RmiServer.SERVER_SERVICE);
            }
            catch (RemoteException e) {
                throw new ClientRequestException(e);
            }
        }
    }

    public void disconnect() {
        server = null;
    }

    @Override
    public void createUser(User newUser) throws ClientRequestException {
        try {
            connect();
            server.createUser(authToken, newUser);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void deleteUser(UserId userId) throws ClientRequestException {
        try {
            connect();
            server.deleteUser(authToken, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void updateUser(UserId userId, User updatedUser) throws ClientRequestException {
        try {
            connect();
            server.updateUser(authToken, userId, updatedUser);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void createProject(Project newProject) throws ClientRequestException {
        try {
            connect();
            server.createProject(authToken, newProject);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void deleteProject(ProjectId projectId) throws ClientRequestException {
        try {
            connect();
            server.deleteProject(authToken, projectId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void updateProject(ProjectId projectId, Project updatedProject) throws ClientRequestException {
        try {
            connect();
            server.updateProject(authToken, projectId, updatedProject);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public ServerDocument openProject(ProjectId projectId) throws ClientRequestException {
        try {
            connect();
            return server.openProject(authToken, projectId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void createRole(Role newRole) throws ClientRequestException {
        try {
            connect();
            server.createRole(authToken, newRole);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void deleteRole(RoleId roleId) throws ClientRequestException {
        try {
            connect();
            server.deleteRole(authToken, roleId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void updateRole(RoleId roleId, Role updatedRole) throws ClientRequestException {
        try {
            connect();
            server.updateRole(authToken, roleId, updatedRole);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void createOperation(Operation operation) throws ClientRequestException {
        try {
            connect();
            server.createOperation(authToken, operation);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void deleteOperation(OperationId operationId) throws ClientRequestException {
        try {
            connect();
            server.deleteOperation(authToken, operationId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void updateOperation(OperationId operationId, Operation updatedOperation) throws ClientRequestException {
        try {
            connect();
            server.updateOperation(authToken, operationId, updatedOperation);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws ClientRequestException {
        try {
            connect();
            server.assignRole(authToken, userId, projectId, roleId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws ClientRequestException {
        try {
            connect();
            server.retractRole(authToken, userId, projectId, roleId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void setServerConfiguration(String property, String value) throws ClientRequestException {
        try {
            connect();
            server.setServerConfiguration(authToken, property, value);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public void commit(Project project, CommitBundle commits) throws ClientRequestException {
        try {
            connect();
            server.commit(authToken, project, commits);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddAxiom() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_AXIOM.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveAxiom() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_AXIOM.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddAnnotatation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_ONTOLOGY_ANNOTATION.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveAnnotation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_ONTOLOGY_ANNOTATION.getId(), projectId,
                    userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddImport() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_IMPORT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveImport() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_IMPORT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyOntologyId() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_ONTOLOGY_IRI.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_USER.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_USER.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyUser() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_USER.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_PROJECT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_PROJECT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_PROJECT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canViewProject() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.VIEW_PROJECT.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_ROLE.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_ROLE.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_ROLE.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAddOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ADD_OPERATION.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRemoveOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.REMOVE_OPERATION.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyOperation() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_OPERATION.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canAssignRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.ASSIGN_ROLE.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRetractRole() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.RETRACT_ROLE.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canStopServer() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.STOP_SERVER.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canRestartServer() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.RESTART_SERVER.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canModifyServerConfig() throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, Operations.MODIFY_SERVER_CONFIG.getId(), projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public boolean canPerformOperation(OperationId operationId) throws ClientRequestException {
        try {
            connect();
            return server.isOperationAllowed(authToken, operationId, projectId, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<User> getAllUsers() throws ClientRequestException {
        try {
            connect();
            return server.getAllUsers(authToken);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Project> getProjects(UserId userId) throws ClientRequestException {
        try {
            connect();
            return server.getProjects(authToken, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Project> getAllProjects() throws ClientRequestException {
        try {
            connect();
            return server.getAllProjects(authToken);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public Map<ProjectId, List<Role>> getRoles(UserId userId) throws ClientRequestException {
        try {
            connect();
            return server.getRoles(authToken, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Role> getRoles(UserId userId, ProjectId projectId) throws ClientRequestException {
        try {
            connect();
            return server.getRoles(authToken, userId, projectId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Role> getAllRoles() throws ClientRequestException {
        try {
            connect();
            return server.getAllRoles(authToken);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public Map<ProjectId, List<Operation>> getOperations(UserId userId) throws ClientRequestException {
        try {
            connect();
            return server.getOperations(authToken, userId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Operation> getOperations(UserId userId, ProjectId projectId) throws ClientRequestException {
        try {
            connect();
            return server.getOperations(authToken, userId, projectId);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }

    @Override
    public List<Operation> getAllOperations() throws ClientRequestException {
        try {
            connect();
            return server.getAllOperations(authToken);
        }
        catch (ServerRequestException e) {
            throw new ClientRequestException(e);
        }
    }
}
