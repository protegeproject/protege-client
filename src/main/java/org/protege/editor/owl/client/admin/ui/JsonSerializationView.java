package org.protege.editor.owl.client.admin.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.ui.UserLoginPanel;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class JsonSerializationView extends AbstractOWLViewComponent implements AdminTabListener {
    private static final long serialVersionUID = 6269299425107449908L;
    private JsonSerializationPanel serializationPanel;
    
    private JPanel buttonPanel;
    
    private JButton saveButton;
    
    private JButton cancelButton;
    
    private AdminTabManager manager = null;

    @Override
    protected void initialiseOWLView() throws Exception {
    	setLayout(new BorderLayout());
    	setBorder(GuiUtils.MATTE_BORDER);
    	serializationPanel = new JsonSerializationPanel(getOWLEditorKit());
    	add(serializationPanel, BorderLayout.CENTER);
    	add(createJButtonPanel(), BorderLayout.SOUTH);
    	manager = AdminTabManager.get(getOWLEditorKit());
    	manager.addListener(this);
    	// listener is not added until view initialized and changes may have already
    	// occurred
    	if (LocalHttpClient.current_user() != null) {
    		if (LocalHttpClient.current_user().configStateChanged()) {
    			setButtons(true);
    		}
    	}
    }

    private ClientSession getClientSession() {
        return ClientSession.getInstance(getOWLEditorKit());
    }

    @Override
    protected void disposeOWLView() {
        serializationPanel.dispose();
    }
    
    private JPanel createJButtonPanel() {
		buttonPanel = new JPanel();
		saveButton = new JButton("Save");
		saveButton.setEnabled(false);
		
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event)
			{
				try {
					LocalHttpClient.current_user().saveConfig();
					setButtons(false);
				}
				catch (LoginTimeoutException e) {
					JOptionPane.showMessageDialog(getOWLEditorKit().getOWLWorkspace(),
							new JLabel(e.getMessage()), "Failed to save changes",
							JOptionPane.ERROR_MESSAGE);
					localClientLogout();
					UserLoginPanel.showDialog(getOWLEditorKit(), JsonSerializationView.this);
				}
				catch (AuthorizationException | ClientRequestException e) {
					JOptionPane.showMessageDialog(getOWLEditorKit().getOWLWorkspace(),
							new JLabel(e.getMessage()), "Failed to save changes",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event)
			{
				try {
					LocalHttpClient.current_user().initConfig();
					setButtons(false);
					manager.statusChanged(AdminTabEvent.CONFIGURATION_RESET);
				}
				catch (LoginTimeoutException e) {
					JOptionPane.showMessageDialog(getOWLEditorKit().getOWLWorkspace(),
							new JLabel(e.getMessage()), "Failed to reset changes",
							JOptionPane.ERROR_MESSAGE);
					localClientLogout();
					UserLoginPanel.showDialog(getOWLEditorKit(), JsonSerializationView.this);
				}
				catch (AuthorizationException | ClientRequestException e) {
					JOptionPane.showMessageDialog(getOWLEditorKit().getOWLWorkspace(),
							new JLabel(e.getMessage()), "Failed to reset changes",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		return buttonPanel;
	}

	private void localClientLogout() {
		try {
			Client activeClient = getClientSession().getActiveClient();
			ClientUtils.performLogout(getClientSession(), activeClient);
		}
		catch (Exception e) {
			ErrorLogPanel.showErrorDialog(e);
		}
	}

	@Override
	public void statusChanged(AdminTabEvent event) {
		if (event.equals(AdminTabEvent.CONFIGURATION_CHANGED)) {
			setButtons(true);
					
		}		
	}
	
	private void setButtons(boolean b) {
		saveButton.setEnabled(b);
		cancelButton.setEnabled(b);	
		
	}
}
