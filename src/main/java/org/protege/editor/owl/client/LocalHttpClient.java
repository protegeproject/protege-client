package org.protege.editor.owl.client;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.api.exception.UnknownProjectIdException;
import edu.stanford.protege.metaproject.api.exception.UnknownUserIdException;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;
import edu.stanford.protege.metaproject.impl.RoleIdImpl;
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
import org.protege.editor.owl.client.util.Config;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.http.messages.History;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import static org.protege.editor.owl.server.http.ServerEndpoints.*;
import static org.protege.editor.owl.server.http.ServerProperties.*;

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

	//private ServerConfiguration serverConfiguration;
	private Config config = null;
	
	public Config getConfig() { return config; }
	
	private Serializer serl = new DefaultJsonSerializer();

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
		if (event.equals(EventCategory.SWITCH_ONTOLOGY)) {
			projectId = event.getSource().getActiveProject();
		}
	}

	

	public Project getCurrentProject() {
		return project;
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
			config.setActiveProject(pid);
			project = config.getCurrentConfig().getProject(pid);
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

	private void postProjectSnapShotToServer(Project project, File font) throws LoginTimeoutException,
			AuthorizationException, ClientRequestException {
		try {
			OWLOntology ont;
			if (font.getName().endsWith(".zip")) {
				ZipInputStream zi = new ZipInputStream(new FileInputStream(font));
				ont = OWLManager.createConcurrentOWLOntologyManager().loadOntologyFromOntologyDocument(zi);
			} else {
				ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(font);
			}
			ByteArrayOutputStream b = writeRequestArgumentsIntoByteStream(project.getId(), new SnapShot(ont));
			post(PROJECT_SNAPSHOT,
					RequestBody.create(ApplicationContentType, b.toByteArray()),
					true); // send request to server
		}
		catch (IOException | OWLOntologyCreationException e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Unable to send request to server (see error log for details)", e);
		}
	}

	private ByteArrayOutputStream writeRequestArgumentsIntoByteStream(ProjectId projectId,
			SnapShot ontologySnapshot) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(b);
		os.writeObject(projectId);
		os.writeObject(ontologySnapshot);
		return b;
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
	public List<Role> getActiveRoles() {
		List<Role> activeRoles = new ArrayList<>();
		if (getRemoteProject().isPresent()) {
			activeRoles = config.getRoles(userId, getRemoteProject().get(), GlobalPermissions.INCLUDED);
		}
		return activeRoles;
	}

	@Override
	public List<Operation> getActiveOperations() {
		List<Operation> activeOperations = new ArrayList<>();
		if (getRemoteProject().isPresent()) {
			activeOperations = config.getOperations(userId, getRemoteProject().get());
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

	public List<String> getCodes(int no) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
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

	public void putEVSHistory(String code, String name, String operation, String reference)
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
	
	public void genConceptHistory()
			throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		try {
			
			get(GEN_CON_HIST + "?projectid=" + projectId.get());
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ClientRequestException("Failed to successfuly generate concept history.", e);
		}
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
		else if (response.code() == StatusCodes.SERVICE_UNAVAILABLE) {
			throw new ClientRequestException(originalMessage);
		}
		else {
			String msg = String.format("%s (contact server admin for further assistance)", originalMessage);
			throw new ClientRequestException(msg);
		}
	}


	

	

	public Optional<ProjectId> getRemoteProject() {
		return Optional.ofNullable(projectId);
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
	
	public boolean isWorkFlowManager() { 
    	
    		try {
    			Role wfm = getRole(new RoleIdImpl("mp-project-manager"));
    			return getActiveRoles().contains(wfm);
    		} catch (ClientRequestException e) {
    			e.printStackTrace();
    			return false;
    		}
    	
    }
}
