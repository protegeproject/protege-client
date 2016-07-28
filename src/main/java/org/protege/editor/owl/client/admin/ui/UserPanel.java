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
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
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
public class UserPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = 631771601957397823L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList userList;
    private User selectedUser;
    private ClientSession session;
    private Client client;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public UserPanel(OWLEditorKit editorKit) {
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
            if (configManager.hasSelection() && !configManager.getSelection().isUser()) {
                userList.clearSelection();
            }
        } else if (event.equals(AdminTabEvent.CONFIGURATION_RESET)) {
        	listUsers();
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.SWITCH_CLIENT) || event.hasCategory(ClientSessionChangeEvent.EventCategory.CLEAR_SESSION)) {
            client = session.getActiveClient();
            removeAll();
            initUi();
        }
    };

    private void initUi() {
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
        userList.addKeyListener(keyAdapter);
        userList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(userList.getSelectedValue() instanceof UserListHeaderItem) {
                    addUser();
                } else {
                    editUser();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(userList.getSelectedValue() instanceof UserListHeaderItem) {
                    addUser();
                } else {
                    editUser();
                }
            }
        }
    };

    private void listUsers() {
        ArrayList<Object> data = new ArrayList<>();
        data.add(new UserListHeaderItem());
        try {
            if(client != null) {
                List<User> users = client.getAllUsers();
                Collections.sort(users);
                data.addAll(users.stream().map(UserListItem::new).collect(Collectors.toList()));
            }
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        userList.setListData(data.toArray());
    }

    private void addUser() {
        if(client != null && client.canCreateUser()) {
            Optional<User> user = UserDialogPanel.showDialog(editorKit);
            if (user.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listUsers();
                userList.setSelectedValue(new UserListItem(user.get()), true);
            }
        }
    }

    private void editUser() {
        if(client != null && client.canUpdateUser()) {
            Optional<User> user = UserDialogPanel.showDialog(editorKit, selectedUser);
            if (user.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listUsers();
                userList.setSelectedValue(new UserListItem(user.get()), true);
            }
        }
    }

    private void deleteUser() {
        if(client != null && client.canDeleteUser()) {
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
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listUsers();
            }
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
            return (client != null && client.canCreateUser());
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
            return (client != null && client.canUpdateUser());
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return (client != null && client.canDeleteUser());
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
        session.removeListener(sessionListener);
    }
}
