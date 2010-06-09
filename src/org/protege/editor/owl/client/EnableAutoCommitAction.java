package org.protege.editor.owl.client;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.apache.log4j.Logger;
import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.owl.OWLEditorKit;

public class EnableAutoCommitAction extends ProtegeAction {
    private static final long serialVersionUID = 1693797438703528655L;
    private static final Logger logger = Logger.getLogger(EnableAutoCommitAction.class);

    public EnableAutoCommitAction() {
    }

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return ClientOntologyBuilder.getServerPreferences((OWLEditorKit) getEditorKit()) != null;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        ServerPreferences preferences = ClientOntologyBuilder.getServerPreferences((OWLEditorKit) getEditorKit());
        if (preferences == null) return;
        if (event.getSource() instanceof JCheckBoxMenuItem) {
            boolean doAutoCommit = ((JCheckBoxMenuItem) event.getSource()).isSelected();
            preferences.setAutoCommit(doAutoCommit);
            if (doAutoCommit) {
                logger.info("Auto-commit enabled");
            }
            else {
                logger.info("Auto-commit disabled");
            }
        }
    }
    
    public void setMenuItem(JMenuItem menu) {
        ((JCheckBoxMenuItem) menu).setSelected(true);
    }

}
