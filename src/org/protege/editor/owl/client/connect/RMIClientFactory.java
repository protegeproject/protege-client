package org.protege.editor.owl.client.connect;

import java.awt.Component;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.panel.LoginDialog;
import org.protege.owl.server.api.AuthToken;
import org.protege.owl.server.connect.rmi.AbstractRMIClientFactory;
import org.semanticweb.owlapi.model.IRI;

public class RMIClientFactory extends AbstractRMIClientFactory {
	Component parent = null;
    
    public RMIClientFactory(Component parent) {
		this.parent = parent;
	}

	@Override
    protected AuthToken login(IRI serverLocation) {
		AuthToken token = null;
		
		LoginDialog login = new LoginDialog();
        login.setLocationRelativeTo(parent);
		login.showDialog();
        
		if (login.okPressed()) {
			try {
				token = login(serverLocation, login.getName(), login.getPass());
			} catch (Exception e) {
				ProtegeApplication.getErrorLog().logError(e);
			}
		}

        return token;
    }
 
}
