package org.protege.editor.owl.client.api;

import org.protege.editor.owl.client.api.exception.ClientRequestException;

import edu.stanford.protege.metaproject.api.OperationId;

public interface PolicyMediator {

    boolean canAddAxiom() throws ClientRequestException;

    boolean canRemoveAxiom() throws ClientRequestException;

    boolean canAddAnnotatation() throws ClientRequestException;

    boolean canRemoveAnnotation() throws ClientRequestException;

    boolean canAddImport() throws ClientRequestException;

    boolean canRemoveImport() throws ClientRequestException;

    boolean canModifyOntologyId() throws ClientRequestException;

    boolean canAddUser() throws ClientRequestException;

    boolean canRemoveUser() throws ClientRequestException;

    boolean canModifyUser() throws ClientRequestException;

    boolean canAddProject() throws ClientRequestException;

    boolean canRemoveProject() throws ClientRequestException;

    boolean canModifyProject() throws ClientRequestException;

    boolean canOpenProject() throws ClientRequestException;

    boolean canAddRole() throws ClientRequestException;

    boolean canRemoveRole() throws ClientRequestException;

    boolean canModifyRole() throws ClientRequestException;

    boolean canAddOperation() throws ClientRequestException;

    boolean canRemoveOperation() throws ClientRequestException;

    boolean canModifyOperation() throws ClientRequestException;

    boolean canAssignRole() throws ClientRequestException;

    boolean canRetractRole() throws ClientRequestException;

    boolean canStopServer() throws ClientRequestException;

    boolean canRestartServer() throws ClientRequestException;

    boolean canModifyServerConfig() throws ClientRequestException;

    boolean canPerformOperation(OperationId operationId) throws ClientRequestException;
}
