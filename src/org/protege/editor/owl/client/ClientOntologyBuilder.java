package org.protege.editor.owl.client;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

public class ClientOntologyBuilder extends ProtegeOWLAction {

	public ClientOntologyBuilder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initialise() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		OWLWorkspace workspace = getOWLWorkspace();
		Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, workspace);
		ServerConnectionDialog dialog = new ServerConnectionDialog(frame);
		dialog.setVisible(true);
	}

}
