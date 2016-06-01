package org.protege.editor.owl.client;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.rmi.RemoteException;
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
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Host;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.Serializer;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalHttpClient implements Client {

	//private AuthToken authToken;
	private String serverAddress;

	private ProjectId projectId;
	private String userId;

	private UserInfo userInfo;

	private String auth_header_value;
	private final String AUTH_HEADER = "Authorization";

	private MetaprojectFactory fact = Manager.getFactory();

	OkHttpClient req_client = new OkHttpClient();

	private static LocalHttpClient current_user;

	public static LocalHttpClient current_user() {
		return current_user;
	}

	public LocalHttpClient(String user, String pwd, String serverAddress) {
		this.serverAddress = serverAddress;
		this.userInfo = login(user, pwd);
		this.userId = user;
		String toenc = this.userId + ":" + userInfo.getNonce();
		this.auth_header_value = "Basic " + new String(Base64.encodeBase64(toenc.getBytes()));
		LocalHttpClient.current_user = this;
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


	public void setActiveProject(ProjectId projectId) {
		this.projectId = projectId;
	}

	@Override
	public AuthToken getAuthToken() {
		// need to get rid of this method
		return null;
	}

	@Override
	public List<User> getAllUsers() throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createUser(User newUser, Optional<SaltedPasswordDigest> password)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteUser(UserId userId) throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateUser(UserId userId, User updatedUser)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Project> getProjects(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {

		String url = HTTPServer.PROJECTS + "?userid=" + userId.get();

		Response response = get(url);
		try {


			ObjectInputStream ois = new ObjectInputStream(response.body().byteStream());
			return (List<Project>) ois.readObject();


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public List<Project> getAllProjects() throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void updateProject(ProjectId projectId, Project updatedProject)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<ProjectId, List<Role>> getRoles(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Role> getRoles(UserId userId, ProjectId projectId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Role> getAllRoles() throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createRole(Role newRole) throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteRole(RoleId roleId) throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRole(RoleId roleId, Role updatedRole)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<ProjectId, List<Operation>> getOperations(UserId userId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Operation> getOperations(UserId userId, ProjectId projectId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Operation> getAllOperations() throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createOperation(Operation operation)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteOperation(OperationId operationId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateOperation(OperationId operationId, Operation updatedOperation)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void assignRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void retractRole(UserId userId, ProjectId projectId, RoleId roleId)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Host getHost() throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRootDirectory(String rootDirectory)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, String> getServerProperties()
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setServerProperty(String property, String value)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unsetServerProperty(String property)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

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
			e.printStackTrace();
		}


		return hist;
	}

	@Override
	public boolean canAddAxiom() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRemoveAxiom() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAddAnnotation() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRemoveAnnotation() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAddImport() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRemoveImport() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canModifyOntologyId() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canUpdateServerConfig() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canCreateUser() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canDeleteUser() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canUpdateUser() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canCreateProject() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canDeleteProject() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canUpdateProject() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canOpenProject() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canCreateRole() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canDeleteRole() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canUpdateRole() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canCreateOperation() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canDeleteOperation() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canUpdateOperation() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAssignRole() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRetractRole() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canStopServer() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canRestartServer() throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canPerformOperation(OperationId operationId) throws ClientRequestException {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public ServerDocument createProject(ProjectId projectId, Name projectName, Description description, UserId owner,
			Optional<ProjectOptions> options, Optional<CommitBundle> initialCommit)
					throws AuthorizationException, ClientRequestException, RemoteException {

		String url = HTTPServer.CREATE_PROJECT;

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
			e.printStackTrace();
		}


		// Do initial commit if the commit bundle is not empty
		if (initialCommit.isPresent()) {

			try {

				commit(projectId, initialCommit.get());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return serverDoc;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		return sdoc;

	}

	public VersionedOWLOntology buildVersionedOntology(ServerDocument sdoc, OWLOntologyManager owlManager)
			throws ClientRequestException, OWLOntologyCreationException {

		ChangeHistory remoteChangeHistory = getAllChanges(sdoc);


		OWLOntology targetOntology = owlManager.createOntology();
		ClientUtils.updateOntology(targetOntology, remoteChangeHistory, owlManager);
		return new VersionedOWLOntologyImpl(sdoc, targetOntology, remoteChangeHistory);
	}

	public ChangeHistory getAllChanges(ServerDocument sdoc) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		return history;

	}




	@Override
	public void deleteProject(ProjectId projectId, boolean includeFile)
			throws AuthorizationException, ClientRequestException, RemoteException {
		// TODO Auto-generated method stub

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

}
