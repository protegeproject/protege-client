package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import java.awt.event.ActionEvent;
import java.util.concurrent.ScheduledFuture;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

public class EnableAutoUpdateAction extends AbstractClientAction {

    private static final long serialVersionUID = 1098490684799516207L;

    private ScheduledFuture<?> autoUpdate;
    private JCheckBoxMenuItem checkBoxMenuItem;

    @Override
    public void initialise() throws Exception {
        super.initialise();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    public void setMenuItem(JMenuItem menu) {
        checkBoxMenuItem = (JCheckBoxMenuItem) menu;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        killAutoUpdate();
        if (checkBoxMenuItem.isSelected()) {
            try {
                final VersionedOWLOntology vont = getActiveVersionedOntology();
                autoUpdate = submit(new AutoUpdate(vont), 15); // TODO Make the auto-update timing adjustable
            }
            catch (SynchronizationException e) {
                showSynchronizationErrorDialog(e.getMessage(), e);
            }
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
                    showSynchronizationErrorDialog("Autoupdate failed: " + t.getMessage(), t);
                }
                lastRunSuccessful = false;
            }
        }
    }
}
