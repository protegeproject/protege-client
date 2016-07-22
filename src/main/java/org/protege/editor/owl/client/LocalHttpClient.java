package org.protege.editor.owl.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.exception.ServerException;
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

import com.google.gson.Gson;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.AuthenticationRegistry;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.MetaprojectAgent;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.OperationRegistry;
import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.Policy;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.ProjectRegistry;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.RoleRegistry;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.Serializer;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.UserRegistry;
import edu.stanford.protege.metaproject.api.exception.IdAlreadyInUseException;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.api.exception.ProjectNotInPolicyException;
import edu.stanford.protege.metaproject.api.exception.UnknownMetaprojectObjectIdException;
import edu.stanford.protege.metaproject.api.exception.UserNotInPolicyException;
import edu.stanford.protege.metaproject.api.exception.UserNotRegisteredException;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;
import edu.stanford.protege.metaproject.impl.Operations;
import edu.stanford.protege.metaproject.impl.UserIdImpl;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import io.undertow.util.StatusCodes;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalHttpClient implements Client, ClientSessionListener {

	//private AuthToken authToken;
	private String serverAddress;

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
		
		req_client = new OkHttpClient.Builder().readTimeout(360, TimeUnit.SECONDS).build();
				
		this.serverAddress = serverAddress;
		this.userInfo = login(user, pwd);
		this.userId = new UserIdImpl(user);
		String toenc = this.userId.get() + ":" + userInfo.getNonce();
		this.auth_header_value = "Basic " + new String(Base64.encodeBase64(toenc.getBytes()));
		LocalHttpClient.current_user = this;
		//check if user is allowed to edit config
		initConfig();
	}
	
	public void initConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		config = getConfig();
		proj_registry = config.getMetaproject().getProjectRegistry();
		user_registry = config.getMetaproject().getUserRegistry();
		auth_registry = config.getAuthenticationRegistry();
		meta_agent = config.getMetaproject().getMetaprojectAgent();
		role_registry = config.getMetaproject().getRoleRegistry();
		op_registry = config.getMetaproject().getOperationRegistry();
		policy = config.getMetaproject().getPolicy();
		config_state_changed = false;
		
	}
	
	
	private UserInfo login(String user, String pwd) throws Exception {

		final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
		String url = HTTPServer.LOGIN;

		LoginCreds creds = new LoginCreds(user, pwd);

		HttpAuthResponse resp = null;

		Serializer<Gson> serl = new DefaultJsonSerializer();
		RequestBody body = RequestBody.create(JSON, serl.write(creds, LoginCreds.class));

		Response response = post(url, body, false);

		if (response.code() == StatusCodes.UNAUTHORIZED) {
			String error = (String) serl.parse(new InputStreamReader(response.body().byteStream()), String.class);
			throw new Exception(error);
		} else if (response.code() == StatusCodes.INTERNAL_SERVER_ERROR) {
			throw new Exception("Internal server error");
			
		} else {
			resp = (HttpAuthResponse) serl.parse(new InputStreamReader(response.body().byteStream()), HttpAuthResponse.class);
			response.body().close();
			return new UserInfo(resp.getId(), resp.getName(), resp.getEmail(), resp.getToken());
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
		if (auth_token != null) {
			
		} else {
			User user = null;
			try {
				user = user_registry.get(userId);
			} catch (UnknownMetaprojectObjectIdException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			auth_token = new AuthorizedUserToken(user);
		}
		return auth_token;
	}

	@Override
	public List<User> getAllUsers() throws AuthorizationException, ClientRequestException, RemoteException {
		return new ArrayList<User>(user_registry.getEntries());
	}

	@Override
	public void createUser(User newUser, Optional<? extends Password> password)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
			meta_agent.add(newUser);
			if (password.isPresent()) {
				Password newpassword = password.get();
                if (newpassword instanceof SaltedPasswordDigest) {
                    auth_registry.add(newUser.getId(), (SaltedPasswordDigest) newpassword);
                }
			}
			putConfig();
		}
		catch (IdAlreadyInUseException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            meta_agent.remove(user_registry.get(userId));
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public void updateUser(UserId userId, User updatedUser, Optional<? extends Password> updatedPassword)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            user_registry.update(userId, updatedUser);
            if (updatedPassword.isPresent()) {
                Password password = updatedPassword.get();
                if (password instanceof SaltedPasswordDigest) {
                    auth_registry.changePassword(userId, (SaltedPasswordDigest) password);
                }
            }
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException | UserNotRegisteredException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}
	
	
	public ServerDocument createProject(Project proj)
			throws LoginTimeoutException, AuthorizationException, ClientRequestException, RemoteException {

		ProjectId projectId = proj.getId();
		Name projectName = proj.getName();
		Description description = proj.getDescription();
		UserId owner = proj.getOwner();
		Optional<ProjectOptions> options = proj.getOptions();

		String url = HTTPServer.PROJECT;

		Response response;
		ServerDocument sdoc = null;

		try {

			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(projectId);
			os.writeObject(projectName);
			os.writeObject(description);
			os.writeObject(owner);
			ProjectOptions popts = (options.isPresent()) ? options.get() : null;
			os.writeObject(popts);

			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			sdoc = (ServerDocument) ois.readObject();
			
			response.body().close();

			// send snapshot to server
			OWLOntology ont = putSnapShot(proj.getFile(), sdoc);

			initConfig();
			return sdoc;

		} catch (IOException | ClassNotFoundException e) {
			throw new ClientRequestException("Data transmission error", e);
		}

	}
	

	@Override
	public void deleteProject(ProjectId projectId, boolean includeFile)
			throws AuthorizationException, ClientRequestException, RemoteException {
		String url = HTTPServer.PROJECT + "?projectid=" + projectId.get();

		try {
			delete(url, true);
			initConfig();
		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		} 
	}

	@Override
	public ServerDocument openProject(ProjectId projectId)
			throws AuthorizationException, ClientRequestException, RemoteException {

		String url = HTTPServer.PROJECT + "?projectid=" + projectId.get();


		okhttp3.Response response;
		ServerDocument sdoc = null;
		try {
			response = get(url);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			sdoc = (ServerDocument) ois.readObject();
			
			response.body().close();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}   
		return sdoc;

	}
	
	@Override
	public ChangeHistory commit(ProjectId projectId, CommitBundle commitBundle)
			throws AuthorizationException, OutOfSyncException, ClientRequestException, RemoteException {

		String url = HTTPServer.COMMIT;

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = null;
		Response response = null;
		ChangeHistory hist = null;
		try {
			os = new ObjectOutputStream(b);
			os.writeObject(projectId);
			os.writeObject(commitBundle);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());

			if (response.code() == 200) {
				hist = (ChangeHistory) ois.readObject();

			} else if (response.code() == 500) {
				ServerException ex = (ServerException) ois.readObject();
				if (ex.getCause() instanceof ServerServiceException) {
					ServerServiceException iex = (ServerServiceException) ex.getCause();
					if (iex.getCause() instanceof OutOfSyncException) {
						throw (OutOfSyncException) iex.getCause();
					} else if (iex.getCause() instanceof AuthorizationException) {
						throw (AuthorizationException) iex.getCause();						
					} else {
						throw new ClientRequestException(iex.getMessage(), iex);						
					}
				} else {
					throw new ClientRequestException(ex.getMessage(), ex);
				}
			}




		} catch (IOException | ClassNotFoundException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		} finally {
			response.body().close();
		}


		return hist;
	}


	public VersionedOWLOntology buildVersionedOntology(ServerDocument sdoc, OWLOntologyManager owlManager,
			ProjectId pid)
			throws ClientRequestException, OWLOntologyCreationException {
		
		projectId = pid;
		try {
			project = proj_registry.get(pid);
		} catch (UnknownMetaprojectObjectIdException e) {
			// TODO Log this
			e.printStackTrace();
		}
		
		OWLOntology targetOntology = null;
		ChangeHistory remoteChangeHistory = null;
		
		if (snapShotExists(sdoc)) {			
			
		} else {
			getSnapShot(sdoc);					
		}
		
		targetOntology = loadSnapShot(owlManager, sdoc);		
		
		remoteChangeHistory = getLatestChanges(sdoc, DocumentRevision.START_REVISION);		
		
		
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}
	
	public boolean snapShotExists(ServerDocument sdoc) {
		String fileName = sdoc.getHistoryFile().getName() + "-snapshot";
		return (new File(fileName)).exists();	
	}
	
	public OWLOntology loadSnapShot(OWLOntologyManager manIn, ServerDocument sdoc) {
		try {
			long beg = System.currentTimeMillis();
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
	        OWLOntology ontIn = manIn.createOntology();
	        String fileName = sdoc.getHistoryFile().getName() + "-snapshot";
	        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(new File(fileName)));
	        serializer.read(inputStream, new BinaryOWLOntologyBuildingHandler(ontIn), manIn.getOWLDataFactory());
	        System.out.println("Time to serialize in " + (System.currentTimeMillis() - beg)/1000);
	        return ontIn;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public OWLOntology putSnapShot(File file, ServerDocument sdoc) {
		OWLOntology ont = null;
		try {
			ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
			
			
			String url = HTTPServer.PROJECT_SNAPSHOT;

			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = null;
			Response response = null;
			
			try {
				os = new ObjectOutputStream(b);
				os.writeObject(sdoc);
				os.writeObject(new SnapShot(ont));
				RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

				response = post(url, req, true);


				ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
				ServerDocument new_sdoc = (ServerDocument) ois.readObject();
				System.out.println("got it");

			} catch (IOException | ClassNotFoundException e) {
				throw new ClientRequestException(e.getMessage(), e.getCause());
			} finally {
				response.body().close();
			}

			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ont;
	}
	
	public void createLocalSnapShot(OWLOntology ont, ServerDocument sdoc) {
		try {
			
			long beg = System.currentTimeMillis();
			BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
			BufferedOutputStream outputStream = null;
			
			String fileName = sdoc.getHistoryFile().getName() + "-snapshot";

			outputStream = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			serializer.write(new OWLOntologyWrapper(ont), new DataOutputStream(outputStream));
			outputStream.close();
			System.out.println("Time to serialize out snapshot " + (System.currentTimeMillis() - beg)/1000);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}     

	}
	
	public void getSnapShot(ServerDocument sdoc) {
		try {
			String url = HTTPServer.PROJECT_SNAPSHOT_GET;
			
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = null;
			
			
			os = new ObjectOutputStream(b);
			os.writeObject(sdoc);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response resp = post(url, req, true);
			
			ObjectInputStream ois = new ObjectInputStream(resp.body().byteStream());
			
			SnapShot shot = (SnapShot) ois.readObject();
			createLocalSnapShot(shot.getOntology(), sdoc);
			
			resp.body().close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

	public ChangeHistory getAllChanges(ServerDocument sdoc) throws ClientRequestException {
		ChangeHistory history = null;
		try {
			long beg = System.currentTimeMillis();


			// TODO: get all changes
			String url = HTTPServer.ALL_CHANGES;
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(sdoc.getHistoryFile());
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			
			

			history = (ChangeHistory) ois.readObject();
			
			System.out.println("Time to execute get all changes " + (System.currentTimeMillis() - beg)/1000);
			
			response.body().close();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}  
		return history;

	}
	
	public DocumentRevision getRemoteHeadRevision(VersionedOWLOntology vont) throws
		ClientRequestException {	
		DocumentRevision remoteHead = null;
		try {
			long beg = System.currentTimeMillis();


			// TODO: get all changes
			String url = HTTPServer.HEAD;
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(vont.getServerDocument().getHistoryFile());
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			
			

			remoteHead = (DocumentRevision) ois.readObject();
			
			System.out.println("Time to execute get all changes " + (System.currentTimeMillis() - beg)/1000);
			
			response.body().close();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}  
		return remoteHead;
	}
	
	public ChangeHistory getLatestChanges(VersionedOWLOntology vont) throws
		ClientRequestException {
		DocumentRevision start = vont.getChangeHistory().getHeadRevision();
		return getLatestChanges(vont.getServerDocument(), start);
	}
	
	public ChangeHistory getLatestChanges(ServerDocument sdoc, DocumentRevision start)
		throws ClientRequestException {
		ChangeHistory history = null;
		try {


			// TODO: get all changes
			String url = HTTPServer.LATEST_CHANGES;
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(sdoc.getHistoryFile());
			
			os.writeObject(start);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());

			history = (ChangeHistory) ois.readObject();
			
			response.body().close();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}  
		return history;

	}
	
	@Override
	public List<Project> getProjects(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {

		try {
			return new ArrayList<>(meta_agent.getProjects(userId));
		} catch (UserNotInPolicyException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}


	}

	@Override
	public List<Project> getAllProjects() throws AuthorizationException, ClientRequestException, RemoteException {		
		return new ArrayList<>(proj_registry.getEntries());
	}



	@Override
	public void updateProject(ProjectId projectId, Project updatedProject)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            proj_registry.update(projectId, updatedProject);
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public Map<ProjectId, List<Role>> getRoles(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		Map<ProjectId, List<Role>> roleMap = new HashMap<>();
        for (Project project : getAllProjects()) {
            roleMap.put(project.getId(), getRoles(userId, project.getId()));
        }
        return roleMap;
	}

	@Override
	public List<Role> getRoles(UserId userId, ProjectId projectId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            return new ArrayList<>(meta_agent.getRoles(userId, projectId));
        }
        catch (UserNotInPolicyException | ProjectNotInPolicyException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public List<Role> getAllRoles() throws AuthorizationException, ClientRequestException, RemoteException {
		return new ArrayList<Role>(this.role_registry.getEntries());
	}

	@Override
	public void createRole(Role newRole) throws AuthorizationException, ClientRequestException, RemoteException {
		 try {
			meta_agent.add(newRole);
			putConfig();
		} catch (IdAlreadyInUseException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
		try {
			meta_agent.remove(role_registry.get(roleId));
			putConfig();
		} catch (UnknownMetaprojectObjectIdException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void updateRole(RoleId roleId, Role updatedRole)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            role_registry.update(roleId, updatedRole);
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public Map<ProjectId, List<Operation>> getOperations(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		Map<ProjectId, List<Operation>> operationMap = new HashMap<>();
        for (Project project : getAllProjects()) {
            operationMap.put(project.getId(), getOperations(userId, project.getId()));
        }
        return operationMap;
		
	}

	@Override
	public List<Operation> getOperations(UserId userId, ProjectId projectId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            return new ArrayList<>(meta_agent.getOperations(userId, projectId));
        }
        catch (UserNotInPolicyException | ProjectNotInPolicyException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}
	
	@Override
	public List<Operation> getOperations(RoleId roleId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            return new ArrayList<>(meta_agent.getOperations(role_registry.get(roleId)));
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}	


	@Override
	public List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException, RemoteException {
		return new ArrayList<>(op_registry.getEntries());
	}

	@Override
	public void createOperation(Operation operation)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            meta_agent.add(operation);
            putConfig();
        }
        catch (IdAlreadyInUseException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public void deleteOperation(OperationId operationId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            meta_agent.remove(op_registry.get(operationId));
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
		
	}

	@Override
	public void updateOperation(OperationId operationId, Operation updatedOperation)
			throws AuthorizationException, ClientRequestException, RemoteException {
		try {
            op_registry.update(operationId, updatedOperation);
            putConfig();
        }
        catch (UnknownMetaprojectObjectIdException e) {
        	throw new ClientRequestException(e.getMessage(), e.getCause());
        }
	}

	@Override
	public void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		policy.add(roleId, projectId, userId);
        putConfig();

	}

	@Override
	public void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		policy.remove(userId, projectId, roleId);
        putConfig();

	}

	@Override
	public Host getHost() throws AuthorizationException, ClientRequestException, RemoteException {
		return config.getHost();
	}

	@Override
	public void setHostAddress(URI hostAddress) throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSecondaryPort(int portNumber)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRootDirectory() throws AuthorizationException, ClientRequestException, RemoteException {
		return config.getServerRoot().toString();
	}

	@Override
	public void setRootDirectory(String rootDirectory)
			throws AuthorizationException, ClientRequestException, RemoteException {
		config.setServerRoot(new File(rootDirectory));
        putConfig();

	}

	@Override
	public Map<String, String> getServerProperties()
			throws AuthorizationException, ClientRequestException, RemoteException {
		return config.getProperties();
	}

	@Override
	public void setServerProperty(String property, String value)
			throws AuthorizationException, ClientRequestException, RemoteException {
		config.addProperty(property, value);
		putConfig();

	}

	@Override
	public void unsetServerProperty(String property)
			throws AuthorizationException, ClientRequestException, RemoteException {
		config.removeProperty(property);
		putConfig();

	}
	
	public Role getRole(RoleId id) {
		try {
			return role_registry.get(id);
		} catch (UnknownMetaprojectObjectIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	


	

	@Override
	public UserInfo getUserInfo() {
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
		
            List<Operation> activeOperations = new ArrayList<>();
            if (getRemoteProject().isPresent()) {
                
					try {
						activeOperations = getOperations(userId, getRemoteProject().get());
					} catch (AuthorizationException | RemoteException e) {
						throw new ClientRequestException(e.getCause());						
					}
				
            }
            return activeOperations;
        
		
	}






	

	private Response post(String url, RequestBody body, boolean cred) {
		Request request;
		
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
			return req_client.newCall(request).execute();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private Response delete(String url, boolean cred) {
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
			return req_client.newCall(request).execute();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Response get(String url) throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		Request request = new Request.Builder()
				.url(serverAddress + url)
				.addHeader(AUTH_HEADER, auth_header_value)
				.get()
				.build();
		try {
			Response response = req_client.newCall(request).execute();
			if (!response.isSuccessful()) {
				String msg = response.header("Error-Message");
				if (response.code() == StatusCodes.UNAUTHORIZED) {
					throw new AuthorizationException(msg);
				}
				/*
				 * 440 Login Timeout. Reference: https://support.microsoft.com/en-us/kb/941201
				 */
				else if (response.code() == 440) {
					throw new LoginTimeoutException(msg);
				}
				else {
					throw new ClientRequestException(msg);
				}
			}
			return response;
		}
		catch (IOException e) {
			throw new ClientRequestException(e);
		}
	}
	
	private ServerConfiguration getConfig() throws LoginTimeoutException, AuthorizationException, ClientRequestException {
		String url = HTTPServer.METAPROJECT;
		Response response = get(url);
		try {
			Serializer<Gson> serl = new DefaultJsonSerializer();
			InputStream is = response.body().byteStream();
			return (ServerConfiguration) serl.parse(new InputStreamReader(is), ServerConfiguration.class);
		}
		catch (ObjectConversionException e) {
			throw new ClientRequestException("Unable to parse the incoming server configuration data", e);
		}
		catch (IOException e) {
			throw new ClientRequestException("Data transmission error", e);
		}
		finally {
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
		}
		catch (IOException e) {
			throw new ClientRequestException("Data transmission error", e);
		}
		finally {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initConfig();
	}
	
	public void putEVSHistory(String code, String name, String operation, String reference) throws ClientRequestException {

		String url = HTTPServer.EVS_REC;

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream os = null;
		Response response = null;

		EVSHistory hist = new EVSHistory(code, name, operation, reference);

		try {
			os = new ObjectOutputStream(b);
			os.writeObject(hist);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			response = post(url, req, true);

			

			if (response.code() == 200) {
				// ok, do nothing
			} else if (response.code() == 500) {
				ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
				ServerException ex = (ServerException) ois.readObject();
				throw new ClientRequestException(ex.getMessage());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
    	return meta_agent.isOperationAllowed(operationId, projectId, userId);        
    }

    private boolean queryAdminPolicy(UserId userId, OperationId operationId) {
    	return meta_agent.isOperationAllowed(operationId, userId);
        
    }
    
    private Optional<ProjectId> getRemoteProject() {
        return Optional.ofNullable(projectId);
    }

	@Override
	public ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
			Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
					throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
