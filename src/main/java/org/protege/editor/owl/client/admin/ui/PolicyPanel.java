package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.exception.UserNotInPolicyException;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.admin.model.ProjectMListItem;
import org.protege.editor.owl.client.admin.model.RoleMListItem;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PolicyPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -7419215167017892008L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList projectList, roleList;
    private Project selectedProject;

    /**
     * Constructor
     *
     * @param editorKit    OWL editor kit
     */
    public PolicyPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        initUiComponents();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED)) {
            projectList.clearSelection();
            roleList.clearSelection();
            clearList(roleList, projectList);
            listProjects();
        } else if(event.equals(AdminTabEvent.POLICY_ITEM_SELECTION_CHANGED)) {
            if(configManager.getPolicySelection() != null) {
                listRoles();
            }
        }
    };

    private void initUiComponents() {
        setBackground(getBackground());
        setLayout(new BorderLayout());
        setupProjectList();
        setupRoleList();

        JPanel projectPanel = new JPanel(new BorderLayout());
        JScrollPane projectScrollpane = new JScrollPane(projectList);
        projectScrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        projectPanel.add(projectScrollpane, BorderLayout.CENTER);

        JPanel rolePanel = new JPanel(new BorderLayout());
        JScrollPane roleScrollpane = new JScrollPane(roleList);
        roleScrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        rolePanel.add(roleScrollpane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, rolePanel);
        splitPane.setBorder(GuiUtils.EMPTY_BORDER);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
    }

    private ListSelectionListener projectListSelectionListener = e -> {
        Object selectedObj = projectList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof ProjectMListItem) {
                selectedProject = ((ProjectMListItem) selectedObj).getProject();
                configManager.setPolicySelection(selectedProject);
            }
            else if(selectedObj instanceof ProjectListHeaderItem) {
                configManager.clearPolicySelection();
                clearList(roleList);
            }
        }
    };

    private ListSelectionListener roleListSelectionListener = e -> {
        Object selectedObj = roleList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof RoleMListItem) {
                configManager.setPolicySelection(((RoleMListItem) selectedObj).getRole());
            } else if(selectedObj instanceof RoleListHeaderItem) {
                configManager.clearPolicySelection();
            }
        }
    };

    private void setupProjectList() {
        projectList = new MList() {
            protected void handleAdd() {
                addProjectAssignment();
            }

            protected void handleDelete() {
                deleteProjectAssignment();
            }
        };
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.addListSelectionListener(projectListSelectionListener);
        projectList.setCellRenderer(new ProjectListCellRenderer());
    }

    private void setupRoleList() {
        roleList = new MList() {
            protected void handleAdd() {
                addRoleAssignment();
            }

            protected void handleDelete() {
                deleteRoleAssignment();
            }
        };
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.addListSelectionListener(roleListSelectionListener);
        roleList.setCellRenderer(new RoleListCellRenderer());
    }

    private void listProjects() {
        if(configManager.getSelection() != null && configManager.getSelection().isUser()) {
            User user = (User) configManager.getSelection();
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            ArrayList<Object> data = new ArrayList<>();
            data.add(new ProjectListHeaderItem());
            try {
                List<Project> projects = client.getProjects(user.getId());
                data.addAll(projects.stream().map(ProjectListItem::new).collect(Collectors.toList()));
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                Throwable t = e.getCause();
                if(t != null && t.getCause() != null && !(t.getCause() instanceof UserNotInPolicyException)) { // TODO revise
                    ErrorLogPanel.showErrorDialog(e);
                }
            }
            projectList.setListData(data.toArray());
        }
    }

    private void listRoles() {
        if(selectedProject != null && configManager.getSelection().isUser() && configManager.getPolicySelection().isProject()) {
            User user = (User)configManager.getSelection();
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            List<Role> roles = new ArrayList<>();
            try {
                roles = client.getRoles(user.getId(), selectedProject.getId());
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                e.printStackTrace();
            }
            ArrayList<Object> data = new ArrayList<>();
            data.add(new RoleListHeaderItem());
            data.addAll(roles.stream().map(RoleListItem::new).collect(Collectors.toList()));
            roleList.setListData(data.toArray());
        }
    }

    private void addProjectAssignment() {
        PolicyAssignmentDialogPanel.showDialog(editorKit, (User)configManager.getSelection());
        listProjects();
    }

    private void deleteProjectAssignment() {
        Object selectedObj = projectList.getSelectedValue();
        if (selectedObj instanceof ProjectListItem) {
            Project project = ((ProjectListItem) selectedObj).getProject();
            User user = (User)configManager.getSelection();
            String projectName = project.getName().get();
            int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Project Assignment '" + projectName + "'",
                    new JLabel("Proceed to delete permissions of user '" + user.getName().get() + "' on project '" + projectName + "'?\n" +
                            "All role assignments to '" + projectName + "' will be removed."),
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
            if (res != JOptionPane.OK_OPTION) {
                return;
            }
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            try {
                List<Role> roles = client.getRoles(user.getId(), project.getId());
                for(Role role : roles) {
                    client.retractRole(user.getId(), project.getId(), role.getId());
                }
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                ErrorLogPanel.showErrorDialog(e);
            }
            listProjects();
            listRoles();
        }
    }

    private void addRoleAssignment() {
        if(projectList.getSelectedValue() != null && projectList.getSelectedValue() instanceof ProjectListItem) {
            PolicyAssignmentDialogPanel.showDialog(editorKit, (User)configManager.getSelection(), ((ProjectListItem) projectList.getSelectedValue()).getProject());
            listRoles();
        }
    }

    private void deleteRoleAssignment() {
        Object selectedObj = roleList.getSelectedValue();
        if (selectedObj instanceof RoleListItem) {
            Role role = ((RoleListItem) selectedObj).getRole();
            User user = (User)configManager.getSelection();
            String roleName = role.getName().get();
            int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Role Assignment '" + roleName + "'",
                    new JLabel("Proceed to delete assignment of role '" + roleName + "' to user '" + user.getName().get() + "' on project '" +
                            selectedProject.getName().get() + "'?"),
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
            if (res != JOptionPane.OK_OPTION) {
                return;
            }
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            try {
                client.retractRole(user.getId(), selectedProject.getId(), role.getId());
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                ErrorLogPanel.showErrorDialog(e);
            }
            listProjects();
            listRoles();
        }
    }

    private void clearList(JList... lists) {
        for(JList list : lists) {
            list.setListData(Collections.emptyList().toArray());
        }
    }


    /**
     * Project list header item
     */
    public class ProjectListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Project Assignments";
        }

        @Override
        public boolean canAdd() {
            return true;
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
            return false;
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return true;
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return project.getName().get();
        }
    }

    /**
     * Role list header item
     */
    public class RoleListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Role Assignments";
        }

        @Override
        public boolean canAdd() {
            return true;
        }
    }

    /**
     * Role list item
     */
    public class RoleListItem implements RoleMListItem {
        private Role role;

        /**
         * Constructor
         *
         * @param role  Role
         */
        public RoleListItem(Role role) {
            this.role = checkNotNull(role);
        }

        @Override
        public Role getRole() {
            return role;
        }

        @Override
        public boolean isEditable() {
            return false;
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return true;
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return role.getName().get();
        }
    }

    @Override
    public void dispose() {
        projectList.removeListSelectionListener(projectListSelectionListener);
        roleList.removeListSelectionListener(roleListSelectionListener);
        configManager.removeListener(tabListener);
    }
}
