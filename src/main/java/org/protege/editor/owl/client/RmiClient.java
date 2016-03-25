package org.protege.editor.owl.client;

import org.protege.editor.owl.client.api.Client;
import org.protege.owl.server.api.CommitBundle;
import org.protege.owl.server.api.exception.ServerRequestException;
import org.protege.owl.server.connect.RmiServer;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ClientConfiguration;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

public class RmiClient implements Client {

    private AuthToken authToken;
    private RmiServer server;

    public RmiClient(AuthToken authToken, RmiServer server) {
        this.authToken = authToken;
        this.server = server;
    }

    @Override
    public AuthToken getAuthToken() {
        return authToken;
    }

    @Override
    public ClientConfiguration getClientConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addUser(User newUser) throws ServerRequestException {
        server.addUser(authToken, newUser);
    }

    @Override
    public void removeUser(UserId userId) throws ServerRequestException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addProject(Project newProject) throws ServerRequestException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeProject(ProjectId projectId) throws ServerRequestException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void viewProject(ProjectId projectId) throws ServerRequestException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void commit(ProjectId projectId, CommitBundle changes) throws ServerRequestException {
        // TODO Auto-generated method stub
        
    }
}
