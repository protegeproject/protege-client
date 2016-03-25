package org.protege.editor.owl.client.connect;

import org.protege.owl.server.api.RmiLoginService;
import org.protege.owl.server.security.DefaultLoginService;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.Salt;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.UserAuthenticator;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.impl.UnauthorizedUserToken;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class DefaultUserAuthenticator implements UserAuthenticator {

    private RmiLoginService loginService;

    public DefaultUserAuthenticator(RmiLoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public AuthToken hasValidCredentials(UserId userId, PlainPassword password) throws Exception {
        AuthToken authToken = new UnauthorizedUserToken(userId);
        if (loginService instanceof DefaultLoginService) {
            Salt userSalt = ((DefaultLoginService) loginService).getSalt(userId);
            SaltedPasswordDigest passwordDigest = Manager.getFactory().createPasswordHasher().hash(password, userSalt);
            authToken = loginService.login(userId, passwordDigest);
        }
        return authToken;
    }
}