package org.protege.editor.owl.client;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ServerUtils;
import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;
import org.protege.owl.server.connect.RmiServer;

import java.rmi.RemoteException;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ClientConfiguration;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

public class LocalClient implements Client {

    private AuthToken authToken;
    private String serverAddress;

    private RmiServer server;

    public LocalClient(AuthToken authToken, String serverAddress) {
        this.authToken = authToken;
        this.serverAddress = serverAddress;
    }

    @Override
    public AuthToken getAuthToken() {
        return authToken;
    }

    @Override
    public String getServerAddress() {
        return serverAddress;
    }

    @Override
    public ClientConfiguration getClientConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    public void connect() throws ServerRequestException {
        if (server == null) {
            try {
                server = (RmiServer) ServerUtils.getRemoteService(serverAddress, RmiServer.SERVER_SERVICE);
            }
            catch (RemoteException e) {
                throw new ServerRequestException(e);
            }
        }
    }

    public void disconnect() {
        server = null;
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
    public void viewProject(ProjectId projectId) throws ServerRequestException {
        connect();
        server.viewProject(authToken, projectId);
    }

    @Override
    public void commit(Project project, CommitBundle commits) throws ServerRequestException {
        connect();
        server.commit(authToken, project, commits);
    }
}
