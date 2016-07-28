package org.protege.editor.owl.client.admin.ui;

import com.google.common.base.Objects;
import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.impl.Operations;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.event.*;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.admin.model.ProjectMListItem;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -6832671439689809834L;
    private static MetaprojectFactory f = Manager.getFactory();
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList projectList;
    private Project selectedProject;
    private ClientSession session;
    private Client client;
    private UserId userId;

    /**
     * Constructor
     *
     * @param editorKit    OWL editor kit
     */
    public ProjectPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        session = ClientSession.getInstance(editorKit);
        session.addListener(sessionListener);
        client = session.getActiveClient();
        userId = (client != null ? f.getUserId(client.getUserInfo().getId()) : null);
        initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED)) {
            if(configManager.hasSelection() && !configManager.getSelection().isProject()) {
                projectList.clearSelection();
            }
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if (event.hasCategory(EventCategory.SWITCH_CLIENT) || event.hasCategory(ClientSessionChangeEvent.EventCategory.CLEAR_SESSION)) {
            client = session.getActiveClient();
            if(event.hasCategory(EventCategory.SWITCH_CLIENT)) {
                userId = f.getUserId(client.getUserInfo().getId()); // TODO: should get a UserId directly!
            }
            removeAll();
            initUi();
        }
    };

    private void initUi() {
        setupList();
        setLayout(new BorderLayout());
        JScrollPane scrollpane = new JScrollPane(projectList);
        scrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        add(scrollpane, BorderLayout.CENTER);
        listProjects();
    }

    private ListSelectionListener listSelectionListener = e -> {
        Object selectedObj = projectList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof ProjectListItem) {
                selectedProject = ((ProjectListItem) selectedObj).getProject();
                configManager.setSelection(selectedProject);
            }
            else if (selectedObj instanceof ProjectListHeaderItem) {
                configManager.clearSelection();
            }
        }
    };

    private void setupList() {
        projectList = new MList() {
            protected void handleAdd() {
                addProject();
            }

            protected void handleDelete() {
                deleteProject();
            }

            protected void handleEdit() {
                editProject();
            }
        };
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.addListSelectionListener(listSelectionListener);
        projectList.setCellRenderer(new ProjectListCellRenderer());
        projectList.addKeyListener(keyAdapter);
        projectList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(projectList.getSelectedValue() instanceof ProjectListHeaderItem) {
                    addProject();
                } else {
                    editProject();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(projectList.getSelectedValue() instanceof ProjectListHeaderItem) {
                    addProject();
                } else {
                    editProject();
                }
            }
        }
    };

    private void listProjects() {
        ArrayList<Object> data = new ArrayList<>();
        data.add(new ProjectListHeaderItem());
        try {
            if(client != null) {
                List<Project> projects = client.getAllProjects();
                Collections.sort(projects);
                data.addAll(projects.stream().map(ProjectListItem::new).collect(Collectors.toList()));
            }
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        projectList.setListData(data.toArray());
    }

    private void addProject() {
        if(client != null && client.canCreateProject()) {
            Optional<Project> project = ProjectDialogPanel.showDialog(editorKit);
            if (project.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listProjects();
                selectProject(project.get());
            }
        }
    }

    private void editProject() {
        if(client != null && canModifyProject(selectedProject)) {
            Optional<Project> project = ProjectDialogPanel.showDialog(editorKit, selectedProject);
            if (project.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listProjects();
                selectProject(project.get());
            }
        }
    }

    private void deleteProject() {
        if(client != null && canDeleteProject(selectedProject)) {
            Object selectedObj = projectList.getSelectedValue();
            if (selectedObj instanceof ProjectListItem) {
                Project project = ((ProjectListItem) selectedObj).getProject();
                String projectName = project.getName().get();

                JPanel panel = new JPanel(new GridLayout(0, 1));
                panel.add(new JLabel("Proceed to delete project '" + projectName + "'? All policy entries involving '" + projectName + "' will be removed."));
                JCheckBox checkBox = new JCheckBox("Delete the history file of the project");
                panel.add(checkBox);

                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Project '" + projectName + "'", panel,
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    client.deleteProject(project.getId(), checkBox.isSelected());
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listProjects();
            }
        }
    }

    private void selectProject(Project project) {
        for(int i = 0; i < projectList.getModel().getSize(); i++) {
            Object item = projectList.getModel().getElementAt(i);
            if(item instanceof ProjectListItem) {
                if (((ProjectListItem)item).getProject().getId().equals(project.getId())) {
                    projectList.setSelectedValue(item, true);
                    break;
                }
            }
        }
    }

    private boolean canDeleteProject(Project project) {
        return client.queryProjectPolicy(userId, project.getId(), Operations.REMOVE_PROJECT.getId());
    }

    private boolean canModifyProject(Project project) {
        return client.queryProjectPolicy(userId, project.getId(), Operations.MODIFY_PROJECT.getId());
    }


    /**
     * Add Project item
     */
    public class ProjectListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Projects";
        }

        @Override
        public boolean canAdd() {
            return (client != null && client.canCreateProject());
        }
    }

    /**
     * Project list item
     */
    public class ProjectListItem implements ProjectMListItem {
        private Project project;

        /**
         * Constructor
         *
         * @param project   Project
         */
        public ProjectListItem(Project project) {
            this.project = checkNotNull(project);
        }

        @Override
        public Project getProject() {
            return project;
        }

        @Override
        public boolean isEditable() {
            return (client != null && canModifyProject(project));
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return (client != null && canDeleteProject(project));
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return project.getName().get();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProjectListItem)) {
                return false;
            }
            ProjectListItem that = (ProjectListItem) o;
            return Objects.equal(project, that.project);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(project);
        }
    }

    @Override
    public void dispose() {
        projectList.removeListSelectionListener(listSelectionListener);
        configManager.removeListener(tabListener);
        session.removeListener(sessionListener);
    }
}
