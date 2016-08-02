package org.protege.editor.owl.client.admin.ui;

import com.google.common.base.Objects;
import edu.stanford.protege.metaproject.api.Operation;
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
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;

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
public class OperationPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -6374121042315963259L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList operationList;
    private Operation selectedOperation;
    private ClientSession session;
    private Client client;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public OperationPanel(OWLEditorKit editorKit) {
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
            if(configManager.hasSelection() && !configManager.getSelection().isOperation()) {
                operationList.clearSelection();
            }
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGIN) || event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGOUT)) {
            client = session.getActiveClient();
            removeAll();
            initUi();
        }
    };

    private void initUi() {
        setupList();
        setLayout(new BorderLayout());
        JScrollPane scrollpane = new JScrollPane(operationList);
        scrollpane.setBorder(new EmptyBorder(3, 0, 0, 0));
        add(scrollpane, BorderLayout.CENTER);
        listOperations();
    }

    private ListSelectionListener listSelectionListener = e -> {
        Object selectedObj = operationList.getSelectedValue();
        if (selectedObj != null && !e.getValueIsAdjusting()) {
            if (selectedObj instanceof OperationListItem) {
                selectedOperation = ((OperationListItem) selectedObj).getOperation();
                configManager.setSelection(selectedOperation);
            }
            else if (selectedObj instanceof OperationListHeaderItem) {
                configManager.clearSelection();
            }
        }
    };

    private void setupList() {
        operationList = new MList() {
            protected void handleAdd() {
                addOperation();
            }

            protected void handleDelete() {
                deleteOperation();
            }

            protected void handleEdit() {
                editOperation();
            }
        };
        operationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        operationList.addListSelectionListener(listSelectionListener);
        operationList.setCellRenderer(new OperationListCellRenderer());
        operationList.addKeyListener(keyAdapter);
        operationList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                if(operationList.getSelectedValue() instanceof OperationListHeaderItem) {
                    addOperation();
                } else {
                    editOperation();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                if(operationList.getSelectedValue() instanceof OperationListHeaderItem) {
                    addOperation();
                } else {
                    editOperation();
                }
            }
        }
    };

    private void listOperations() {
        ArrayList<Object> data = new ArrayList<>();
        data.add(new OperationListHeaderItem());
        try {
            if(client != null) {
                List<Operation> operations = client.getAllOperations();
                Collections.sort(operations);
                data.addAll(operations.stream().map(OperationListItem::new).collect(Collectors.toList()));
            }
        } catch (ClientRequestException | AuthorizationException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        operationList.setListData(data.toArray());
    }

    private void addOperation() {
        if(client != null && client.canCreateOperation()) {
            Optional<Operation> operation = OperationDialogPanel.showDialog(editorKit);
            if (operation.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listOperations();
                operationList.setSelectedValue(new OperationListItem(operation.get()), true);
            }
        }
    }

    private void editOperation() {
        if(client != null && client.canUpdateOperation() && !selectedOperation.isSystemOperation()) {
            Optional<Operation> operation = OperationDialogPanel.showDialog(editorKit, selectedOperation);
            if (operation.isPresent()) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listOperations();
                operationList.setSelectedValue(new OperationListItem(operation.get()), true);
            }
        }
    }

    private void deleteOperation() {
        if(client != null && client.canDeleteOperation()) {
            Object selectedObj = operationList.getSelectedValue();
            if (selectedObj instanceof OperationListItem) {
                Operation operation = ((OperationListItem) selectedObj).getOperation();
                String operationName = operation.getName().get();
                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Operation '" + operationName + "'",
                        new JLabel("Proceed to delete operation '" + operationName + "'? All policy entries involving '" + operationName + "' will be removed."),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    client.deleteOperation(operation.getId());
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                listOperations();
            }
        }
    }


    /**
     * Add Operation item
     */
    public class OperationListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Operations";
        }

        @Override
        public boolean canAdd() {
            return (client != null && client.canCreateOperation());
        }
    }

    /**
     * Operation list item
     */
    public class OperationListItem implements MListItem {
        private Operation operation;

        /**
         * Constructor
         *
         * @param operation Operation
         */
        public OperationListItem(Operation operation) {
            this.operation = checkNotNull(operation);
        }

        public Operation getOperation() {
            return operation;
        }

        @Override
        public boolean isEditable() {
            return (!operation.isSystemOperation() && client != null && client.canUpdateOperation());
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return (!operation.isSystemOperation() && client != null && client.canDeleteOperation());
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return operation.getName().get();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof OperationListItem)) {
                return false;
            }
            OperationListItem that = (OperationListItem) o;
            return Objects.equal(operation, that.operation);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(operation);
        }
    }

    @Override
    public void dispose() {
        operationList.removeListSelectionListener(listSelectionListener);
        configManager.removeListener(tabListener);
        session.removeListener(sessionListener);
    }
}
