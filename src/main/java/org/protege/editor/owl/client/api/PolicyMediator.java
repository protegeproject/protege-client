package org.protege.editor.owl.client.api;

import edu.stanford.protege.metaproject.api.OperationId;

public interface PolicyMediator {



    boolean canAddAxiom();

    boolean canRemoveAxiom();

    boolean canAddAnnotatation();

    boolean canRemoveAnnotation();

    boolean canAddImport();

    boolean canRemoveImport();

    boolean canModifyOntologyId();

    boolean canAddUser();

    boolean canRemoveUser();

    boolean canModifyUser();

    boolean canAddProject();

    boolean canRemoveProject();

    boolean canModifyProject();

    boolean canViewProject();

    boolean canAddRole();

    boolean canRemoveRole();

    boolean canModifyRole();

    boolean canAddOperation();

    boolean caRemoveOperation();

    boolean canModifyOperation();

    boolean canAssignRole();

    boolean canRetractRole();

    boolean canStopServer();

    boolean canRestartServer();

    boolean canModifyServerConfig();

    boolean canPerformOperation(OperationId operationId);
}
