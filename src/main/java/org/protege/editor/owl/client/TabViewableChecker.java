package org.protege.editor.owl.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.protege.editor.core.ui.view.ViewComponent;
import org.protege.editor.core.ui.view.ViewComponentPlugin;
import org.protege.editor.core.ui.workspace.TabViewable;
import org.protege.editor.core.ui.workspace.WorkspaceTabPlugin;
import org.protege.editor.owl.client.LocalHttpClient.UserType;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;

import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.impl.RoleIdImpl;

public class TabViewableChecker implements TabViewable {
	
	private Client client;
	
	private List<String> wf_man_tabs = new ArrayList<String>();
	private List<String> wf_mod_tabs = new ArrayList<String>();
	private List<String> admin_tabs = new ArrayList<String>();
	
	private List<String> req_admin_tabs = new ArrayList<String>();
	private List<String> req_editing_tabs = new ArrayList<String>();
	
	private void initTabs() {
		// these should be populated using the config file and metaproject api
		wf_man_tabs.add("NCI Edit Tab");
		wf_man_tabs.add("Lucene Search Tab");
		wf_man_tabs.add("Revision History");
		wf_man_tabs.add("Annotation Properties");
		wf_man_tabs.add("Entities");
		wf_man_tabs.add("Active Ontology");
		wf_man_tabs.add("Object Properties");
		wf_man_tabs.add("Classification results");
		wf_man_tabs.add("Superclass hierarchy (inferred)");
		wf_man_tabs.add("Superclass hierarchy");
		
		// currently modelers can see all the same tabs, but only in read only mode
		
		
		wf_mod_tabs.add("NCI Edit Tab");
		wf_mod_tabs.add("Lucene Search Tab");
		wf_mod_tabs.add("Revision History");
		wf_mod_tabs.add("Annotation Properties");
		wf_mod_tabs.add("Entities");
		wf_mod_tabs.add("Active Ontology");
		wf_mod_tabs.add("Object Properties");
		wf_mod_tabs.add("Classification results");
		wf_mod_tabs.add("Superclass hierarchy (inferred)");
		wf_mod_tabs.add("Superclass hierarchy");
		
		admin_tabs.add("Server Administration");
		req_admin_tabs.add("Server Administration");
		
		req_editing_tabs.add("NCI Edit Tab");
		req_editing_tabs.add("Lucene Search Tab");
		
	}
	
	public TabViewableChecker(Client c) {
		client = c;
		initTabs();
	}
	
	@Override
	public boolean checkViewable(ViewComponentPlugin plugin) {
		Set<String> categories = plugin.getCategorisations();
		for (String category : categories) {
			if (checkStringAgainstClient(category)) {
				return true;
			}
			
		}
		return false;
	}
	
	@Override
	public boolean checkViewable(WorkspaceTabPlugin plugin) {
		return checkStringAgainstClient(plugin.getLabel());
	}
	
	@Override
	public boolean isReadOnly(ViewComponentPlugin view) {
		if (this.isWorkFlowModeler()) {
			return true;
		}
		return false;
	}

	
	
	private boolean checkStringAgainstClient(String cat) {
		// TODO: deal with diff between views and tabs and how categories are produced
		
		if (isSysAdmin()) {
			return admin_tabs.contains(cat);
		} else if (isWorkFlowModeler()) {
			return wf_mod_tabs.contains(cat);
		} else if (isWorkFlowManager()) {
			return wf_man_tabs.contains(cat);			
		} else {
			return false;
		}
		
		
	}
	
	private boolean isWorkFlowManager() {
		// check if project loaded first
		if (((LocalHttpClient) client).getRemoteProject().isPresent()) {
			try {
				Role wfm = ((LocalHttpClient) client).getRole(new RoleIdImpl("mp-project-manager"));
				return client.getActiveRoles().contains(wfm);
			} catch (ClientRequestException e) {
				e.printStackTrace();
			}
		}
		return false;		
	}
	
	private boolean isWorkFlowModeler() {
		if (((LocalHttpClient) client).getRemoteProject().isPresent()) {
			try {
				Role wfm = ((LocalHttpClient) client).getRole(new RoleIdImpl("mp-workflow-modeler"));
				return client.getActiveRoles().contains(wfm);
			} catch (ClientRequestException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return !isSysAdmin();
		}
	}

	private boolean isSysAdmin() {		
		return (((LocalHttpClient) client).getClientType() == UserType.ADMIN);
	}

	@Override
	public boolean isRequired(WorkspaceTabPlugin plugin) {
		String label = plugin.getLabel();
		if (this.isSysAdmin()) {
			return this.req_admin_tabs.contains(label);
		} 
		if (this.isWorkFlowManager() || this.isWorkFlowModeler()) {
			return this.req_editing_tabs.contains(label);
		}
		return false;
	}

	

	

}
