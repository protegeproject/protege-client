package org.protege.editor.owl.client.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.protege.editor.owl.client.api.ClientRequests;
import org.protege.editor.owl.client.api.PolicyMediator;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.GlobalPermissions;
import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.Port;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.IdAlreadyInUseException;
import edu.stanford.protege.metaproject.api.exception.UnknownOperationIdException;
import edu.stanford.protege.metaproject.api.exception.UnknownRoleIdException;
import edu.stanford.protege.metaproject.api.exception.UnknownUserIdException;
import edu.stanford.protege.metaproject.impl.ConfigurationBuilder;
import edu.stanford.protege.metaproject.impl.Operations;
import edu.stanford.protege.metaproject.impl.RoleIdImpl;

public class Config implements PolicyMediator, ClientRequests {
	
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private ServerConfiguration config = null;	
	
	public ServerConfiguration getCurrentConfig() {
		return config;
	}
	
	private UserId userId = null;
	private ProjectId projectId = null;
	
	public void setActiveProject(ProjectId projectId) {
		this.projectId = projectId;
	}
	
	public Optional<ProjectId> getRemoteProject() {
		return Optional.ofNullable(projectId);
	}
	
	
	public Config(ServerConfiguration cfg, UserId user) {
		config = cfg;
		userId = user;
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
		return queryAdminPolicy(userId, Operations.REMOVE_PROJECT.getId());
	}

	@Override
	public boolean canUpdateProject() {
		return queryAdminPolicy(userId, Operations.MODIFY_PROJECT.getId());
	}

	@Override
	public boolean canOpenProject() {
		return queryAdminPolicy(userId, Operations.OPEN_PROJECT.getId());
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
		return queryAdminPolicy(userId, operationId);
	}
	
	@Override
	public List<User> getAllUsers() {
		return new ArrayList<>(config.getUsers());
	}
	
	@Override
	public void createUser(User newUser, Optional<? extends Password> password)
			throws AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.addUser(newUser)
					.createServerConfiguration();
			if (password.isPresent()) {
				Password newpassword = password.get();
				if (newpassword instanceof SaltedPasswordDigest) {
					config = new ConfigurationBuilder(config)
							.registerUser(newUser.getId(), (SaltedPasswordDigest) newpassword)
							.createServerConfiguration();
				}
			}
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create user (see error log for details)", e);
		}
	}

	@Override
	public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.removeUser(config.getUser(userId))
					.removePolicy(userId)
                    .unregisterUser(userId)
					.createServerConfiguration();
		} catch (UnknownUserIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete user (see error log for details)", e);
		}
	}

	@Override
	public void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
			throws AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.setUser(userId, updatedUser)
				.createServerConfiguration();
		if (updatedPassword.isPresent()) {
			Password password = updatedPassword.get();
			if (password instanceof SaltedPasswordDigest) {
				config = new ConfigurationBuilder(config)
						.changePassword(userId, (SaltedPasswordDigest) password)
						.createServerConfiguration();
			}
		}
	}
	
	@Override
	public List<Project> getAllProjects() {
		return new ArrayList<>(config.getProjects());
	}
	
	@Override
	public List<Project> getProjects(UserId userId) {
		return new ArrayList<>(config.getProjects(userId));
	}
	
	@Override
	public void updateProject(ProjectId projectId, Project updatedProject)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.setProject(projectId, updatedProject)
				.createServerConfiguration();
	}

	@Override
	public Map<ProjectId, List<Role>> getRoles(UserId userId, GlobalPermissions globalPermissions) {
		Map<ProjectId, List<Role>> roleMap = new HashMap<>();
		for (Project project : getAllProjects()) {
			roleMap.put(project.getId(), getRoles(userId, project.getId(), globalPermissions));
		}
		return roleMap;
	}

	@Override
	public List<Role> getRoles(UserId userId, ProjectId projectId, GlobalPermissions globalPermissions) {
		return new ArrayList<>(config.getRoles(userId, projectId, globalPermissions));
	}

	@Override
	public List<Role> getAllRoles() {
		return new ArrayList<>(config.getRoles());
	}
	
	public Role getRole(RoleId id) throws ClientRequestException {
		try {
			return config.getRole(id);
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get role details (see error log for details)", e);
		}
	}

	@Override
	public void createRole(Role newRole) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.addRole(newRole)
					.createServerConfiguration();
			
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create role (see error log for details)", e);
		}
	}

	@Override
	public void deleteRole(RoleId roleId) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.removeRole(config.getRole(roleId))
					.createServerConfiguration();
			
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete role (see error log for details)", e);
		}
	}

	@Override
	public void updateRole(RoleId roleId, Role updatedRole)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.setRole(roleId, updatedRole)
				.createServerConfiguration();
		
	}

	@Override
	public Map<ProjectId, List<Operation>> getOperations(UserId userId) {
		Map<ProjectId, List<Operation>> operationMap = new HashMap<>();
		for (Project project : getAllProjects()) {
			operationMap.put(project.getId(), getOperations(userId, project.getId()));
		}
		return operationMap;
	}

	@Override
	public List<Operation> getOperations(UserId userId, ProjectId projectId) {
		return new ArrayList<>(config.getOperations(userId, projectId, GlobalPermissions.INCLUDED));
	}

	@Override
	public List<Operation> getOperations(RoleId roleId) throws ClientRequestException {
		try {
			return new ArrayList<>(config.getOperations(config.getRole(roleId)));
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get operation list (see error log for details)", e);
		}
	}

	@Override
	public List<Operation> getAllOperations() {
		return new ArrayList<>(config.getOperations());
	}

	@Override
	public void createOperation(Operation operation)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.addOperation(operation)
					.createServerConfiguration();
			
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create operation (see error log for details)", e);
		}
	}

	@Override
	public void deleteOperation(OperationId operationId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			config = new ConfigurationBuilder(config)
					.removeOperation(config.getOperation(operationId))
					.createServerConfiguration();
			
		} catch (UnknownOperationIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete operation (see error log for details)", e);
		}
	}

	@Override
	public void updateOperation(OperationId operationId, Operation updatedOperation)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.setOperation(operationId, updatedOperation)
				.createServerConfiguration();
		
	}

	@Override
	public void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.addPolicy(userId, projectId, roleId)
				.createServerConfiguration();
		
	}

	@Override
	public void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.removePolicy(userId, projectId, roleId)
				.createServerConfiguration();
		
	}
	
	@Override
	public Host getHost() {
		return config.getHost();
	}

	@Override
	public void setHostAddress(URI hostAddress) {
		Host host = ConfigurationManager.getFactory().getHost(hostAddress, Optional.empty());
		config = new ConfigurationBuilder(config)
				.setHost(host)
				.createServerConfiguration();
	}

	@Override
	public void setSecondaryPort(int portNumber) {
		Host h = config.getHost();
		Port p = ConfigurationManager.getFactory().getPort(portNumber);
		Host nh = ConfigurationManager.getFactory().getHost(h.getUri(), Optional.of(p));
		config = new ConfigurationBuilder(config)
				.setHost(nh)
				.createServerConfiguration();
	}
	
	@Override
	public String getRootDirectory() {
		return config.getServerRoot().toString();
	}

	@Override
	public void setRootDirectory(String rootDirectory)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.setServerRoot(rootDirectory)
				.createServerConfiguration();
		
	}

	@Override
	public Map<String, String> getServerProperties() {
		return config.getProperties();
	}

	@Override
	public void setServerProperty(String property, String value)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.addProperty(property, value)
				.createServerConfiguration();
		
	}

	@Override
	public void unsetServerProperty(String property)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = new ConfigurationBuilder(config)
				.removeProperty(property)
				.createServerConfiguration();
		
	}



	
	/*
	 * Utility methods
	 */
	public boolean queryProjectPolicy(UserId userId, ProjectId projectId, OperationId operationId) {
		return config.isOperationAllowed(operationId, projectId, userId);
	}

	public boolean queryAdminPolicy(UserId userId, OperationId operationId) {

		return config.isOperationAllowed(operationId, userId);
	}

}
