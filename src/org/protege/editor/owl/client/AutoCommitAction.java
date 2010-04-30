package org.protege.editor.owl.client;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.apache.log4j.Logger;
import org.protege.editor.core.ui.action.ProtegeAction;

public class AutoCommitAction extends ProtegeAction {
    private static final long serialVersionUID = 1693797438703528655L;
    private static final Logger logger = Logger.getLogger(AutoCommitAction.class);

    public AutoCommitAction() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JCheckBoxMenuItem) {
            logger.info("is selected = " + ((JCheckBoxMenuItem) event.getSource()).isSelected());
        }
    }
    
    public void setMenuItem(JMenuItem menu) {
        ((JCheckBoxMenuItem) menu).setSelected(true);
    }

}
