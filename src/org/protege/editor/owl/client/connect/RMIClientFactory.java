package org.protege.editor.owl.client.connect;

import org.protege.editor.owl.client.panel.LoginDialog;
import org.protege.owl.server.api.AuthToken;
import org.protege.owl.server.connect.rmi.AbstractRMIClientFactory;
import org.protege.owl.server.policy.RMILoginUtility;
import org.semanticweb.owlapi.model.IRI;

public class RMIClientFactory extends AbstractRMIClientFactory {
    private String userName;
    private String password;
    
    @Override
    protected AuthToken login(IRI serverLocation) {
        if (userName == null) {
            LoginDialog login = new LoginDialog(null, "Login");
            if (login.showDialog()) {
                
            }
        }
        try {
        return RMILoginUtility.login(serverLocation, userName, password);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
