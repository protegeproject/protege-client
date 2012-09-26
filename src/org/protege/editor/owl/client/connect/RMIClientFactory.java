package org.protege.editor.owl.client.connect;

import java.awt.Component;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.panel.LoginDialog;
import org.protege.owl.server.api.AuthToken;
import org.protege.owl.server.api.exception.AuthenticationFailedException;
import org.protege.owl.server.api.exception.UserDeclinedAuthenticationException;
import org.protege.owl.server.connect.rmi.AbstractRMIClientFactory;
import org.semanticweb.owlapi.model.IRI;

public class RMIClientFactory extends AbstractRMIClientFactory {
	Component parent = null;
    
    public RMIClientFactory(Component parent) {
		this.parent = parent;
	}

	@Override
    protected AuthToken login(IRI serverLocation) throws AuthenticationFailedException {
		AuthToken token = null;
		
		LoginDialog login = new LoginDialog();
        login.setLocationRelativeTo(parent);
		login.showDialog();
        
		if (login.okPressed()) {
			try {
				token = login(serverLocation, login.getName(), login.getPass());
			} 
			catch (RemoteException e) {
				throw new AuthenticationFailedException(e);
			}
			catch (NotBoundException nbe) {
			    throw new AuthenticationFailedException(nbe);
			}
		}
		else {
		    throw new UserDeclinedAuthenticationException("User pressed the cancel button.");
		}
        return token;
    }
 
}
