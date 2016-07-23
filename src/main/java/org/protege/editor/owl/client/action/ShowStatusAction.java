package org.protege.editor.owl.client.action;


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ShowStatusAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 4601012273632698091L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is disabled
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
            setEnabled(activeVersionOntology.isPresent());
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        try {
            final VersionedOWLOntology vont = activeVersionOntology.get();
            
            JDialog dialog = new JDialog();
            dialog.setTitle("Client status");
            dialog.setLocationRelativeTo(getOWLWorkspace());

            JPanel panel = new JPanel(new GridLayout(0, 2));

            panel.add(new JLabel("Local/HEAD Revision:"));
            panel.add(new JLabel(vont.getHeadRevision().toString()));

            panel.add(new JLabel("Remote/HEAD Revision:"));
            panel.add(new JLabel(LocalHttpClient.current_user().getRemoteHeadRevision(vont).toString()));

            panel.add(new JLabel("#Uncommitted Changes:"));
            panel.add(new JLabel(ClientUtils.getUncommittedChanges(getOWLModelManager().getHistoryManager(), vont.getOntology(), vont.getChangeHistory()).size()+""));

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(panel, BorderLayout.CENTER);
            dialog.pack();
            dialog.setVisible(true);
        }
        catch (Exception e) {
        	//TODO: LocalHttpClient needs to throw ClientRequestException here
            showErrorDialog("Show status error", e.getMessage(), e);
        }
    }
}
