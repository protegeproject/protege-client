package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.GlobalPermissions;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.User;
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
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.Config;

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
public class PolicyPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -5616803874073649334L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList projectList, roleList;
    private Project selectedProject;
    private ClientSession session;
    private Config config = null;

    /**
     * Constructor
     *
     * @param editorKit    OWL editor kit
     */
    public PolicyPanel(OWLEditorKit editorKit) {
    	this.editorKit = checkNotNull(editorKit);
    	configManager = AdminTabManager.get(editorKit);
    	configManager.addListener(tabListener);
    	session = ClientSession.getInstance(editorKit);
    	session.addListener(sessionListener);
    	if ( session.getActiveClient() != null) {
    		config = session.getActiveClient().getConfig();
    	}
    	initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED) ||
        		(event.equals(AdminTabEvent.CONFIGURATION_CHANGED))) {
        	config = session.getActiveClient().getConfig();
            projectList.clearSelection();
            roleList.clearSelection();
            clearList(roleList, projectList);
            listProjects();
        } else if(event.equals(AdminTabEvent.POLICY_ITEM_SELECTION_CHANGED)) {
            if(configManager.getPolicySelection() != null && configManager.getPolicySelection().isProject()) {
                clearList(roleList);
                listRoles();
            }
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGIN) || event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGOUT)) {
            if (session.getActiveClient() != null) {
            	config = session.getActiveClient().getConfig();
            } else {
            	config = null;
            }
            removeAll();
            initUi();
        }
    };

    private void initUi() {
        setBackground(getBackground());
        setLayout(new BorderLayout());
        setupProjectList();
        setupRoleList();

        JPanel projectPanel = new JPanel(new BorderLayout());
        JScrollPane projectScrollpane = new JScrollPane(projectList);
        projectScrollpane.setBorder(GuiUtils.MATTE_BORDER);
        projectPanel.add(projectScrollpane, BorderLayout.CENTER);

        JPanel rolePanel = new JPanel(new BorderLayout());
        JScrollPane roleScrollpane = new JScrollPane(roleList);
        roleScrollpane.setBorder(GuiUtils.MATTE_BORDER);
        rolePanel.add(roleScrollpane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectPanel, rolePanel);
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
        projectList.setBorder(new EmptyBorder(3, 0, 0, 0));
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.addListSelectionListener(projectListSelectionListener);
        projectList.setCellRenderer(new ProjectListCellRenderer());
        projectList.addKeyListener(keyAdapter);
        projectList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(projectList.getSelectedValue() instanceof ProjectListHeaderItem) {
                    addProjectAssignment();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(projectList.getSelectedValue() instanceof ProjectListHeaderItem) {
                    addProjectAssignment();
                }
            }
        }
    };

    private void setupRoleList() {
        roleList = new MList() {
            protected void handleAdd() {
                addRoleAssignment();
            }

            protected void handleDelete() {
                deleteRoleAssignment();
            }
        };
        roleList.setBorder(new EmptyBorder(3, 0, 0, 0));
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.addListSelectionListener(roleListSelectionListener);
        roleList.setCellRenderer(new RoleListCellRenderer());
        roleList.addKeyListener(roleKeyAdapter);
        roleList.addMouseListener(roleMouseAdapter);
    }

    private KeyAdapter roleKeyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(roleList.getSelectedValue() instanceof RoleListHeaderItem) {
                    addRoleAssignment();
                }
            }
        }
    };

    private MouseAdapter roleMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(roleList.getSelectedValue() instanceof RoleListHeaderItem) {
                    addRoleAssignment();
                }
            }
        }
    };

    private void listProjects() {
        if(configManager.getSelection() != null && configManager.getSelection().isUser()) {
            User user = (User) configManager.getSelection();
            ArrayList<Object> data = new ArrayList<>();
            data.add(new ProjectListHeaderItem());
            if (config != null) {
			    List<Project> projects = config.getProjects(user.getId());
			    Collections.sort(projects);
			    data.addAll(projects.stream().map(ProjectListItem::new).collect(Collectors.toList()));
			}
            projectList.setListData(data.toArray());
        }
    }

    private void listRoles() {
        if(selectedProject != null && configManager.getSelection().isUser()) {
            User user = (User)configManager.getSelection();
            ArrayList<Object> data = new ArrayList<>();
            data.add(new RoleListHeaderItem());
            if (config != null) {
			    List<Role> roles = config.getRoles(user.getId(), selectedProject.getId(), GlobalPermissions.EXCLUDED);
			    Collections.sort(roles);
			    data.addAll(roles.stream().map(RoleListItem::new).collect(Collectors.toList()));
			}
            roleList.setListData(data.toArray());
        }
    }

    private void handleException(Exception e) {
        Throwable t = e.getCause();
        if(t != null && t.getCause() != null) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void addProjectAssignment() {
        if(config != null && config.canAssignRole()) {
            Optional<Project> project = PolicyAssignmentDialogPanel.showDialog(editorKit, (User) configManager.getSelection());
            if (project.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listProjects();
                selectProject(project.get());
            }

        }
    }

    private void deleteProjectAssignment() {
        if(config != null && config.canRetractRole()) {
            Object selectedObj = projectList.getSelectedValue();
            if (selectedObj instanceof ProjectListItem) {
                Project project = ((ProjectListItem) selectedObj).getProject();
                User user = (User) configManager.getSelection();
                String projectName = project.getName().get();
                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Project Assignment '" + projectName + "'",
                        new JLabel("Proceed to delete permissions of user '" + user.getName().get() + "' on project '" + projectName + "'?\n" +
                                "All role assignments to '" + projectName + "' will be removed."),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    List<Role> roles = config.getRoles(user.getId(), project.getId(), GlobalPermissions.EXCLUDED);
                    for (Role role : roles) {
                        config.retractRole(user.getId(), project.getId(), role.getId());
                        configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                    }
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                clearList(projectList, roleList);
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

    private void addRoleAssignment() {
        if(config != null && config.canAssignRole()) {
            if (projectList.getSelectedValue() != null && projectList.getSelectedValue() instanceof ProjectListItem) {
                Project project = ((ProjectListItem) projectList.getSelectedValue()).getProject();
                boolean added = PolicyAssignmentDialogPanel.showDialog(editorKit, (User) configManager.getSelection(), project);
                if(added) {
                    configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                    listRoles();
                    selectProject(project);
                }
            }
        }
    }

    private void deleteRoleAssignment() {
        if(config != null && config.canRetractRole()) {
            Object selectedObj = roleList.getSelectedValue();
            if (selectedObj instanceof RoleListItem) {
                Role role = ((RoleListItem) selectedObj).getRole();
                User user = (User) configManager.getSelection();
                String roleName = role.getName().get();
                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Role Assignment '" + roleName + "'",
                        new JLabel("Proceed to delete assignment of role '" + roleName + "' to user '" + user.getName().get() + "' on project '" +
                                selectedProject.getName().get() + "'?"),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    config.retractRole(user.getId(), selectedProject.getId(), role.getId());
                    configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                listProjects();
                listRoles();
                selectProject(selectedProject);
            }
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
            return (config != null && config.canAssignRole());
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
            return (config != null && config.canRetractRole());
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
            return (config != null && config.canAssignRole());
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
            return (config != null && config.canRetractRole());
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
        session.removeListener(sessionListener);
    }
}
