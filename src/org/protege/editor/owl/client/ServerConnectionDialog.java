package org.protege.editor.owl.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.registry.Registry;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.RemoteOntologyDocument;
import org.protege.owl.server.connect.rmi.RMIClient;
import org.semanticweb.owlapi.model.IRI;

public class ServerConnectionDialog extends JDialog {
	private static final long serialVersionUID = 720048610707964509L;
	private Client client;
	private RemoteOntologyDocument remoteOntology;
	private JTextField urlField;
	private JButton uploadButton;
	
	public ServerConnectionDialog(Frame owner) {
		super(owner, "Server Connection Dialog");
	}

	public void initialise() {
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(getNorth(), BorderLayout.NORTH);
		getContentPane().add(getCenter(),BorderLayout.CENTER);
		getContentPane().add(getSouth(), BorderLayout.SOUTH);
		setSize(new Dimension(800, 600));
		pack();
		validate();
	}
	
	public Client getClient() {
		return client;
	}
	
	public RemoteOntologyDocument getRemoteOntology() {
		return remoteOntology;
	}
	
	private JPanel getNorth() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		urlField = new JTextField(RMIClient.SCHEME + "://localhost:" + Registry.REGISTRY_PORT + "/");	
		return panel;
	}
	
	private JScrollPane getCenter() {
		JScrollPane pane = new JScrollPane();
		return pane;
	}
	
	private JPanel getSouth() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		JButton connect = new JButton("Connect");
		connect.addActionListener(new ConnectActionListener());
		panel.add(connect);
		uploadButton = new JButton("Upload");
		uploadButton.addActionListener(new UploadActionListener());
		panel.add(uploadButton);
		return panel;
	}

	private class ConnectActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			client = new RMIClient(IRI.create(urlField.getText()));
			uploadButton.setEnabled(false);
		}
	}
	
	private class UploadActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
		}
	}

}
