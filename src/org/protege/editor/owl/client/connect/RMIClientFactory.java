package org.protege.editor.owl.client.connect;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.owl.client.panel.LoginDialog;
import org.protege.owl.server.api.AuthToken;
import org.protege.owl.server.connect.rmi.AbstractRMIClientFactory;
import org.protege.owl.server.policy.RMILoginUtility;
import org.semanticweb.owlapi.model.IRI;

public class RMIClientFactory extends AbstractRMIClientFactory {
    
    @Override
    protected AuthToken login(IRI serverLocation) {
        LoginDialog login = new LoginDialog(null, "Login");
        if (login.showDialog()) {
            try {
                return RMILoginUtility.login(serverLocation, login.getName(), login.getPass());
            }
            catch (Exception e) {
                ProtegeApplication.getErrorLog().logError(e);
            }
        }
        return null;
    }
 
}
