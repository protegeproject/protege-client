package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

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
        if (LocalHttpClient.current_user().configStateChanged()) {
        	setButtons(true);
        }
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
			 
            public void actionPerformed(ActionEvent e)
            {
            	LocalHttpClient.current_user().reallyPutConfig();
            	setButtons(false);
            	
            }
        });     
		
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		
		cancelButton.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
            	LocalHttpClient.current_user().initConfig();
            	setButtons(false);
            	manager.statusChanged(AdminTabEvent.CONFIGURATION_RESET);
            	
            	
            }
        });     
		
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		return buttonPanel;
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
