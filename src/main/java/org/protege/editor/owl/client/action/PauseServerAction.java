package org.protege.editor.owl.client.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLModelManagerImpl;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.CollectingChangeVisitor;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AnnotationChange;
import org.semanticweb.owlapi.model.ImportChange;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.UnloadableImportException;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PauseServerAction extends AbstractClientAction implements ClientSessionListener {

    private static final long serialVersionUID = 1098490684799516207L;

    private Optional<VersionedOWLOntology> activeVersionOntology = Optional.empty();

    
    private JCheckBoxMenuItem checkBoxMenuItem;

    @Override
    public void initialise() throws Exception {
        super.initialise();
        setEnabled(false); // initially the menu item is enabled
        getClientSession().addListener(this);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
    }

    @Override
    public void handleChange(ClientSessionChangeEvent event) {
        if (event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
            activeVersionOntology = Optional.ofNullable(event.getSource().getActiveVersionOntology());
            if (activeVersionOntology.isPresent()) {
            	if (((LocalHttpClient) getClientSession().getActiveClient()).isWorkFlowManager()) {
            		setEnabled(true);
            	} else {
            		setEnabled(false);
            	}
            }
            
        }
    }

    public void setMenuItem(JMenuItem menu) {
        checkBoxMenuItem = (JCheckBoxMenuItem) menu;
        checkBoxMenuItem.setSelected(false);
    }
    
   

    @Override
    public void actionPerformed(ActionEvent event) {
    	if (checkBoxMenuItem.isSelected()) {
    		try {
				((LocalHttpClient) getClientSession().getActiveClient()).pauseServer();
			} catch (AuthorizationException | ClientRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} else {
    		try {
				((LocalHttpClient) getClientSession().getActiveClient()).resumeServer();
			} catch (AuthorizationException | ClientRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    

 
        
     
		
    

    
}
