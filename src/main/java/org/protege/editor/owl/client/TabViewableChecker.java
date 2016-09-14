package org.protege.editor.owl.client;

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
	
	public TabViewableChecker(Client c) {
		client = c;
	}
	
	@Override
	public boolean checkViewable(ViewComponentPlugin plugin) {
		return false;
	}

	@Override
	public boolean checkViewable(WorkspaceTabPlugin plugin) {
		String level = plugin.getPermissionLevel();
		if (level.equalsIgnoreCase("sysadmin")) {
			return (((LocalHttpClient) client).getClientType() == UserType.ADMIN);
		}
		
		if (level.equals("workflow_manager")) {
			try {
	    		Role wfm = ((LocalHttpClient) client).getRole(new RoleIdImpl("mp-project-manager"));
				return client.getActiveRoles().contains(wfm);
			} catch (ClientRequestException e) {
				e.printStackTrace();
			}
			//return (((LocalHttpClient) client).getClientType() == UserType.NON_ADMIN);
			
		}
		
		if (level.equals("workflow_modeler")) {
			return (((LocalHttpClient) client).getClientType() == UserType.NON_ADMIN);
			
		}
		
		// TODO: currently the default, need to dicsuss with Gilberto
		return false;
	}

}
