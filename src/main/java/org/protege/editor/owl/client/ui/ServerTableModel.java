package org.protege.editor.owl.client.ui;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.impl.ConfigurationUtils;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.OWLClientException;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ServerTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 7786667531755850848L;

    public enum Column {
        PROJECT_NAME, PROJECT_DESCRIPTION, PROJECT_OWNER
    }

    private List<Project> remoteProjects;

    public void initialize(Client client) throws OWLClientException {
        if(client.getProjects().contains(ConfigurationUtils.getUniversalProject())) {
            try {
                remoteProjects = new ArrayList<>(client.getAllProjects());
                remoteProjects.remove(ConfigurationUtils.getUniversalProject());
            } catch (AuthorizationException e) {
                throw new OWLClientException(e.getCause());
            }
        } else {
            remoteProjects = new ArrayList<>(client.getProjects());
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
            case PROJECT_NAME:
                return "Project Name";
            case PROJECT_DESCRIPTION:
                return "Description";
            case PROJECT_OWNER:
                return "Owner";
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

        Column c = Column.values()[col];
        switch (c) {
            case PROJECT_NAME:
                return project.getName().get();
            case PROJECT_DESCRIPTION:
                return project.getDescription().get();
            case PROJECT_OWNER:
                return project.getOwner().get();
            default:
                throw new IllegalStateException("Programmer missed a case");
        }

    }

    public ProjectId getValueAt(int row) {
        Project project = remoteProjects.get(row);
        return project.getId();
    }
}
