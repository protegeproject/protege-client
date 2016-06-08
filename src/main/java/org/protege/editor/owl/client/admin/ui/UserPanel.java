package org.protege.editor.owl.client.admin.ui;

import com.google.common.base.Objects;
import edu.stanford.protege.metaproject.api.User;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListItem;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
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
public class UserPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = 1495161684558867063L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList userList;
    private User selectedUser;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public UserPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        initUiComponents();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED)) {
            if (configManager.hasSelection() && !configManager.getSelection().isUser()) {
                userList.clearSelection();
            }
        }
    };

    private void initUiComponents() {
        setupList();
        setLayout(new BorderLayout());
        JScrollPane scrollpane = new JScrollPane(userList);
        scrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        add(scrollpane, BorderLayout.CENTER);
        listUsers();
    }

    private ListSelectionListener listSelectionListener = e -> {
        Object selectedObj = userList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof UserListItem) {
                selectedUser = ((UserListItem) selectedObj).getUser();
                configManager.setSelection(selectedUser);
            } else if (selectedObj instanceof UserListHeaderItem) {
                configManager.clearSelection();
            }
        }
    };

    private void setupList() {
        userList = new MList() {
            protected void handleAdd() {
                addUser();
            }

            protected void handleDelete() {
                deleteUser();
            }

            protected void handleEdit() {
                editUser();
            }
        };
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(listSelectionListener);
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setFixedCellHeight(30);
    }

    private void listUsers() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        ArrayList<Object> data = new ArrayList<>();
        data.add(new UserListHeaderItem());
        try {
            if(client != null) {
                List<User> users = client.getAllUsers();
                Collections.sort(users);
                data.addAll(users.stream().map(UserListItem::new).collect(Collectors.toList()));
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        userList.setListData(data.toArray());
    }

    private void addUser() {
        Optional<User> user = UserDialogPanel.showDialog(editorKit);
        if (user.isPresent()) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            listUsers();
            userList.setSelectedValue(new UserListItem(user.get()), true);
        }
    }

    private void editUser() {
        Optional<User> user = UserDialogPanel.showDialog(editorKit, selectedUser);
        if (user.isPresent()) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            listUsers();
            userList.setSelectedValue(new UserListItem(user.get()), true);
        }
    }

    private void deleteUser() {
        Object selectedObj = userList.getSelectedValue();
        if (selectedObj instanceof UserListItem) {
            User user = ((UserListItem) selectedObj).getUser();
            String userName = user.getName().get();
            int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete User '" + userName + "'",
                    new JLabel("Proceed to delete user '" + userName + "'?\n" + "All policy entries involving '" + userName + "' will be removed."),
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
            if (res != JOptionPane.OK_OPTION) {
                return;
            }
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            try {
                client.deleteUser(user.getId());
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                ErrorLogPanel.showErrorDialog(e);
            }
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            listUsers();
        }
    }


    /**
     * Add User item
     */
    public class UserListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Users";
        }

        @Override
        public boolean canAdd() {
            return true;
        }
    }

    /**
     * User list item
     */
    public class UserListItem implements MListItem {
        private User user;

        /**
         * Constructor
         *
         * @param user User
         */
        public UserListItem(User user) {
            this.user = checkNotNull(user);
        }

        public User getUser() {
            return user;
        }

        @Override
        public boolean isEditable() {
            return true;
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
            return user.getName().get() + "(" + user.getEmailAddress().get() + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserListItem)) {
                return false;
            }
            UserListItem that = (UserListItem) o;
            return Objects.equal(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(user);
        }
    }

    @Override
    public void dispose() {
        userList.removeListSelectionListener(listSelectionListener);
        configManager.removeListener(tabListener);
    }
}
