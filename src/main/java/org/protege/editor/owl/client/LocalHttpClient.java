package org.protege.editor.owl.client;

import com.google.gson.Gson;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.*;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;
import edu.stanford.protege.metaproject.impl.Operations;
import edu.stanford.protege.metaproject.impl.UserIdImpl;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import io.undertow.util.StatusCodes;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.messages.EVSHistory;
import org.protege.editor.owl.server.http.messages.HttpAuthResponse;
import org.protege.editor.owl.server.http.messages.LoginCreds;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentSerializer;
import org.semanticweb.binaryowl.owlapi.BinaryOWLOntologyBuildingHandler;
import org.semanticweb.binaryowl.owlapi.OWLOntologyWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LocalHttpClient implements Client, ClientSessionListener {

	private static Logger logger = LoggerFactory.getLogger(LocalHttpClient.class);

	//private AuthToken authToken;
	private String serverAddress;
	private boolean adminClient = false;

	private ProjectId projectId;
	private Project project = null;
	private UserId userId = null;

	private UserInfo userInfo;
	private AuthToken auth_token = null;

	private String auth_header_value;
	private final String AUTH_HEADER = "Authorization";

	OkHttpClient req_client = null; 

	private ServerConfiguration config;
	private AuthenticationRegistry auth_registry;
	private ProjectRegistry proj_registry;
	private UserRegistry user_registry;
	private MetaprojectAgent meta_agent;
	private RoleRegistry role_registry;
	private Policy policy;
	private OperationRegistry op_registry;

	private boolean save_cancel_semantics = true;
	private boolean config_state_changed = false;

	private static LocalHttpClient current_user = null;

	public static LocalHttpClient current_user() {
		return current_user;
	}

	public ServerConfiguration getCurrentConfig() {
		return config;
	}

	public LocalHttpClient(String user, String pwd, String serverAddress) throws Exception {
		req_client = new OkHttpClient.Builder().writeTimeout(360,  TimeUnit.SECONDS).readTimeout(360, TimeUnit.SECONDS).build();
		this.serverAddress = serverAddress;
		this.userInfo = login(user, pwd);
		this.userId = new UserIdImpl(user);
		String toenc = this.userId.get() + ":" + userInfo.getNonce();
		this.auth_header_value = "Basic " + new String(Base64.encodeBase64(toenc.getBytes()));
		LocalHttpClient.current_user = this;
		//check if user is allowed to edit config
		initConfig();
	}
	
	private boolean checkAdmin() {
		int adminPort = config.getHost().getSecondaryPort().get().get();
		int serverAddressPort = URI.create(serverAddress).getPort();
		return adminPort == serverAddressPort;
		
	}

	public void initConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = getConfig();
		adminClient = checkAdmin();
		proj_registry = config.getMetaproject().getProjectRegistry();
		user_registry = config.getMetaproject().getUserRegistry();
		auth_registry = config.getAuthenticationRegistry();
		meta_agent = config.getMetaproject().getMetaprojectAgent();
		role_registry = config.getMetaproject().getRoleRegistry();
		op_registry = config.getMetaproject().getOperationRegistry();
		policy = config.getMetaproject().getPolicy();
		config_state_changed = false;
	}

	private UserInfo login(String user, String pwd) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.LOGIN;
		Serializer<Gson> serl = new DefaultJsonSerializer();
		LoginCreds creds = new LoginCreds(user, pwd);
		RequestBody req = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), serl.write(creds, LoginCreds.class));
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, false);
		try {
			HttpAuthResponse resp = (HttpAuthResponse) serl.parse(new InputStreamReader(response.body().byteStream()), HttpAuthResponse.class);
			return new UserInfo(resp.getId(), resp.getName(), resp.getEmail(), resp.getToken());
		} catch (IOException | ObjectConversionException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	@Override
	public void handleChange(ClientSessionChangeEvent event) {
		if (event.equals(EventCategory.SWITCH_ONTOLOGY)) {
			projectId = event.getSource().getActiveProject();
		}
	}

	@Override
	public void setActiveProject(ProjectId projectId) {
		this.projectId = projectId;
	}

	public Project getCurrentProject() {
		return project;
	}

	@Override
	public AuthToken getAuthToken() {
		if (auth_token == null) {
			User user = null;
			try {
				user = user_registry.get(userId);
			} catch (UnknownMetaprojectObjectIdException e) {
				logger.error(e.getMessage());
				throw new RuntimeException("Client failed to create auth token (see error log for details)", e);
			}
			auth_token = new AuthorizedUserToken(user);
		}
		return auth_token;
	}

	@Override
	public List<User> getAllUsers() throws AuthorizationException, ClientRequestException {
		return new ArrayList<User>(user_registry.getEntries());
	}

	@Override
	public void createUser(User newUser, Optional<? extends Password> password)
			throws AuthorizationException, ClientRequestException {
		try {
			meta_agent.add(newUser);
			if (password.isPresent()) {
				Password newpassword = password.get();
				if (newpassword instanceof SaltedPasswordDigest) {
					auth_registry.add(newUser.getId(), (SaltedPasswordDigest) newpassword);
				}
			}
			putConfig();
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create user (see error log for details)", e);
		}
	}

	@Override
	public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException {
		try {
			meta_agent.remove(user_registry.get(userId));
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete user (see error log for details)", e);
		}
	}

	@Override
	public void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
			throws AuthorizationException, ClientRequestException {
		try {
			user_registry.update(userId, updatedUser);
			if (updatedPassword.isPresent()) {
				Password password = updatedPassword.get();
				if (password instanceof SaltedPasswordDigest) {
					auth_registry.changePassword(userId, (SaltedPasswordDigest) password);
				}
			}
			putConfig();
		} catch (UnknownMetaprojectObjectIdException | UserNotRegisteredException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to update user (see error log for details)", e);
		}
	}

	public ServerDocument createProject(Project proj)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.PROJECT;
		RequestBody req = null;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(proj.getId());
			os.writeObject(proj.getName());
			os.writeObject(proj.getDescription());
			os.writeObject(proj.getOwner());
			Optional<ProjectOptions> options = proj.getOptions();
			ProjectOptions popts = (options.isPresent()) ? options.get() : null;
			os.writeObject(popts);
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ServerDocument sdoc = (ServerDocument) ois.readObject();
			// send snapshot to server
			putSnapShot(proj.getFile(), sdoc);
			initConfig();
			return sdoc;
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	@Override
	public void deleteProject(ProjectId projectId, boolean includeFile)
			throws AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request string
		 */
		String url = HTTPServer.PROJECT + "?projectid=" + projectId.get();
		
		/*
		 * Send the delete request
		 */
		delete(url, true);
		initConfig();
	}

	@Override
	public ServerDocument openProject(ProjectId projectId)
			throws AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request string
		 */
		String url = HTTPServer.PROJECT + "?projectid=" + projectId.get();
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = get(url);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ServerDocument sdoc = (ServerDocument) ois.readObject();
			return sdoc;
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	@Override
	public ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
			throws AuthorizationException, SynchronizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.COMMIT;
		RequestBody req = null;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(projectId);
			os.writeObject(commitBundle);
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ChangeHistory hist = (ChangeHistory) ois.readObject();
			return hist;
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public VersionedOWLOntology buildVersionedOntology(ServerDocument sdoc, OWLOntologyManager owlManager,
			ProjectId pid) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		projectId = pid;
		try {
			project = proj_registry.get(pid);
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get the project (see error log for details)", e);
		}
		
		if (!snapShotExists(sdoc)) {
			getSnapShot(sdoc);
		}
		OWLOntology targetOntology = loadSnapShot(owlManager, sdoc);
		ChangeHistory remoteChangeHistory = getLatestChanges(sdoc, DocumentRevision.START_REVISION);
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}

	public boolean snapShotExists(ServerDocument sdoc) {
		String fileName = sdoc.getHistoryFile().getName() + "-snapshot";
		return (new File(fileName)).exists();	
	}

	public OWLOntology loadSnapShot(OWLOntologyManager manIn, ServerDocument sdoc) throws ClientRequestException {
		try {
//			long beg = System.currentTimeMillis();
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			OWLOntology ontIn = manIn.createOntology();
			String fileName = sdoc.getHistoryFile().getName() + "-snapshot";
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(new File(fileName)));
			serializer.read(inputStream, new BinaryOWLOntologyBuildingHandler(ontIn), manIn.getOWLDataFactory());
//			System.out.println("Time to serialize in " + (System.currentTimeMillis() - beg) / 1000);
			return ontIn;
		} catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to load the ontology snapshot (see error log for details)", e);
		}
	}

	public OWLOntology putSnapShot(File file, ServerDocument sdoc) throws LoginTimeoutException,
	AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.PROJECT_SNAPSHOT;
		OWLOntology ont = null;
		RequestBody req = null;
		try {
			ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(sdoc);
			os.writeObject(new SnapShot(ont));
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response	response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ServerDocument new_sdoc = (ServerDocument) ois.readObject();
			if (new_sdoc != null) {
				return ont;
			} else {
				throw new ClientRequestException("Unexpected error when uploading snapshot to server "
						+ "(contact server admin for further assistance)");
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public void createLocalSnapShot(OWLOntology ont, ServerDocument sdoc) throws ClientRequestException {
		BufferedOutputStream outputStream = null;
		try {
//			long beg = System.currentTimeMillis();
			String fileName = sdoc.getHistoryFile().getName() + "-snapshot";
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			outputStream = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			serializer.write(new OWLOntologyWrapper(ont), new DataOutputStream(outputStream));
//			System.out.println("Time to serialize out snapshot " + (System.currentTimeMillis() - beg)/1000);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to create local snapshot (see error log for details)", e);
		} finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}
	
	public void getSnapShot(ServerDocument sdoc) throws LoginTimeoutException, AuthorizationException,
			ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.PROJECT_SNAPSHOT_GET;
		RequestBody req = null;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(sdoc);
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			SnapShot shot = (SnapShot) ois.readObject();
			createLocalSnapShot(shot.getOntology(), sdoc);
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public ChangeHistory getAllChanges(ServerDocument sdoc) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.ALL_CHANGES;
		RequestBody req = null;
		try {
//			long beg = System.currentTimeMillis();
			// TODO: get all changes
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(sdoc.getHistoryFile());
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ChangeHistory history = (ChangeHistory) ois.readObject();
//			System.out.println("Time to execute get all changes " + (System.currentTimeMillis() - beg)/1000);
			return history;
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public DocumentRevision getRemoteHeadRevision(VersionedOWLOntology vont) throws
			LoginTimeoutException, AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.HEAD;
		RequestBody req = null;
		try {
//			long beg = System.currentTimeMillis();
			// TODO: get all changes
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(vont.getServerDocument().getHistoryFile());
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			DocumentRevision remoteHead = (DocumentRevision) ois.readObject();
			return remoteHead;
//			System.out.println("Time to execute get all changes " + (System.currentTimeMillis() - beg)/1000);
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public ChangeHistory getLatestChanges(VersionedOWLOntology vont) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		DocumentRevision start = vont.getChangeHistory().getHeadRevision();
		return getLatestChanges(vont.getServerDocument(), start);
	}

	public ChangeHistory getLatestChanges(ServerDocument sdoc, DocumentRevision start)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		/*
		 * Prepare the request body
		 */
		String url = HTTPServer.LATEST_CHANGES;
		RequestBody req = null;
		try {
			// TODO: get all changes
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(sdoc.getHistoryFile());
			os.writeObject(start);
			req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
		
		/*
		 * Send the request and stream the response data
		 */
		Response response = post(url, req, true);
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			return (ChangeHistory) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	@Override
	public List<Project> getProjects(UserId userId) throws ClientRequestException {
		try {
			return new ArrayList<>(meta_agent.getProjects(userId));
		} catch (UserNotInPolicyException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get project list (see error log for details)", e);
		}
	}

	@Override
	public List<Project> getAllProjects() throws AuthorizationException, ClientRequestException {
		return new ArrayList<>(proj_registry.getEntries());
	}

	@Override
	public void updateProject(ProjectId projectId, Project updatedProject)
			throws AuthorizationException, ClientRequestException {
		try {
			proj_registry.update(projectId, updatedProject);
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to update project (see error log for details)", e);
		}
	}

	@Override
	public Map<ProjectId, List<Role>> getRoles(UserId userId, GlobalPermissions globalPermissions)
			throws AuthorizationException, ClientRequestException {
		Map<ProjectId, List<Role>> roleMap = new HashMap<>();
		for (Project project : getAllProjects()) {
			roleMap.put(project.getId(), getRoles(userId, project.getId(), globalPermissions));
		}
		return roleMap;
	}

	@Override
	public List<Role> getRoles(UserId userId, ProjectId projectId, GlobalPermissions globalPermissions)
			throws AuthorizationException, ClientRequestException {
		try {
			return new ArrayList<>(meta_agent.getRoles(userId, projectId, globalPermissions));
		} catch (UserNotInPolicyException | ProjectNotInPolicyException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get role list (see error log for details)", e);
		}
	}

	@Override
	public List<Role> getAllRoles() throws AuthorizationException, ClientRequestException {
		return new ArrayList<Role>(this.role_registry.getEntries());
	}

	@Override
	public void createRole(Role newRole) throws AuthorizationException, ClientRequestException {
		 try {
			meta_agent.add(newRole);
			putConfig();
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create role (see error log for details)", e);
		}
	}

	@Override
	public void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException {
		try {
			meta_agent.remove(role_registry.get(roleId));
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete role (see error log for details)", e);
		}
	}

	@Override
	public void updateRole(RoleId roleId, Role updatedRole)
			throws AuthorizationException, ClientRequestException {
		try {
			role_registry.update(roleId, updatedRole);
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to update role (see error log for details)", e);
		}
	}

	@Override
	public Map<ProjectId, List<Operation>> getOperations(UserId userId)
			throws AuthorizationException, ClientRequestException {
		Map<ProjectId, List<Operation>> operationMap = new HashMap<>();
		for (Project project : getAllProjects()) {
			operationMap.put(project.getId(), getOperations(userId, project.getId()));
		}
		return operationMap;
	}

	@Override
	public List<Operation> getOperations(UserId userId, ProjectId projectId)
			throws AuthorizationException, ClientRequestException {
		try {
			return new ArrayList<>(meta_agent.getOperations(userId, projectId, GlobalPermissions.INCLUDED));
		} catch (UserNotInPolicyException | ProjectNotInPolicyException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get operation list (see error log for details)", e);
		}
	}

	@Override
	public List<Operation> getOperations(RoleId roleId)
			throws AuthorizationException, ClientRequestException {
		try {
			return new ArrayList<>(meta_agent.getOperations(role_registry.get(roleId)));
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get operation list (see error log for details)", e);
		}
	}

	@Override
	public List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException {
		return new ArrayList<>(op_registry.getEntries());
	}

	@Override
	public void createOperation(Operation operation)
			throws AuthorizationException, ClientRequestException {
		try {
			meta_agent.add(operation);
			putConfig();
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create operation (see error log for details)", e);
		}
	}

	@Override
	public void deleteOperation(OperationId operationId)
			throws AuthorizationException, ClientRequestException {
		try {
			meta_agent.remove(op_registry.get(operationId));
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete operation (see error log for details)", e);
		}
	}

	@Override
	public void updateOperation(OperationId operationId, Operation updatedOperation)
			throws AuthorizationException, ClientRequestException {
		try {
			op_registry.update(operationId, updatedOperation);
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to update operation (see error log for details)", e);
		}
	}

	@Override
	public void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException {
		policy.add(roleId, projectId, userId);
		putConfig();
	}

	@Override
	public void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException {
		policy.remove(userId, projectId, roleId);
		putConfig();
	}

	@Override
	public Host getHost() throws AuthorizationException, ClientRequestException {
		return config.getHost();
	}

	@Override
	public void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException {
		Host h = Manager.getFactory().getHost(hostAddress, Optional.empty());
		config.setHost(h);

	}

	@Override
	public void setSecondaryPort(int portNumber)
			throws AuthorizationException, ClientRequestException {
		Host h = config.getHost();
		Port p = Manager.getFactory().getPort(portNumber);
		Host nh = Manager.getFactory().getHost(h.getUri(), Optional.of(p));
		config.setHost(nh);

	}

	@Override
	public String getRootDirectory() throws AuthorizationException, ClientRequestException {
		return config.getServerRoot().toString();
	}

	@Override
	public void setRootDirectory(String rootDirectory)
			throws AuthorizationException, ClientRequestException {
		config.setServerRoot(new File(rootDirectory));
		putConfig();
	}

	@Override
	public Map<String, String> getServerProperties()
			throws AuthorizationException, ClientRequestException {
		return config.getProperties();
	}

	@Override
	public void setServerProperty(String property, String value)
			throws AuthorizationException, ClientRequestException {
		config.addProperty(property, value);
		putConfig();
	}

	@Override
	public void unsetServerProperty(String property)
			throws AuthorizationException, ClientRequestException {
		config.removeProperty(property);
		putConfig();
	}

	public Role getRole(RoleId id) throws ClientRequestException {
		try {
			return role_registry.get(id);
		} catch (UnknownMetaprojectObjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get role details (see error log for details)", e);
		}
	}

	@Override
	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public List<Project> getProjects() throws ClientRequestException {
		return getProjects(userId);
	}

	@Override
	public List<Role> getActiveRoles() throws ClientRequestException {
		try {
			List<Role> activeRoles = new ArrayList<>();
			if (getRemoteProject().isPresent()) {
				activeRoles = getRoles(userId, getRemoteProject().get(), GlobalPermissions.INCLUDED);
			}
			return activeRoles;
		} catch (AuthorizationException e) {
			throw new ClientRequestException(e.getMessage(), e);
		}
	}

	@Override
	public List<Operation> getActiveOperations() throws ClientRequestException {
		List<Operation> activeOperations = new ArrayList<>();
		if (getRemoteProject().isPresent()) {
			try {
				activeOperations = getOperations(userId, getRemoteProject().get());
			} catch (AuthorizationException e) {
				throw new ClientRequestException(e.getMessage(), e);
			}
		}
		return activeOperations;
	}

	private Response post(String url, RequestBody body, boolean cred) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		Request request = null;
		if (cred) {
			request = new Request.Builder()
					.url(serverAddress + url)
					.addHeader(AUTH_HEADER, auth_header_value)
					.post(body)
					.build();

		} else {
			request = new Request.Builder()
					.url(serverAddress + url)
					.post(body)
					.build();
		}
		try {
			Response response = req_client.newCall(request).execute();
			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private Response delete(String url, boolean cred) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException  {
		Request request;
		if (cred) {
			request = new Request.Builder()
					.url(serverAddress + url)
					.addHeader(AUTH_HEADER, auth_header_value)
					.delete()
					.build();
		} else {
			request = new Request.Builder()
					.url(serverAddress + url)
					.delete()
					.build();
		}
		try {
			Response response = req_client.newCall(request).execute();
			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private Response get(String url) throws LoginTimeoutException, AuthorizationException,
			ClientRequestException {
		Request request = new Request.Builder()
				.url(serverAddress + url)
				.addHeader(AUTH_HEADER, auth_header_value)
				.get()
				.build();
		try {
			Response response = req_client.newCall(request).execute();
			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ServerConfiguration getConfig() throws LoginTimeoutException, AuthorizationException,
			ClientRequestException {
		String url = HTTPServer.METAPROJECT;
		Response response = get(url);
		try {
			Serializer<Gson> serl = new DefaultJsonSerializer();
			InputStream is = response.body().byteStream();
			return (ServerConfiguration) serl.parse(new InputStreamReader(is), ServerConfiguration.class);
		} catch (ObjectConversionException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to parse the incoming server configuration data", e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to receive data", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public String getCode() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		String url = HTTPServer.GEN_CODE;
		Response response = get(url);
		try {
			return response.body().string();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to receive data (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public void putConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		if (save_cancel_semantics) {
			config_state_changed = true;
		} else {
			reallyPutConfig();
		}
	}
	
	public boolean configStateChanged() {
		return config_state_changed;
	}
	
	public void reallyPutConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
		String url = HTTPServer.METAPROJECT;

		Serializer<Gson> serl = new DefaultJsonSerializer();
		RequestBody body = RequestBody.create(JSON, serl.write(this.config, ServerConfiguration.class));

		post(url, body, true);
		try {
			// give the server some time to reboot
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		initConfig();
	}
	
	public void putEVSHistory(String code, String name, String operation, String reference)
			 throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		String url = HTTPServer.EVS_REC;
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		try {
			EVSHistory hist = new EVSHistory(code, name, operation, reference);
			ObjectOutputStream os = new ObjectOutputStream(b);
			os.writeObject(hist);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());
			post(url, req, true);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to send data (see error log for details)", e);
		}
	}

	private void throwRequestExceptions(Response response) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		String originalMessage = response.header("Error-Message");
		if (response.code() == StatusCodes.UNAUTHORIZED) {
			throw new AuthorizationException(originalMessage);
		}
		/*
		 * 440 Login Timeout. Reference: https://support.microsoft.com/en-us/kb/941201
		 */
		else if (response.code() == 440) {
			throw new LoginTimeoutException(originalMessage);
		}
		else {
			String msg = String.format("%s (contact server admin for further assistance)", originalMessage);
			throw new ClientRequestException(msg);
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
    	return (adminClient && meta_agent.isOperationAllowed(operationId, projectId, userId));        
    }

    private boolean queryAdminPolicy(UserId userId, OperationId operationId) {
    	return (adminClient && meta_agent.isOperationAllowed(operationId, userId));
        
    }
    
    private Optional<ProjectId> getRemoteProject() {
        return Optional.ofNullable(projectId);
    }

	@Override
	public ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
			Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
					throws AuthorizationException, ClientRequestException {
		// TODO Auto-generated method stub
		return null;
	}
}
