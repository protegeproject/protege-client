package org.protege.editor.owl.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.api.exception.UnknownProjectIdException;
import edu.stanford.protege.metaproject.api.exception.UnknownUserIdException;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;
import edu.stanford.protege.metaproject.impl.RoleIdImpl;
import edu.stanford.protege.metaproject.impl.ServerStatus;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import io.undertow.util.StatusCodes;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.api.exception.*;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.client.util.Config;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.http.ServerProperties;
import org.protege.editor.owl.server.http.messages.History;
import org.protege.editor.owl.server.http.messages.HttpAuthResponse;
import org.protege.editor.owl.server.http.messages.LoginCreds;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.*;
import org.protege.editor.owl.ui.util.ProgressDialog;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentSerializer;
import org.semanticweb.binaryowl.owlapi.BinaryOWLOntologyBuildingHandler;
import org.semanticweb.binaryowl.owlapi.OWLOntologyWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.protege.editor.owl.server.http.ServerEndpoints.*;
import static org.protege.editor.owl.server.http.ServerProperties.*;

public class LocalHttpClient implements Client, ClientSessionListener {

	public enum UserType {NON_ADMIN, ADMIN}

	private static final Logger logger = LoggerFactory.getLogger(LocalHttpClient.class);

	private static final MediaType JsonContentType = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType ApplicationContentType = MediaType.parse("application");

	private static final String authHeader = "Authorization";

	private static final String SNAPSHOT_CHECKSUM = "-checksum";

	private final String serverAddress;

	private final OkHttpClient httpClient;

	private UserId userId;
	private UserInfo userInfo;

	private AuthToken authToken;

	//private ServerConfiguration serverConfiguration;
	private Config config = null;

	public Config getConfig() {
		return config;
	}

	private Serializer serl = new DefaultJsonSerializer();

	private static LocalHttpClient currentHttpClient;

	/**
	 * The constructor
	 */
	public LocalHttpClient(String username, String password, String serverAddress)
		throws LoginTimeoutException, AuthorizationException, ClientRequestException {
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
		return config.getCurrentConfig();
	}

	public UserType getClientType() {
		int adminPort = config.getHost().getSecondaryPort().get().get();
		int serverAddressPort = URI.create(serverAddress).getPort();
		return (adminPort == serverAddressPort) ? UserType.ADMIN : UserType.NON_ADMIN;
	}

	public void initConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		ServerConfiguration serverConfiguration = getServerConfig();
		config = new Config(serverConfiguration, userId);
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
			User user = config.getCurrentConfig().getUser(userId);
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
		logger.info("ClientSessionChangeEvent: " + event);
	}


	@Override
	public AuthToken getAuthToken() {
		return authToken;
	}

	public ServerDocument createProject(Project project, File font)
		throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			ServerDocument sdoc = postProjectToServer(project);
			postProjectSnapShotToServer(project, font); // send snapshot to server
			initConfig();
			return sdoc;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ServerDocument postProjectToServer(Project project) throws IOException,
		LoginTimeoutException, AuthorizationException, ClientRequestException {
		ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(project);
		Response response = post(PROJECT,
			RequestBody.create(ApplicationContentType, b.toByteArray()),
			true); // send the request to server
		return retrieveServerDocumentFromServerResponse(response);
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
	public void deleteProject(@Nonnull ProjectId projectId, boolean includeFile)
		throws AuthorizationException, LoginTimeoutException, ClientRequestException {
		String requestUrl = PROJECT + "?projectid=" + projectId.get();
		delete(requestUrl, true); // send request to server
		initConfig();
	}

	@Override
	public ServerDocument openProject(@Nonnull ProjectId projectId)
		throws AuthorizationException, LoginTimeoutException, ClientRequestException {
		if (getClientType() == UserType.ADMIN) { // admin clients cannot edit/browse ontologies
			throw new ClientRequestException("Admin clients cannot open projects");
		}
		String requestUrl = PROJECT + "?projectid=" + projectId.get();
		Response response = get(requestUrl); // send request to server
		return retrieveServerDocumentFromServerResponse(response);
	}

	@Override
	public ChangeHistory commit(@Nonnull ProjectId projectId, CommitBundle commitBundle)
		throws AuthorizationException, ClientRequestException {
		checkSnapshotChecksumPresent(projectId);
		try {
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(commitBundle);
			Response response = postWithProjectId(COMMIT,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				projectId,
					true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private void checkSnapshotChecksumPresent(@Nonnull ProjectId projectId) {
		if (!getSnapshotChecksum(projectId).isPresent()) {
			throw new IllegalArgumentException("Missing snapshot checksum for project " + projectId);
		}
	}

	@Override
	public List<Project> classifiableProjects() {
		try {
			Response response = get(PROJECTS_UNCLASSIFIED);
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			List<Project> projects = (List<Project>) ois.readObject();
			return projects;
		} catch (AuthorizationException e) {
			throw new RuntimeException(e);
		} catch (ClientRequestException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(
			CommitBundle commitBundle) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
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
													   @Nonnull ProjectId pid)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		if (pid == null) throw new IllegalArgumentException("projectId is null");
		setCurrentProject(pid);
		if (!getSnapShotFile(pid).get().exists()) {
			SnapShot snapshot = getSnapShot(pid);
			createLocalSnapShot(snapshot.getOntology(), pid);
		}
		OWLOntology targetOntology = loadSnapShot(owlManager, pid);
		ChangeHistory remoteChangeHistory = getLatestChanges(sdoc, DocumentRevision.START_REVISION, pid);
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}

	private void setCurrentProject(@Nonnull ProjectId pid) throws ClientRequestException {
		if (pid == null) throw new IllegalArgumentException("projectId is null");
		try {
			config.setActiveProject(pid);
			config.getCurrentConfig().getProject(pid);
		} catch (UnknownProjectIdException e) {
			logger.error(e.getMessage());
			throw new ClientRequestException("Client failed to get the project (see error log for details)", e);
		}
	}

	private static Optional<File> getSnapShotFile(@Nonnull ProjectId projectId) {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		try {
			Files.createDirectories(Paths.get(projectId.get()));
		} catch (IOException e) {
			logger.error("Unable to create snapshot directory for " + projectId + ": " + e);
			return Optional.empty();
		}
		return Optional.of(new File(projectId.get() + File.separator + "history-snapshot"));
	}

	private static Optional<String> getSnapshotChecksum(@Nonnull ProjectId projectId) {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		Path path = Paths.get(getSnapShotFile(projectId).get().getAbsolutePath() + SNAPSHOT_CHECKSUM);
		try {
			return Optional.of(new String(Files.readAllBytes(path), Charset.defaultCharset()));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public OWLOntology loadSnapShot(OWLOntologyManager manIn, @Nonnull ProjectId pid) throws ClientRequestException {
		if (pid == null) throw new IllegalArgumentException("projectId is null");
		try {
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			OWLOntology ontIn = manIn.createOntology();
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(getSnapShotFile(pid).get()));
			serializer.read(inputStream, new BinaryOWLOntologyBuildingHandler(ontIn), manIn.getOWLDataFactory());
			return ontIn;
		} catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to load the ontology snapshot (see error log for details)", e);
		}
	}

	private void postProjectSnapShotToServer(Project project, File font) throws LoginTimeoutException,
		AuthorizationException, ClientRequestException {
		Response response = null;
		try {
			OWLOntology ont;
			if (font.getName().endsWith(".zip")) {
				ZipInputStream zi = new ZipInputStream(new FileInputStream(font));
				ont = OWLManager.createConcurrentOWLOntologyManager().loadOntologyFromOntologyDocument(zi);
			} else {
				ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(font);
			}
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(project.getId(), new SnapShot(ont));

			response = post(PROJECT_SNAPSHOT,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				true); // send request to server

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			String snapshotChecksum = (String) ois.readObject();
			writeSnapshotChecksum(project.getId(), snapshotChecksum);
		} catch (IOException | OWLOntologyCreationException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(ProjectId projectId, SnapShot ontologySnapshot)
		throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(projectId);
		os.writeObject(ontologySnapshot);
		return b;
	}

	public void createLocalSnapShot(OWLOntology ont, @Nonnull ProjectId projectId) throws ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		BufferedOutputStream outputStream = null;
		try {
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			outputStream = new BufferedOutputStream(new FileOutputStream(getSnapShotFile(projectId).get()));
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

	public SnapShot getSnapShot(@Nonnull ProjectId projectId) throws LoginTimeoutException, AuthorizationException,
		ClientRequestException {
		String requestUrl = PROJECT_SNAPSHOT + "?projectid=" + projectId.get();
		Response response = get(requestUrl); // send request to server
		return retrieveDocumentSnapshotFromServerResponse(response, projectId);
	}

	private SnapShot retrieveDocumentSnapshotFromServerResponse(Response response, @Nonnull ProjectId projectId)
		throws ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		try {
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			SnapShot snapshot = (SnapShot) ois.readObject();
			String checksum = (String) ois.readObject();
			writeSnapshotChecksum(projectId, checksum);
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

	public ChangeHistory getAllChanges(ServerDocument sdoc, @Nonnull ProjectId projectId) throws LoginTimeoutException,
		AuthorizationException, ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		try {
			HistoryFile historyFile = sdoc.getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(historyFile);
			Response response = postWithProjectId(ALL_CHANGES,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				projectId,
				true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		} catch (IOException e) {
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

	public DocumentRevision getRemoteHeadRevision(VersionedOWLOntology vont, @Nonnull ProjectId projectId) throws
		AuthorizationException, ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		try {
			HistoryFile historyFile = vont.getServerDocument().getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(historyFile);
			Response response = postWithProjectId(HEAD,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				projectId,
				true); // send request to server
			return retrieveDocumentRevisionFromServerResponse(response);
		} catch (IOException e) {
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

	public ChangeHistory getLatestChanges(VersionedOWLOntology vont, @Nonnull ProjectId projectId)
			throws AuthorizationException, ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		DocumentRevision start = vont.getChangeHistory().getHeadRevision();
		return getLatestChanges(vont.getServerDocument(), start, projectId);
	}

	public ChangeHistory getLatestChanges(ServerDocument sdoc, DocumentRevision start, @Nonnull ProjectId projectId)
		throws AuthorizationException, ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		try {
			HistoryFile historyFile = sdoc.getHistoryFile();
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(start, historyFile);
			Response response = postWithProjectId(LATEST_CHANGES,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				projectId,
				true); // send request to server
			return retrieveChangeHistoryFromServerResponse(response);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(
			DocumentRevision start, HistoryFile historyFile) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(historyFile);
		os.writeObject(start);
		return b;
	}

	public void squashHistory(SnapShot snapshot, @Nonnull ProjectId projectId) throws ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		checkSnapshotChecksumPresent(projectId);
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(b);
			oos.writeObject(snapshot);

			Response response = postWithProjectId(SQUASH,
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				projectId,
				true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			String snapshotChecksum = (String) ois.readObject();
			writeSnapshotChecksum(projectId, snapshotChecksum);

			createLocalSnapShot(snapshot.getOntology(), projectId);
		} catch (IOException | AuthorizationException | ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	public Role getRole(RoleId id) throws ClientRequestException {
		return config.getRole(id);
	}


	@Override
	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public List<Project> getProjects() {
		return config.getProjects(userId);
	}

	@Override
	public List<Role> getActiveRoles(ProjectId projectId) {
		List<Role> activeRoles = new ArrayList<>();
		if (projectId != null) {
			activeRoles = config.getRoles(userId, projectId, GlobalPermissions.INCLUDED);
		}
		return activeRoles;
	}

	@Override
	public List<Operation> getActiveOperations(ProjectId projectId) {
		List<Operation> activeOperations = new ArrayList<>();
		if (projectId != null) {
			activeOperations = config.getOperations(userId, projectId);
		}
		return activeOperations;
	}

	private Request.Builder postBuilder(String url, RequestBody body, boolean withCredential) {
		Request.Builder builder = new Request.Builder()
			.url(serverAddress + url)
			.post(body);

		if (withCredential) {
			builder = builder.addHeader(authHeader, getAuthHeaderString());
		}
		return builder;
	}

	private Response postWithProjectId(String url, RequestBody body, @Nonnull ProjectId projectId, boolean withCredential)
			throws AuthorizationException, ClientRequestException {
		if (projectId == null) {
			throw new RuntimeException("POST projectId is null: " + url);
		}
		Optional<String> snapshotChecksum = getSnapshotChecksum(projectId);
		if (!snapshotChecksum.isPresent()) {
			throw new RuntimeException("POST snapshot checksum is missing");
		}

		Request.Builder builder = postBuilder(url, body, withCredential);

		builder.addHeader(ServerProperties.PROJECTID_HEADER, projectId.get());
		builder.addHeader(ServerProperties.SNAPSHOT_CHECKSUM_HEADER, snapshotChecksum.get());

		try {
			Response response = httpClient.newCall(builder.build()).execute();

			if (!response.isSuccessful() && response.code() == ServerProperties.HISTORY_SNAPSHOT_OUT_OF_DATE) {
				ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
				ProgressDialog dlg = new ProgressDialog();

				dlg.setMessage("History snapshot out of date. Fetching latest.");
				final ListenableFuture<?> snapshotTask = service.submit(() -> {
					try {
						SnapShot snapshot = getSnapShot(projectId);
						createLocalSnapShot(snapshot.getOntology(), projectId);
					} catch (AuthorizationException | ClientRequestException e) {
						throw new RuntimeException(e);
					}
					finally {
						dlg.setVisible(false);
					}
				});
				dlg.setVisible(true);

				String newChecksum = getSnapshotChecksum(projectId).get();

				builder = postBuilder(url, body, withCredential)
						.addHeader(ServerProperties.PROJECTID_HEADER, projectId.get())
						.addHeader(ServerProperties.SNAPSHOT_CHECKSUM_HEADER, newChecksum);

				response = httpClient.newCall(builder.build()).execute();
			}

			if (!response.isSuccessful()) {
				throwRequestExceptions(response);
			}
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private Response post(String url, RequestBody body, boolean withCredential)
		throws AuthorizationException, ClientRequestException {
		Request.Builder builder = postBuilder(url, body, withCredential);

		try {
			Response response = httpClient.newCall(builder.build()).execute();

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
		AuthorizationException, ClientRequestException {
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

	private ServerConfiguration getServerConfig() throws LoginTimeoutException, AuthorizationException,
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

	public List<String> getCodes(int no, @Nonnull ProjectId projectId) throws
		LoginTimeoutException, AuthorizationException, ClientRequestException {
		return retrieveCodesFromServerResponse(get(GEN_CODE + "?count=" + no + "&projectid=" + projectId.get()));
	}


	public void saveConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		post(METAPROJECT,
			RequestBody.create(JsonContentType, serl.write(config.getCurrentConfig(), ServerConfiguration.class)),
			true); // send request to server
		sleep(1000); // give the server some time to reboot
		initConfig();
	}

	public void pauseServer() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		get(SERVER_PAUSE);

	}

	public void resumeServer() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		get(SERVER_RESUME);

	}

	private static void sleep(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void putEVSHistory(String code, String name, String operation, String reference, @Nonnull ProjectId projectId)
		throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			History evsHistory = new History(userId.get(), code, name, operation, reference);
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(evsHistory);
			post(EVS_REC + "?projectid=" + projectId.get(),
				RequestBody.create(ApplicationContentType, b.toByteArray()),
				true); // send request to server
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to send data (see error log for details)", e);
		}
	}

	public void genConceptHistory(@Nonnull ProjectId projectId)
		throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		if (projectId == null) throw new IllegalArgumentException("projectId cannot be null");
		try {
			get(GEN_CON_HIST + "?projectid=" + projectId.get());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to successfuly generate concept history.", e);
		}
	}

	private void writeSnapshotChecksum(@Nonnull ProjectId projectId, String checksum) throws IOException {
		if (projectId == null) throw new IllegalArgumentException("projectId is null");
		File snapshotFile = getSnapShotFile(projectId).get();
		OutputStream checksumStream = new FileOutputStream(snapshotFile.getAbsolutePath() + SNAPSHOT_CHECKSUM);
		checksumStream.write(checksum.getBytes());
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(History hist)
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
			originalMessage = String.format("Unknown server error (code: %d)", response.code());
		}
		if (response.code() == StatusCodes.UNAUTHORIZED) {
			throw new AuthorizationException(originalMessage);
		} else if (response.code() == StatusCodes.CONFLICT) {
			throw new SynchronizationException(originalMessage);
		}
		/*
		 * 440 Login Timeout. Reference: https://support.microsoft.com/en-us/kb/941201
		 */
		else if (response.code() == 440) {
			throw new LoginTimeoutException(originalMessage);
		} else if (response.code() == StatusCodes.SERVICE_UNAVAILABLE) {
			throw new ServiceUnavailableException(originalMessage);
		} else {
			String msg = String.format("%s (contact server admin for further assistance)", originalMessage);
			throw new ClientRequestException(msg);
		}
	}

	public boolean codeIsLessThan(String lower, String upper) {

		String p = config.getCurrentConfig().getProperty(CODEGEN_PREFIX);
		String s = config.getCurrentConfig().getProperty(CODEGEN_SUFFIX);
		String d = config.getCurrentConfig().getProperty(CODEGEN_DELIMETER);

		int lowNum = 0;
		int upNum = 0;

		if (d != null) {
			String[] lowSplit = lower.split(d);
			String[] upSplit = upper.split(d);
			lowNum = Integer.parseInt(lowSplit[1]);
			upNum = Integer.parseInt(upSplit[1]);

		} else {
			String lowRem = lower.substring(p.length());
			String upRem = upper.substring(p.length());
			if (s != null) {
				lowNum = Integer.parseInt(lowRem.substring(0, lowRem.length() - s.length()));
				upNum = Integer.parseInt(upRem.substring(0, upRem.length() - s.length()));
			} else {
				lowNum = Integer.parseInt(lowRem);
				upNum = Integer.parseInt(upRem);

			}
		}
		return lowNum < upNum;


	}

	public boolean isWorkFlowManager(ProjectId projectId) {
		try {
			Role wfm = getRole(new RoleIdImpl("mp-project-manager"));
			return getActiveRoles(projectId).contains(wfm);
		} catch (ClientRequestException e) {
			e.printStackTrace();
			return false;
		}
	}

	public ServerStatus getServerStatus() throws ClientRequestException {
		Response response = null;
		try {
			response = get(SERVER_STATUS);
			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			ServerStatus serverStatus = (ServerStatus) ois.readObject();
			return serverStatus;
		} catch (IOException | ClassNotFoundException | AuthorizationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to read data from server (see error log for details)", e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}
}
