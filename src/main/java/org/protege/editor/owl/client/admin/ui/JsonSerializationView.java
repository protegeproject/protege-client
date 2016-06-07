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
    
    private JButton clearButton;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        serializationPanel = new JsonSerializationPanel(getOWLEditorKit());
        add(serializationPanel, BorderLayout.CENTER);
        add(createJButtonPanel(), BorderLayout.SOUTH);
        AdminTabManager.get(getOWLEditorKit()).addListener(this);
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
		
		clearButton = new JButton("Clear");
		clearButton.setEnabled(false);
		
		clearButton.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
            	LocalHttpClient.current_user().initConfig();
            	setButtons(false);
            	
            }
        });     
		
		buttonPanel.add(saveButton);
		buttonPanel.add(clearButton);
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
		clearButton.setEnabled(b);	
		
	}
}
