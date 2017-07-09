package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import edu.stanford.protege.metaproject.impl.ServerStatus;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PauseServerAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 1098490684799516207L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    
    private JCheckBoxMenuItem checkBoxMenuItem;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is enabled
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
            if (activeVersionOntology.isPresent()) {
                LocalHttpClient client = (LocalHttpClient) getClientSession().getActiveClient();
                if (client.isWorkFlowManager(getClientSession().getActiveProject())) {
                    try {
                        ServerStatus status = client.getServerStatus();
                        if (status.pausingUser.isPresent()) {
                            String clientName = client.getUserInfo().getName();
                            String serverName = status.pausingUser.get().getName().get();
                            setEnabled(serverName.equals(clientName));
                            checkBoxMenuItem.setSelected(true);
                        }
                        else {
                            setEnabled(true);
                            checkBoxMenuItem.setSelected(false);
                        }
                    }
                    catch (ClientRequestException e) {
                        setEnabled(false);
                    }
                } else {
                    setEnabled(false);
                }
            }
            
        }
    }

    public void setMenuItem(JMenuItem menu) {
        checkBoxMenuItem = (JCheckBoxMenuItem) menu;
        checkBoxMenuItem.setSelected(false);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (checkBoxMenuItem.isSelected()) {
            try {
                ((LocalHttpClient) getClientSession().getActiveClient()).pauseServer();
                getOWLModelManager().fireEvent(EventType.SERVER_PAUSED);
            } catch (AuthorizationException | ClientRequestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                ((LocalHttpClient) getClientSession().getActiveClient()).resumeServer();
                getOWLModelManager().fireEvent(EventType.SERVER_RESUMED);
            } catch (AuthorizationException | ClientRequestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
