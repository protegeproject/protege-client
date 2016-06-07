package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.impl.MetaprojectUtils;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.exception.PolicyEntryAlreadyExistsException;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PolicyAssignmentDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = 4493401748178536011L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private JComboBox<Project> projectBox;
    private JComboBox<Role> roleBox;
    private JLabel operationsLbl, roleLbl, projectLbl;
    private JList<Operation> operationsList;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private boolean currentlyValid = false, roleOnly = false;
    private User selectedUser;
    private Project selectedProject;
    private Role dialogSelectedRole;

    /**
     * Constructor
     */
    public PolicyAssignmentDialogPanel(OWLEditorKit editorKit, User selectedUser) {
        this(editorKit, selectedUser, false);
    }

    public PolicyAssignmentDialogPanel(OWLEditorKit editorKit, User selectedUser, boolean roleOnly) {
        this.editorKit = checkNotNull(editorKit);
        this.selectedUser = checkNotNull(selectedUser);
        this.roleOnly = checkNotNull(roleOnly);
        initInputFields();
        initUi(roleOnly);
    }

    private void initInputFields() {
        operationsLbl = new JLabel("Allowed operations:");
        roleLbl = new JLabel("Role");
        projectLbl = new JLabel("Project");

        projectBox = new JComboBox<>(getProjects());
        projectBox.setSelectedIndex(-1);
        roleBox = new JComboBox<>(getRoles());
        roleBox.setSelectedIndex(-1);
        operationsList = new JList<>();

        operationsList.setCellRenderer(new OperationListCellRenderer());
        projectBox.setRenderer(new MetaprojectObjectComboBoxRenderer());
        projectBox.addActionListener(e -> {
            if(e.getSource().equals(projectBox)) {
                selectedProject = (Project) projectBox.getSelectedItem();
                handleValueChange();
            }
        });
        roleBox.setRenderer(new MetaprojectObjectComboBoxRenderer());
        roleBox.addActionListener(e -> {
            if(e.getSource().equals(roleBox)) {
                dialogSelectedRole = (Role)roleBox.getSelectedItem();
                operationsList.setListData(getOperations(dialogSelectedRole));
                handleValueChange();
            }
        });
    }

    private void initUi(boolean roleOnly) {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);
        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;
        if(!roleOnly) {
            holderPanel.add(projectLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
            holderPanel.add(projectBox, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            rowIndex++;
        }
        holderPanel.add(roleLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(roleBox, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(new JSeparator(), new GridBagConstraints(0, rowIndex, 2, 1, 100.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 2), 0, 0));
        rowIndex++;
        holderPanel.add(operationsLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(new JScrollPane(operationsList), new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        holderPanel.add(errorArea, new GridBagConstraints(0, rowIndex, 2, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    public void setProject(Project project) {
        this.selectedProject = checkNotNull(project);
    }

    private Role[] getRoles() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        List<Role> roles = new ArrayList<>();
        try {
            roles = client.getAllRoles();
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        return roles.toArray(new Role[roles.size()]);
    }

    private Project[] getProjects() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        List<Project> projects = new ArrayList<>();
        try {
            projects = client.getAllProjects();
            projects.add(MetaprojectUtils.getUniversalProject());
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        return projects.toArray(new Project[projects.size()]);
    }

    private Operation[] getOperations(Role role) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        List<Operation> operations = new ArrayList<>();
        try {
            operations = client.getOperations(role.getId());
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        return operations.toArray(new Operation[operations.size()]);
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (PolicyEntryAlreadyExistsException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if(cause != null) {
                errorArea.setText(cause.getMessage());
            }
            else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws PolicyEntryAlreadyExistsException {
        boolean allValid = true;
        if(policyEntryExists()) {
            throw new PolicyEntryAlreadyExistsException("The specified policy entry already exists");
        }
        if (roleBox.getSelectedItem() == null) {
            allValid = false;
        }
        if(!roleOnly && projectBox.getSelectedItem() == null) {
            allValid = false;
        }
        return allValid;
    }

    private boolean policyEntryExists() {
        if(selectedProject != null && selectedUser != null && dialogSelectedRole != null) {
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            try {
                List<Role> roles = client.getRoles(selectedUser.getId(), selectedProject.getId());
                for(Role role : roles) {
                    if(role.getId().equals(dialogSelectedRole.getId())) {
                        return true;
                    }
                }
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                return false;
            }
        }
        return false;
    }

    private void addAssignment(Project project) {
        Role role = (Role) roleBox.getSelectedItem();
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.assignRole(selectedUser.getId(), project.getId(), role.getId());
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    // Add assignment when a project is selected in the UI
    private Project addAssignment() {
        addAssignment(selectedProject);
        return selectedProject;
    }

    public static Optional<Project> showDialog(OWLEditorKit editorKit, User selectedUser) {
        PolicyAssignmentDialogPanel panel = new PolicyAssignmentDialogPanel(editorKit, selectedUser);
        boolean add = showDialog(editorKit, panel, "Add New Access Policy");
        if(add) {
            return Optional.of(panel.addAssignment());
        }
        return Optional.empty();
    }

    public static void showDialog(OWLEditorKit editorKit, User selectedUser, Project selectedProject) {
        PolicyAssignmentDialogPanel panel = new PolicyAssignmentDialogPanel(editorKit, selectedUser, true);
        panel.setProject(selectedProject);
        boolean add = showDialog(editorKit, panel, "Add New Access Policy within Project '" + selectedProject.getName().get() + "'");
        if(add) {
            panel.addAssignment(selectedProject);
        }
    }

    private static boolean showDialog(OWLEditorKit editorKit, PolicyAssignmentDialogPanel panel, String header) {
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), header, panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        return response == JOptionPane.OK_OPTION;
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners) {
            l.verifiedStatusChanged(currentlyValid);
        }
    }

    @Override
    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.add(listener);
        listener.verifiedStatusChanged(currentlyValid);
    }

    @Override
    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.remove(listener);
    }
}
