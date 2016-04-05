package org.protege.editor.owl.client.api;

import java.util.Map;
import java.util.Set;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.RoleId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

public interface RegistryMediator {

    Set<User> getUsers();

    Set<Project> getProjects();

    Set<Role> getRoles();

    Set<Operation> getOperations();

    Map<UserId,Map<ProjectId,Set<RoleId>>> getPolicyMappings();
}
