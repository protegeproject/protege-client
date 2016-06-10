package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;

import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.util.ClientUtils;

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
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (activeClient != null) {
            ClientUtils.performLogout(getClientSession(), activeClient);
            setEnabled(false);
        }
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_CLIENT)) {
            activeClient = event.getSource().getActiveClient();
            setEnabled(true);
        }
    }
}
