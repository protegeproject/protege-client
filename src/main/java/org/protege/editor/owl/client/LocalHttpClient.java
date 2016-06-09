package org.protege.editor.owl.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import org.apache.commons.codec.binary.Base64;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.UserInfo;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.messages.HttpAuthResponse;
import org.protege.editor.owl.server.http.messages.LoginCreds;
import org.protege.editor.owl.server.versioning.VersionedOWLOntologyImpl;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.AuthenticationRegistry;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.MetaprojectAgent;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
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
import edu.stanford.protege.metaproject.impl.UserIdImpl;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalHttpClient implements Client, ClientSessionListener {

	//private AuthToken authToken;
	private String serverAddress;

	private ProjectId projectId;
	private String userId;

	private UserInfo userInfo;
	private AuthToken auth_token = null;

	private String auth_header_value;
	private final String AUTH_HEADER = "Authorization";

	private MetaprojectFactory fact = Manager.getFactory();

	OkHttpClient req_client = new OkHttpClient.Builder().build();
			//new OkHttpClient.Builder().readTimeout(180, TimeUnit.SECONDS).build();
	
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
	

	private static LocalHttpClient current_user;

	public static LocalHttpClient current_user() {
		return current_user;
	}
	
	public ServerConfiguration getCurrentConfig() {
		return config;		
	}

	public LocalHttpClient(String user, String pwd, String serverAddress) {
		this.serverAddress = serverAddress;
		this.userInfo = login(user, pwd);
		this.userId = user;
		String toenc = this.userId + ":" + userInfo.getNonce();
		this.auth_header_value = "Basic " + new String(Base64.encodeBase64(toenc.getBytes()));
		LocalHttpClient.current_user = this;
		//check if user is allowed to edit config
		initConfig();
	}
	
	public void initConfig() {
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
	
	
	private UserInfo login(String user, String pwd) {

		final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
		String url = HTTPServer.LOGIN;

		LoginCreds creds = new LoginCreds(user, pwd);

		HttpAuthResponse resp = null;

		Serializer<Gson> serl = new DefaultJsonSerializer();
		RequestBody body = RequestBody.create(JSON, serl.write(creds, LoginCreds.class));

		Response response = post(url, body, false);

		try {
			resp = (HttpAuthResponse) serl.parse(new InputStreamReader(response.body().byteStream()), HttpAuthResponse.class);
		} catch (FileNotFoundException | ObjectConversionException e) {
			e.printStackTrace();
		}
		return new UserInfo(resp.getId(), resp.getName(), resp.getEmail(), resp.getToken());


	}
	
	@Override
	public void handleChange(ClientSessionChangeEvent event) {
		projectId = event.getSource().getActiveProject();
	}
	
	@Override
    public void setActiveProject(ProjectId projectId) {
        this.projectId = projectId;
    }

	
	@Override
	public AuthToken getAuthToken() {
		if (auth_token != null) {
			
		} else {
			User user = null;
			try {
				user = user_registry.get(new UserIdImpl(userId));
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
	
	@Override
	public ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
			Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
					throws AuthorizationException, ClientRequestException, RemoteException {

		String url = HTTPServer.PROJECT;

		Response response;
		ServerDocument serverDoc = null;

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
			serverDoc = (ServerDocument) ois.readObject();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}


		// Do initial commit if the commit bundle is not empty
		if (initialCommit.isPresent()) {

			try {

				commit(projectId, initialCommit.get());

			} catch (Exception e) {
				throw new ClientRequestException(e.getMessage(), e.getCause());
			}

		}
		initConfig();
		return serverDoc;

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


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}   
		return sdoc;

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
			hist = (ChangeHistory) ois.readObject();

		} catch (IOException | ClassNotFoundException e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}


		return hist;
	}

	

	@Override
	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public List<Project> getProjects() throws ClientRequestException {
		try {
			return getProjects(fact.getUserId(userId));
		}
		catch (AuthorizationException | RemoteException e) {
			throw new ClientRequestException(e.getMessage(), e);
		}
	}

	@Override
	public List<Role> getActiveRoles() throws ClientRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Operation> getActiveOperations() throws ClientRequestException {
		// TODO Auto-generated method stub
		return null;
	}



	public VersionedOWLOntology buildVersionedOntology(ServerDocument sdoc, OWLOntologyManager owlManager)
			throws ClientRequestException, OWLOntologyCreationException {

		ChangeHistory remoteChangeHistory = getAllChanges(sdoc);


		OWLOntology targetOntology = owlManager.createOntology();
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}

	public ChangeHistory getAllChanges(ServerDocument sdoc) throws ClientRequestException {
		ChangeHistory history = null;
		try {


			// TODO: get all changes
			String url = HTTPServer.ALL_CHANGES;
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(sdoc.getHistoryFile());
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());

			history = (ChangeHistory) ois.readObject();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}  
		return history;

	}
	
	public DocumentRevision getRemoteHeadRevision(VersionedOWLOntology vont) throws
		ClientRequestException {		
		return getLatestChanges(vont).getHeadRevision();
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
			String url = HTTPServer.ALL_CHANGES;
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(b);

			os.writeObject(sdoc.getHistoryFile());
			
			os.writeObject(start);
			RequestBody req = RequestBody.create(MediaType.parse("application"), b.toByteArray());

			Response response = post(url, req, true);

			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());

			history = (ChangeHistory) ois.readObject();


		} catch (Exception e) {
			throw new ClientRequestException(e.getMessage(), e.getCause());
		}  
		return history;

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

	private Response get(String url) {
		Request request = new Request.Builder()
				.url(serverAddress + url)
				.addHeader(AUTH_HEADER, auth_header_value)
				.get()
				.build();    	
		try {
			return req_client.newCall(request).execute();    		

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private ServerConfiguration getConfig() {
		String url = HTTPServer.METAPROJECT;
		Response response = get(url);
	
		Serializer<Gson> serl = new DefaultJsonSerializer();
		
		ServerConfiguration scfg = null;
		

		try {
			scfg = (ServerConfiguration) serl.parse(new InputStreamReader(response.body().byteStream()), ServerConfiguration.class);
		} catch (FileNotFoundException | ObjectConversionException e) {
			e.printStackTrace();
		}
		return scfg;
		
	}
	
	public void putConfig() {
		if (save_cancel_semantics) {
			config_state_changed = true;		
			
		} else {
			reallyPutConfig();
		}		
	}
	
	public boolean configStateChanged() {
		return config_state_changed;
	}
	
	public void reallyPutConfig() {
		
		final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
		String url = HTTPServer.METAPROJECT;

		Serializer<Gson> serl = new DefaultJsonSerializer();
		RequestBody body = RequestBody.create(JSON, serl.write(this.config, ServerConfiguration.class));

		post(url, body, true);
		initConfig();
	}




	
}
