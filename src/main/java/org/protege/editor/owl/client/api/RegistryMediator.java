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

    Set<Project> getProjects();

    Set<Role> getRoles();

    Set<Operation> getOperations();

    Set<User> getAllUsers();

    Set<Project> getAllProjects();

    Set<Role> getAllRoles();

    Set<Operation> getAllOperations();

    Map<UserId,Map<ProjectId,Set<RoleId>>> getPolicyMappings();
}
