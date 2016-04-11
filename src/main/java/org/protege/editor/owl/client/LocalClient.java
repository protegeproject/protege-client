package org.protege.editor.owl.client;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;
import org.protege.owl.server.connect.RmiServer;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ClientConfiguration;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.PolicyAgent;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.impl.Operations;

public class LocalClient implements Client {

    private AuthToken authToken;
    private String serverAddress;

    private ProjectId projectId;
    private UserId userId;

    private RmiServer server;
    private ClientConfiguration clientConfiguration;
    private PolicyAgent policyAgent;

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

    protected void connect() throws ServerRequestException {
        if (server == null) {
            try {
                server = (RmiServer) ServerUtils.getRemoteService(serverAddress, RmiServer.SERVER_SERVICE);
                clientConfiguration = server.getClientConfiguration(userId);
                policyAgent = clientConfiguration.getMetaproject().getPolicyAgent();
            }
            catch (RemoteException e) {
                throw new ServerRequestException(e);
            }
        }
    }

    public void disconnect() {
        server = null;
        clientConfiguration = null;
        policyAgent = null;
    }

    @Override
    public void addUser(User newUser) throws ServerRequestException {
        connect();
        server.addUser(authToken, newUser);
    }

    @Override
    public void removeUser(UserId userId) throws ServerRequestException {
        connect();
        server.removeUser(authToken, userId);
    }

    @Override
    public void modifyUser(UserId userId, User newUser) throws ServerRequestException {
        connect();
        server.modifyUser(authToken, userId, newUser);
    }

    @Override
    public void addProject(Project newProject) throws ServerRequestException {
        connect();
        server.addProject(authToken, newProject);
    }

    @Override
    public void removeProject(ProjectId projectId) throws ServerRequestException {
        connect();
        server.removeProject(authToken, projectId);
    }

    @Override
    public void modifyProject(ProjectId projectId, Project newProject) throws ServerRequestException {
        connect();
        server.modifyProject(authToken, projectId, newProject);
    }

    @Override
    public void viewProject(ProjectId projectId) throws ServerRequestException {
        connect();
        server.viewProject(authToken, projectId);
    }

    @Override
    public void addRole(Role newRole) throws ServerRequestException {
        connect();
        server.addRole(authToken, newRole);
    }

    @Override
    public void removeRole(RoleId roleId) throws ServerRequestException {
        connect();
        server.removeRole(authToken, roleId);
    }

    @Override
    public void modifyRole(RoleId roleId, Role newRole) throws ServerRequestException {
        connect();
        server.modifyRole(authToken, roleId, newRole);
    }

    @Override
    public void addOperation(Operation operation) throws ServerRequestException {
        connect();
        server.addOperation(authToken, operation);
    }

    @Override
    public void removeOperation(OperationId operationId) throws ServerRequestException {
        connect();
        server.removeOperation(authToken, operationId);
    }

    @Override
    public void modifyOperation(OperationId operationId, Operation newOperation) throws ServerRequestException {
        connect();
        server.modifyOperation(authToken, operationId, newOperation);
    }

    @Override
    public void assignRole(UserId userId, ProjectId projectId, RoleId roleId) throws ServerRequestException {
        connect();
        server.assignRole(authToken, userId, projectId, roleId);
    }

    @Override
    public void retractRole(UserId userId, ProjectId projectId, RoleId roleId) throws ServerRequestException {
        connect();
        server.retractRole(authToken, userId, projectId, roleId);
    }

    @Override
    public void modifyServerConfiguration(String property, String value) throws ServerRequestException {
        connect();
        server.modifyServerConfiguration(authToken, property, value);
    }

    @Override
    public void commit(Project project, CommitBundle commits) throws ServerRequestException {
        connect();
        server.commit(authToken, project, commits);
    }

    @Override
    public boolean canAddAxiom() {
        return policyAgent.isOperationAllowed(Operations.ADD_AXIOM.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveAxiom() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_AXIOM.getId(), projectId, userId);
    }

    @Override
    public boolean canAddAnnotatation() {
        return policyAgent.isOperationAllowed(Operations.ADD_ONTOLOGY_ANNOTATION.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveAnnotation() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_ONTOLOGY_ANNOTATION.getId(), projectId, userId);
    }

    @Override
    public boolean canAddImport() {
        return policyAgent.isOperationAllowed(Operations.ADD_IMPORT.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveImport() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_IMPORT.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyOntologyId() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_ONTOLOGY_IRI.getId(), projectId, userId);
    }

    @Override
    public boolean canAddUser() {
        return policyAgent.isOperationAllowed(Operations.ADD_USER.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveUser() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_USER.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyUser() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_USER.getId(), projectId, userId);
    }

    @Override
    public boolean canAddProject() {
        return policyAgent.isOperationAllowed(Operations.ADD_PROJECT.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveProject() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_PROJECT.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyProject() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_PROJECT.getId(), projectId, userId);
    }

    @Override
    public boolean canViewProject() {
        return policyAgent.isOperationAllowed(Operations.VIEW_PROJECT.getId(), projectId, userId);
    }

    @Override
    public boolean canAddRole() {
        return policyAgent.isOperationAllowed(Operations.ADD_ROLE.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveRole() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_ROLE.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyRole() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_ROLE.getId(), projectId, userId);
    }

    @Override
    public boolean canAddOperation() {
        return policyAgent.isOperationAllowed(Operations.ADD_OPERATION.getId(), projectId, userId);
    }

    @Override
    public boolean canRemoveOperation() {
        return policyAgent.isOperationAllowed(Operations.REMOVE_OPERATION.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyOperation() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_OPERATION.getId(), projectId, userId);
    }

    @Override
    public boolean canAssignRole() {
        return policyAgent.isOperationAllowed(Operations.ASSIGN_ROLE.getId(), projectId, userId);
    }

    @Override
    public boolean canRetractRole() {
        return policyAgent.isOperationAllowed(Operations.RETRACT_ROLE.getId(), projectId, userId);
    }

    @Override
    public boolean canStopServer() {
        return policyAgent.isOperationAllowed(Operations.STOP_SERVER.getId(), projectId, userId);
    }

    @Override
    public boolean canRestartServer() {
        return policyAgent.isOperationAllowed(Operations.RESTART_SERVER.getId(), projectId, userId);
    }

    @Override
    public boolean canModifyServerConfig() {
        return policyAgent.isOperationAllowed(Operations.MODIFY_SERVER_CONFIG.getId(), projectId, userId);
    }

    @Override
    public boolean canPerformOperation(OperationId operationId) {
        return policyAgent.isOperationAllowed(operationId, projectId, userId);
    }

    @Override
    public Set<Project> getProjects() {
        return null;
//        return clientConfiguration.getMetaproject().getPolicy().getProjects(userId);
    }

    @Override
    public Set<Role> getRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Operation> getOperations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<User> getAllUsers() {
        return clientConfiguration.getMetaproject().getUserRegistry().getUsers();
    }

    @Override
    public Set<Project> getAllProjects() {
        return clientConfiguration.getMetaproject().getProjectRegistry().getProjects();
    }

    @Override
    public Set<Role> getAllRoles() {
        return clientConfiguration.getMetaproject().getRoleRegistry().getRoles();
    }

    @Override
    public Set<Operation> getAllOperations() {
        return clientConfiguration.getMetaproject().getOperationRegistry().getOperations();
    }

    @Override
    public Map<UserId, Map<ProjectId, Set<RoleId>>> getPolicyMappings() {
        return clientConfiguration.getMetaproject().getPolicy().getPolicyMappings();
    }
}
