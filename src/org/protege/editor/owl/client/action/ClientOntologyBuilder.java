package org.protege.editor.owl.client.action;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

import org.protege.editor.owl.client.panel.ServerConnectionDialog;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

public class ClientOntologyBuilder extends ProtegeOWLAction {
	private static final long serialVersionUID = 8498511423505913017L;

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
		ServerConnectionDialog dialog = new ServerConnectionDialog(frame, getOWLEditorKit());
		dialog.initialise();
		dialog.setLocationRelativeTo(workspace);
		dialog.setVisible(true);
	}

}
