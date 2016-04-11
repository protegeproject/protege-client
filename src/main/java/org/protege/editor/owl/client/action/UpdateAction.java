package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import java.awt.event.ActionEvent;

public class UpdateAction extends AbstractClientAction {

    private static final long serialVersionUID = 2694484296709954780L;

    @Override
    public void initialise() throws Exception {
        super.initialise();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            final VersionedOntologyDocument vont = getActiveVersionedOntology();
            submit(new DoUpdate(vont));
        }
        catch (SynchronizationException e) {
            showSynchronizationErrorDialog(e.getMessage(), e);
        }
    }

    private class DoUpdate implements Runnable {
        private VersionedOntologyDocument vont;

        public DoUpdate(VersionedOntologyDocument vont) {
            this.vont = vont;
        }

        @Override
        public void run() {
//              TODO: Implement update operation in the server
        }
    }
}
