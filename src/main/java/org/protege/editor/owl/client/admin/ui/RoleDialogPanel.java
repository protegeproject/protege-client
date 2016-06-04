package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationId;
import edu.stanford.protege.metaproject.api.Role;
import edu.stanford.protege.metaproject.api.exception.IdAlreadyInUseException;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RoleDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = -1259252313948444328L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField name;
    private AugmentedJTextArea description;
    private JTextField id;
    private JLabel idLbl, nameLbl, descriptionLbl, operationsLbl;
    private Set<OperationId> operations = new HashSet<>();
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private CheckBoxList<AugmentedJCheckBox<Operation>> operationCheckboxList = new CheckBoxList<>();
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private boolean currentlyValid = false;
    private Role selectedRole;

    /**
     * Constructor
     */
    public RoleDialogPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        id = new JTextField(FIELD_WIDTH);
        name = new AugmentedJTextField(FIELD_WIDTH, "Role name");
        description = new AugmentedJTextArea(4, FIELD_WIDTH, "Role description");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(GuiUtils.EMPTY_BORDER);

        Insets insets = id.getBorder().getBorderInsets(id);
        id.setText(UUID.randomUUID().toString());
        id.setEditable(false);
        id.setBorder(new EmptyBorder(insets));
        id.setBackground(getBackground());
        id.setForeground(Color.GRAY.darker().darker());

        idLbl = new JLabel("Role Id:");
        nameLbl = new JLabel("Name:");
        descriptionLbl = new JLabel("Description:");
        operationsLbl = new JLabel("Operations allowed");

        ListSelectionListener listener = e -> handleValueChange();
        addListener(id.getDocument());
        addListener(name.getDocument());
        addListener(description.getDocument());
        operationCheckboxList.addListSelectionListener(listener);
        initList();
    }

    private void initUi() {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);

        JScrollPane descriptionScrollPane = new JScrollPane(description);
        descriptionScrollPane.setBorder(GuiUtils.MATTE_BORDER);

        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;
        holderPanel.add(idLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(id, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(nameLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(name, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(descriptionLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(descriptionScrollPane, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(new JSeparator(), new GridBagConstraints(0, rowIndex, 2, 1, 100.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 2), 0, 0));
        rowIndex++;
        holderPanel.add(operationsLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(operationCheckboxList);
        panel.add(scrollPane, BorderLayout.CENTER);
        holderPanel.add(panel, new GridBagConstraints(0, rowIndex, 2, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        holderPanel.add(errorArea, new GridBagConstraints(0, rowIndex, 2, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
        operationCheckboxList.setVisibleRowCount(20);
    }

    private void initList() {
        List<AugmentedJCheckBox<Operation>> list = new ArrayList<>();
        List<Operation> operationList = getOperations();
        if (operationList != null) {
            Collections.sort(operationList);
            list.addAll(operationList.stream().map(AugmentedJCheckBox::new).collect(Collectors.toList()));
            operationCheckboxList.setListData(list.toArray(new AugmentedJCheckBox[list.size()]));
        }
    }

    private void setIsEditing(Role role) {
        selectedRole = checkNotNull(role);
        id.setText(role.getId().get());
        name.setText(role.getName().get());
        description.setText(role.getDescription().get());
        for(OperationId op : role.getOperations()) {
            for (int i = 0; i < operationCheckboxList.getModel().getSize(); i++) {
                AugmentedJCheckBox<Operation> cb = operationCheckboxList.getModel().getElementAt(i);
                if(cb.getObject().getId().equals(op)) {
                    cb.setSelected(true);
                }
            }
        }
    }

    private List<Operation> getOperations() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            return client.getAllOperations();
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        return null;
    }

    private void addListener(Document doc) {
        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void removeUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void changedUpdate(DocumentEvent e) {
                handleValueChange();
            }
        });
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (IdAlreadyInUseException e) {
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

    private boolean checkInputs() throws IdAlreadyInUseException {
        boolean allValid = true;
        if (name.getText().trim().isEmpty()) {
            allValid = false;
        }
        if (description.getText().trim().isEmpty()) {
            allValid = false;
        }
        boolean noneChecked = true;
        for (int i = 0; i < operationCheckboxList.getModel().getSize(); i++) {
            AugmentedJCheckBox<Operation> cb = operationCheckboxList.getModel().getElementAt(i);
            if (cb.isSelected()) {
                noneChecked = false;
                break;
            }
        }
        if (noneChecked) {
            allValid = false;
        }
        return allValid;
    }

    private Role createRole() {
        MetaprojectFactory f = Manager.getFactory();
        for (int i = 0; i < operationCheckboxList.getModel().getSize(); i++) {
            AugmentedJCheckBox<Operation> cb = operationCheckboxList.getModel().getElementAt(i);
            if (cb.isSelected()) {
                operations.add(cb.getObject().getId());
            }
        }
        return f.getRole(f.getRoleId(id.getText()), f.getName(name.getText()), f.getDescription(description.getText()), operations);
    }

    private void addRole(Role role) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.createRole(role);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void updateRole(Role role) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.updateRole(selectedRole.getId(), role);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    public static Optional<Role> showDialog(OWLEditorKit editorKit) {
        RoleDialogPanel panel = new RoleDialogPanel(editorKit);
        Optional<Role> role = showDialog(editorKit, panel, "Add New Role");
        if(role.isPresent()) {
            panel.addRole(role.get());
            return Optional.of(role.get());
        }
        return Optional.empty();
    }

    public static Optional<Role> showDialog(OWLEditorKit editorKit, Role selectedRole) {
        RoleDialogPanel panel = new RoleDialogPanel(editorKit);
        panel.setIsEditing(selectedRole);
        Optional<Role> role = showDialog(editorKit, panel, "Edit Role '" + selectedRole.getName().get() + "'");
        if(role.isPresent()) {
            panel.updateRole(role.get());
            return Optional.of(role.get());
        }
        return Optional.empty();
    }

    private static Optional<Role> showDialog(OWLEditorKit editorKit, RoleDialogPanel panel, String header) {
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), header, panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createRole());
        }
        return Optional.empty();
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
