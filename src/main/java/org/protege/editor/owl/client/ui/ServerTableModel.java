package org.protege.editor.owl.client.ui;

import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.impl.MetaprojectUtils;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.OWLClientException;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.table.AbstractTableModel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -1677982790864801841L;

    public enum Column {
        REMOTE_PROJECT;
    }

    private List<Project> remoteProjects;

    public void initialize(Client client) throws OWLClientException {
        remoteProjects = new ArrayList<>(client.getProjects());
        if(remoteProjects.contains(MetaprojectUtils.getUniversalProject())) {
            remoteProjects.remove(MetaprojectUtils.getUniversalProject());
            try {
                remoteProjects.addAll(client.getAllProjects());
            } catch (AuthorizationException | RemoteException e) {
                throw new OWLClientException(e.getCause());
            }
        }
        Collections.sort(remoteProjects);
        fireTableStructureChanged();
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int col) {
        Column column = Column.values()[col];
        switch (column) {
        case REMOTE_PROJECT:
            return "Available projects";
        default:
            throw new IllegalStateException("Programmer missed a case");
        }
    }

    @Override
    public int getRowCount() {
        return (remoteProjects == null) ? 0 : remoteProjects.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        Project project = remoteProjects.get(row);
        return new ProjectItem(project.getId(), project.getName());
    }

    public ProjectId getValueAt(int row) {
        ProjectItem projectItem = (ProjectItem) getValueAt(row, -1);
        return projectItem.getId();
    }

    class ProjectItem {

        private ProjectId projectId;
        private Name projectName;

        ProjectItem(ProjectId projectId, Name projectName) {
            this.projectId = projectId;
            this.projectName = projectName;
        }

        public ProjectId getId() {
            return projectId;
        }

        public Name getName() {
            return projectName;
        }

        @Override
        public String toString() {
            return projectName.get();
        }
    }
}
