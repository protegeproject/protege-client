package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

public class EnableAutoUpdateAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 1098490684799516207L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    private ScheduledFuture<?> autoUpdate;
    private JCheckBoxMenuItem checkBoxMenuItem;

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
        activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
        setEnabled(activeVersionOntology.isPresent());
    }

    public void setMenuItem(JMenuItem menu) {
        checkBoxMenuItem = (JCheckBoxMenuItem) menu;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        killAutoUpdate();
        if (checkBoxMenuItem.isSelected()) {
            final VersionedOWLOntology vont = activeVersionOntology.get();
            autoUpdate = submit(new AutoUpdate(vont), 15); // TODO Make the auto-update timing adjustable
        }
    }

    private void killAutoUpdate() {
        if (autoUpdate != null) {
            autoUpdate.cancel(false);
            autoUpdate = null;
        }
    }

    private class AutoUpdate implements Runnable {
        private VersionedOWLOntology vont;

        private boolean lastRunSuccessful = true;

        public AutoUpdate(VersionedOWLOntology vont) {
            this.vont = vont;
        }

        @Override
        public void run() {
            try {
//                  TODO: Implement update operation in the server
                lastRunSuccessful = true;
            }
//            catch (UserDeclinedAuthenticationException udae) {
//                killAutoUpdate();
//                checkBoxMenuItem.setSelected(false);
//            }
            catch (Throwable t) {
                if (!lastRunSuccessful) {
                    showErrorDialog("Synchronization error", "Auto-update failed: " + t.getMessage(), t);
                }
                lastRunSuccessful = false;
            }
        }
    }
}
