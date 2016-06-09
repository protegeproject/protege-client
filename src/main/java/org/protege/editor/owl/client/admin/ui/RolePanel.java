package org.protege.editor.owl.client.admin.ui;

import com.google.common.base.Objects;
import edu.stanford.protege.metaproject.api.Role;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.admin.model.RoleMListItem;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
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
public class RolePanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -6884136427422368948L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList roleList;
    private Role selectedRole;
    private ClientSession session;
    private Client client;

    /**
     * Constructor
     *
     * @param editorKit    OWL editor kit
     */
    public RolePanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        session = ClientSession.getInstance(editorKit);
        session.addListener(sessionListener);
        client = session.getActiveClient();
        initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED)) {
            if(configManager.hasSelection() && !configManager.getSelection().isRole()) {
                roleList.clearSelection();
            }
        }
    };

    private ClientSessionListener sessionListener = event -> {
        client = session.getActiveClient();
        removeAll();
        initUi();
    };

    private void initUi() {
        setupList();
        setLayout(new BorderLayout());
        JScrollPane scrollpane = new JScrollPane(roleList);
        scrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        add(scrollpane, BorderLayout.CENTER);
        listRoles();
    }

    private ListSelectionListener listSelectionListener = e -> {
        Object selectedObj = roleList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof RoleListItem) {
                selectedRole = ((RoleListItem) selectedObj).getRole();
                configManager.setSelection(selectedRole);
            }
            else if (selectedObj instanceof RoleListHeaderItem) {
                configManager.clearSelection();
            }
        }
    };

    private void setupList() {
        roleList = new MList() {
            protected void handleAdd() {
                addRole();
            }

            protected void handleDelete() {
                deleteRole();
            }

            protected void handleEdit() {
                editRole();
            }
        };
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.addListSelectionListener(listSelectionListener);
        roleList.setCellRenderer(new RoleListCellRenderer());
        roleList.addKeyListener(keyAdapter);
        roleList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(roleList.getSelectedValue() instanceof RoleListHeaderItem) {
                    addRole();
                } else {
                    editRole();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(roleList.getSelectedValue() instanceof RoleListHeaderItem) {
                    addRole();
                } else {
                    editRole();
                }
            }
        }
    };

    private void listRoles() {
        ArrayList<Object> data = new ArrayList<>();
        data.add(new RoleListHeaderItem());
        try {
            if(client != null) {
                List<Role> roles = client.getAllRoles();
                Collections.sort(roles);
                data.addAll(roles.stream().map(RoleListItem::new).collect(Collectors.toList()));
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        roleList.setListData(data.toArray());
    }

    private void addRole() {
        if(client != null && client.canCreateRole()) {
            Optional<Role> role = RoleDialogPanel.showDialog(editorKit);
            if (role.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listRoles();
                roleList.setSelectedValue(new RoleListItem(role.get()), true);
            }
        }
    }

    private void editRole() {
        if(client != null && client.canUpdateRole()) {
            Optional<Role> role = RoleDialogPanel.showDialog(editorKit, selectedRole);
            if (role.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listRoles();
                roleList.setSelectedValue(new RoleListItem(role.get()), true);
            }
        }
    }

    private void deleteRole() {
        if(client != null && client.canDeleteRole()) {
            Object selectedObj = roleList.getSelectedValue();
            if (selectedObj instanceof RoleListItem) {
                Role role = ((RoleListItem) selectedObj).getRole();
                String roleName = role.getName().get();
                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Role '" + roleName + "'",
                        new JLabel("Proceed to delete role '" + roleName + "'? All policy entries involving '" + roleName + "' will be removed."),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    client.deleteRole(role.getId());
                } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listRoles();
            }
        }
    }


    /**
     * Add Role item
     */
    public class RoleListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Roles";
        }

        @Override
        public boolean canAdd() {
            return (client != null && client.canCreateRole());
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
            return (client != null && client.canUpdateRole());
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return (client != null && client.canDeleteRole());
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return role.getName().get();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RoleListItem)) {
                return false;
            }
            RoleListItem that = (RoleListItem) o;
            return Objects.equal(role, that.role);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(role);
        }
    }

    @Override
    public void dispose() {
        roleList.removeListSelectionListener(listSelectionListener);
        configManager.removeListener(tabListener);
        session.removeListener(sessionListener);
    }
}
