package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;

import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.util.ClientUtils;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class LogoutAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = -7606089236286884895L;

    private Client activeClient;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false);
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        getClientSession().removeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (activeClient != null) {
            try {
                ClientUtils.performLogout(getClientSession(), activeClient);
                setEnabled(false);
            }
            catch (Exception e) {
                showErrorDialog("Logout error", e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_CLIENT)) {
            activeClient = event.getSource().getActiveClient();
            setEnabled(true);
        }
        else if (event.hasCategory(EventCategory.CLEAR_SESSION)) {
            setEnabled(false);
       }
    }
}
