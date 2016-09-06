package org.protege.editor.owl.client;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.*;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;
import edu.stanford.protege.metaproject.impl.ConfigurationBuilder;
import edu.stanford.protege.metaproject.impl.Operations;
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
import org.protege.editor.owl.server.http.messages.EVSHistory;
import org.protege.editor.owl.server.http.messages.HttpAuthResponse;
import org.protege.editor.owl.server.http.messages.LoginCreds;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.*;
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

import static org.protege.editor.owl.server.http.ServerEndpoints.*;

public class LocalHttpClient implements Client, ClientSessionListener {

	public enum UserType { NON_ADMIN, ADMIN }

	private static final Logger logger = LoggerFactory.getLogger(LocalHttpClient.class);

	private static final MediaType JsonContentType = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType ApplicationContentType = MediaType.parse("application");

	private static final String authHeader = "Authorization";

	private final String serverAddress;

	private final OkHttpClient httpClient;

	private UserId userId;
	private UserInfo userInfo;

	private ProjectId projectId;
	private Project project;

	private AuthToken authToken;

	private ServerConfiguration serverConfiguration;
	private Serializer serl = new DefaultJsonSerializer();

	private boolean saveCancelSemantics = true;
	private boolean configStateChanged = false;

	private static LocalHttpClient currentHttpClient;

	/**
	 * The constructor
	 */
	public LocalHttpClient(String username, String password, String serverAddress)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException{
		httpClient = new OkHttpClient.Builder()
				.writeTimeout(360, TimeUnit.SECONDS)
				.readTimeout(360, TimeUnit.SECONDS)
				.build();
		this.serverAddress = serverAddress;
		login(username, password);
		initConfig();
		initAuthToken();
		LocalHttpClient.currentHttpClient = this;
	}

	public static LocalHttpClient current_user() {
		return currentHttpClient;
	}

	public ServerConfiguration getCurrentConfig() {
		return serverConfiguration;
	}

	public UserType getClientType() {
		int adminPort = serverConfiguration.getHost().getSecondaryPort().get().get();
		int serverAddressPort = URI.create(serverAddress).getPort();
		return (adminPort == serverAddressPort) ? UserType.ADMIN : UserType.NON_ADMIN;
	}

	public void initConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = getConfig();
		configStateChanged = false;
	}

	private void login(String username, String password)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		LoginCreds creds = new LoginCreds(username, password);
		Response response = post(LOGIN,
				RequestBody.create(JsonContentType, serl.write(creds, LoginCreds.class)),
				false); // send the request to server
		userInfo = retrieveUserInfoFromServerResponse(response);
		userId = ConfigurationManager.getFactory().getUserId(userInfo.getId());
	}

	private void initAuthToken() {
		try {
			User user = serverConfiguration.getUser(userId);
			authToken = new AuthorizedUserToken(user);
		} catch (UnknownUserIdException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
	}

	private UserInfo retrieveUserInfoFromServerResponse(Response response)
			throws ClientRequestException {
		try {
			InputStream is = response.body().byteStream();
			HttpAuthResponse authResponse = (HttpAuthResponse) serl.parse(new InputStreamReader(is), HttpAuthResponse.class);
			return new UserInfo(authResponse.getId(), authResponse.getName(), authResponse.getEmail(), authResponse.getToken());
		} catch (ObjectConversionException e) {
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
		return authToken;
	}

	@Override
	public List<User> getAllUsers() {
		return new ArrayList<>(serverConfiguration.getUsers());
	}

	@Override
	public void createUser(User newUser, Optional<? extends Password> password)
			throws AuthorizationException, ClientRequestException {
		try {
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.addUser(newUser)
					.createServerConfiguration();
			if (password.isPresent()) {
				Password newpassword = password.get();
				if (newpassword instanceof SaltedPasswordDigest) {
					serverConfiguration = new ConfigurationBuilder(serverConfiguration)
							.registerUser(newUser.getId(), (SaltedPasswordDigest) newpassword)
							.createServerConfiguration();
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
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.removeUser(serverConfiguration.getUser(userId))
					.createServerConfiguration();
			putConfig();
		} catch (UnknownUserIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete user (see error log for details)", e);
		}
	}

	@Override
	public void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
			throws AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setUser(userId, updatedUser)
				.createServerConfiguration();
		if (updatedPassword.isPresent()) {
			Password password = updatedPassword.get();
			if (password instanceof SaltedPasswordDigest) {
				serverConfiguration = new ConfigurationBuilder(serverConfiguration)
						.changePassword(userId, (SaltedPasswordDigest) password)
						.createServerConfiguration();
			}
		}
		putConfig();
	}

	@Override
	public ServerDocument createProject(ProjectId projectId, Name projectName, Description description,
			UserId owner, Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
			throws AuthorizationException, ClientRequestException {
		throw new RuntimeException("Use method createProject(Project) instead");
	}

	public ServerDocument createProject(Project project)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(project);
			Response response = post(PROJECT,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send the request to server
			ServerDocument sdoc = retrieveServerDocumentFromServerResponse(response);
			File ontologyFile = project.getFile();
			putSnapShot(ontologyFile, sdoc); // send snapshot to server
			initConfig();
			return sdoc;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(Project proj) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(proj.getId());
		os.writeObject(proj.getName());
		os.writeObject(proj.getDescription());
		os.writeObject(proj.getOwner());
		Optional<ProjectOptions> options = proj.getOptions();
		ProjectOptions popts = (options.isPresent()) ? options.get() : null;
		os.writeObject(popts);
		return b;
	}

	private ServerDocument retrieveServerDocumentFromServerResponse(Response response)
			throws ClientRequestException {
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
	public void deleteProject(ProjectId projectId, boolean includeFile)
			throws AuthorizationException, LoginTimeoutException, ClientRequestException {
		String requestUrl = PROJECT + "?projectid=" + projectId.get();
		delete(requestUrl, true); // send request to server
		initConfig();
	}

	@Override
	public ServerDocument openProject(ProjectId projectId)
			throws AuthorizationException, LoginTimeoutException, ClientRequestException {
		if (getClientType() == UserType.ADMIN) { // admin clients cannot edit/browse ontologies
			throw new ClientRequestException("Admin clients cannot open projects");
		}
		String requestUrl = PROJECT + "?projectid=" + projectId.get();
		Response response = get(requestUrl); // send request to server
		return retrieveServerDocumentFromServerResponse(response);
	}

	@Override
	public ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
			throws AuthorizationException, LoginTimeoutException, SynchronizationException, ClientRequestException {
		try {
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(projectId, commitBundle);
			Response response = post(COMMIT,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(ProjectId projectId,
			CommitBundle commitBundle) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(projectId);
		os.writeObject(commitBundle);
		return b;
	}

	private ChangeHistory retrieveChangeHistoryFromServerResponse(Response response)
			throws ClientRequestException {
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

	private List<String> retrieveCodesFromServerResponse(Response response) throws ClientRequestException {
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			@SuppressWarnings("unchecked")
			List<String> codes = (List<String>) ois.readObject();
			return codes;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to receive data (see error log for details)", e);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Internal error, server sent wrong object back", e);
			
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
		
	}

	public VersionedOWLOntology buildVersionedOntology(ServerDocument sdoc, OWLOntologyManager owlManager,
			ProjectId pid) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		setCurrentProject(pid);
		if (!getSnapShotFile(sdoc).exists()) {
			SnapShot snapshot = getSnapShot(pid);
			createLocalSnapShot(snapshot.getOntology(), sdoc);
		}
		OWLOntology targetOntology = loadSnapShot(owlManager, sdoc);
		ChangeHistory remoteChangeHistory = getLatestChanges(sdoc, DocumentRevision.START_REVISION);
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}

	private void setCurrentProject(ProjectId pid) throws ClientRequestException {
		try {
			projectId = pid;
			project = serverConfiguration.getProject(pid);
		} catch (UnknownProjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get the project (see error log for details)", e);
		}
	}

	private static File getSnapShotFile(ServerDocument sdoc) {
		String fname = sdoc.getHistoryFile().getName() + "-snapshot";
		return new File(fname);
	}

	public OWLOntology loadSnapShot(OWLOntologyManager manIn, ServerDocument sdoc) throws ClientRequestException {
		try {
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			OWLOntology ontIn = manIn.createOntology();
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(getSnapShotFile(sdoc)));
			serializer.read(inputStream, new BinaryOWLOntologyBuildingHandler(ontIn), manIn.getOWLDataFactory());
			return ontIn;
		} catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to load the ontology snapshot (see error log for details)", e);
		}
	}

	public OWLOntology putSnapShot(File file, ServerDocument sdoc) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		try {
			OWLOntology ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(sdoc, new SnapShot(ont));
			Response response = post(PROJECT_SNAPSHOT,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
			return retrieveOntologyFromServerResponse(ont, response);
		}
		catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(ServerDocument sdoc,
			SnapShot ontologySnapshot) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(sdoc);
		os.writeObject(ontologySnapshot);
		return b;
	}

	private OWLOntology retrieveOntologyFromServerResponse(OWLOntology ont, Response response)
			throws ClientRequestException {
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
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			outputStream = new BufferedOutputStream(new FileOutputStream(getSnapShotFile(sdoc)));
			serializer.write(new OWLOntologyWrapper(ont), new DataOutputStream(outputStream));
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

	public SnapShot getSnapShot(ProjectId projectId) throws LoginTimeoutException, AuthorizationException,
			ClientRequestException {
		String requestUrl = PROJECT_SNAPSHOT + "?projectid=" + projectId.get();
		Response response = get(requestUrl); // send request to server
		return retrieveDocumentSnapshotFromServerResponse(response);
	}

	private SnapShot retrieveDocumentSnapshotFromServerResponse(Response response)
			throws ClientRequestException {
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			SnapShot snapshot = (SnapShot) ois.readObject();
			return snapshot;
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
		try {
			HistoryFile historyFile = sdoc.getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(historyFile);
			Response response = post(ALL_CHANGES,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(HistoryFile historyFile)
			throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(historyFile);
		return b;
	}

	public DocumentRevision getRemoteHeadRevision(VersionedOWLOntology vont) throws
			LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			HistoryFile historyFile = vont.getServerDocument().getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(historyFile);
			Response response = post(HEAD,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
			return retrieveDocumentRevisionFromServerResponse(response);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private DocumentRevision retrieveDocumentRevisionFromServerResponse(Response response)
			throws ClientRequestException {
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			DocumentRevision remoteHead = (DocumentRevision) ois.readObject();
			return remoteHead;
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
		try {
			HistoryFile historyFile = sdoc.getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(start, historyFile);
			Response response = post(LATEST_CHANGES,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(DocumentRevision start,
			HistoryFile historyFile) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(historyFile);
		os.writeObject(start);
		return b;
	}

	@Override
	public List<Project> getProjects(UserId userId) {
		return new ArrayList<>(serverConfiguration.getProjects(userId));
	}

	@Override
	public List<Project> getAllProjects() {
		return new ArrayList<>(serverConfiguration.getProjects());
	}

	@Override
	public void updateProject(ProjectId projectId, Project updatedProject)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setProject(projectId, updatedProject)
				.createServerConfiguration();
		putConfig();
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
		return new ArrayList<>(serverConfiguration.getRoles(userId, projectId, globalPermissions));
	}

	@Override
	public List<Role> getAllRoles() {
		return new ArrayList<>(serverConfiguration.getRoles());
	}
	
	public Role getRole(RoleId id) throws ClientRequestException {
		try {
			return serverConfiguration.getRole(id);
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get role details (see error log for details)", e);
		}
	}

	@Override
	public void createRole(Role newRole) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.addRole(newRole)
					.createServerConfiguration();
			putConfig();
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create role (see error log for details)", e);
		}
	}

	@Override
	public void deleteRole(RoleId roleId) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.removeRole(serverConfiguration.getRole(roleId))
					.createServerConfiguration();
			putConfig();
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete role (see error log for details)", e);
		}
	}

	@Override
	public void updateRole(RoleId roleId, Role updatedRole)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setRole(roleId, updatedRole)
				.createServerConfiguration();
		putConfig();
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
		return new ArrayList<>(serverConfiguration.getOperations(userId, projectId, GlobalPermissions.INCLUDED));
	}

	@Override
	public List<Operation> getOperations(RoleId roleId) throws ClientRequestException {
		try {
			return new ArrayList<>(serverConfiguration.getOperations(serverConfiguration.getRole(roleId)));
		} catch (UnknownRoleIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get operation list (see error log for details)", e);
		}
	}

	@Override
	public List<Operation> getAllOperations() {
		return new ArrayList<>(serverConfiguration.getOperations());
	}

	@Override
	public void createOperation(Operation operation)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.addOperation(operation)
					.createServerConfiguration();
			putConfig();
		} catch (IdAlreadyInUseException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to create operation (see error log for details)", e);
		}
	}

	@Override
	public void deleteOperation(OperationId operationId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			serverConfiguration = new ConfigurationBuilder(serverConfiguration)
					.removeOperation(serverConfiguration.getOperation(operationId))
					.createServerConfiguration();
			putConfig();
		} catch (UnknownOperationIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to delete operation (see error log for details)", e);
		}
	}

	@Override
	public void updateOperation(OperationId operationId, Operation updatedOperation)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setOperation(operationId, updatedOperation)
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.addPolicy(userId, projectId, roleId)
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.removePolicy(userId, projectId, roleId)
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public Host getHost() {
		return serverConfiguration.getHost();
	}

	@Override
	public void setHostAddress(URI hostAddress) {
		Host host = ConfigurationManager.getFactory().getHost(hostAddress, Optional.empty());
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setHost(host)
				.createServerConfiguration();
	}

	@Override
	public void setSecondaryPort(int portNumber) {
		Host h = serverConfiguration.getHost();
		Port p = ConfigurationManager.getFactory().getPort(portNumber);
		Host nh = ConfigurationManager.getFactory().getHost(h.getUri(), Optional.of(p));
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setHost(nh)
				.createServerConfiguration();
	}

	@Override
	public String getRootDirectory() {
		return serverConfiguration.getServerRoot().toString();
	}

	@Override
	public void setRootDirectory(String rootDirectory)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.setServerRoot(new File(rootDirectory))
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public Map<String, String> getServerProperties() {
		return serverConfiguration.getProperties();
	}

	@Override
	public void setServerProperty(String property, String value)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.addProperty(property, value)
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public void unsetServerProperty(String property)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		serverConfiguration = new ConfigurationBuilder(serverConfiguration)
				.removeProperty(property)
				.createServerConfiguration();
		putConfig();
	}

	@Override
	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public List<Project> getProjects() {
		return getProjects(userId);
	}

	@Override
	public List<Role> getActiveRoles() {
		List<Role> activeRoles = new ArrayList<>();
		if (getRemoteProject().isPresent()) {
			activeRoles = getRoles(userId, getRemoteProject().get(), GlobalPermissions.INCLUDED);
		}
		return activeRoles;
	}

	@Override
	public List<Operation> getActiveOperations() {
		List<Operation> activeOperations = new ArrayList<>();
		if (getRemoteProject().isPresent()) {
			activeOperations = getOperations(userId, getRemoteProject().get());
		}
		return activeOperations;
	}

	private Response post(String url, RequestBody body, boolean withCredential)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		Request request = null;
		if (withCredential) {
			request = new Request.Builder()
					.url(serverAddress + url)
					.addHeader(authHeader, getAuthHeaderString())
					.post(body)
					.build();

		} else {
			request = new Request.Builder()
					.url(serverAddress + url)
					.post(body)
					.build();
		}
		try {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private Response delete(String url, boolean withCredential) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException  {
		Request request;
		if (withCredential) {
			request = new Request.Builder()
					.url(serverAddress + url)
					.addHeader(authHeader, getAuthHeaderString())
					.delete()
					.build();
		} else {
			request = new Request.Builder()
					.url(serverAddress + url)
					.delete()
					.build();
		}
		try {
			Response response = httpClient.newCall(request).execute();
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
				.addHeader(authHeader, getAuthHeaderString())
				.get()
				.build();
		try {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private String getAuthHeaderString() {
		String toenc = userId.get() + ":" + userInfo.getNonce();
		return "Basic " + new String(Base64.encodeBase64(toenc.getBytes()));
	}

	private ServerConfiguration getConfig() throws LoginTimeoutException, AuthorizationException,
			ClientRequestException {
		Response response = get(METAPROJECT);
		try {
			return ConfigurationManager.getConfigurationLoader().loadConfiguration(new InputStreamReader(response.body().byteStream()));
		} catch (ObjectConversionException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to parse the incoming server configuration data", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	public List<String> getCodes(int no) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		return retrieveCodesFromServerResponse(get(GEN_CODE + "?count=" + no));		
	}	
 
	public void putConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		if (saveCancelSemantics) {
			configStateChanged = true;
		} else {
			reallyPutConfig();
		}
	}

	public boolean configStateChanged() {
		return configStateChanged;
	}

	public void reallyPutConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		post(METAPROJECT,
				RequestBody.create(JsonContentType, serl.write(this.serverConfiguration, ServerConfiguration.class)),
				true); // send request to server
		sleep(1000); // give the server some time to reboot
		initConfig();
	}

	private static void sleep(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void putEVSHistory(String code, String name, String operation, String reference)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			EVSHistory evsHistory = new EVSHistory(code, name, operation, reference);
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(evsHistory);
			post(EVS_REC,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to send data (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(EVSHistory hist)
			throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(hist);
		return b;
	}

	private void throwRequestExceptions(Response response)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		String originalMessage = response.header("Error-Message");
		if (originalMessage == null) {
			originalMessage = "Unknown server error";
		}
		if (response.code() == StatusCodes.UNAUTHORIZED) {
			throw new AuthorizationException(originalMessage);
		}
		else if (response.code() == StatusCodes.CONFLICT) {
			throw new SynchronizationException(originalMessage);
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
		if (getClientType() == UserType.NON_ADMIN) {
			return false;
		}
		return serverConfiguration.isOperationAllowed(operationId, projectId, userId);
	}

	private boolean queryAdminPolicy(UserId userId, OperationId operationId) {
		if (getClientType() == UserType.NON_ADMIN) {
			return false;
		}
		return serverConfiguration.isOperationAllowed(operationId, userId);
	}

	private Optional<ProjectId> getRemoteProject() {
		return Optional.ofNullable(projectId);
	}
}
