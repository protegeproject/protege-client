package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.MetaprojectFactory;
import edu.stanford.protege.metaproject.api.Operation;
import edu.stanford.protege.metaproject.api.OperationType;
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
import javax.swing.text.Document;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OperationDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = -3827309465532294196L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField name;
    private AugmentedJTextArea description;
    private JTextField id;
    private JComboBox<OperationType> typesBox = new JComboBox<>(OperationType.values());
    private JLabel idLbl, nameLbl, descriptionLbl, typeLbl;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private boolean currentlyValid = false;
    private Operation selectedOperation;

    /**
     * Constructor
     */
    public OperationDialogPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        id = new JTextField(FIELD_WIDTH);
        name = new AugmentedJTextField(FIELD_WIDTH, "Operation name");
        description = new AugmentedJTextArea(4, FIELD_WIDTH, "Operation description");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(GuiUtils.EMPTY_BORDER);

        Insets insets = id.getBorder().getBorderInsets(id);
        id.setText(UUID.randomUUID().toString());
        id.setEditable(false);
        id.setBorder(new EmptyBorder(insets));
        id.setBackground(getBackground());
        id.setForeground(Color.GRAY.darker().darker());

        idLbl = new JLabel("Operation Id:");
        nameLbl = new JLabel("Name:");
        descriptionLbl = new JLabel("Description:");
        typeLbl = new JLabel("Type:");

        addListener(id.getDocument());
        addListener(name.getDocument());
        addListener(description.getDocument());
    }

    private void initUi() {
        JPanel holderPanel = new JPanel(new GridBagLayout());
        add(holderPanel);
        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;
        JScrollPane descriptionScrollPane = new JScrollPane(description);
        descriptionScrollPane.setBorder(GuiUtils.MATTE_BORDER);
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
        holderPanel.add(typeLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(typesBox, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
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

    private void setIsEditing(Operation operation) {
        selectedOperation = checkNotNull(operation);
        id.setText(operation.getId().get());
        name.setText(operation.getName().get());
        description.setText(operation.getDescription().get());
        typesBox.setSelectedItem(operation.getType());
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
        if(typesBox.getSelectedItem() == null) {
            allValid = false;
        }
        return allValid;
    }

    private Operation createOperation() {
        MetaprojectFactory f = Manager.getFactory();
        return f.getCustomOperation(f.getOperationId(id.getText()), f.getName(name.getText()), f.getDescription(description.getText()),
                (OperationType)typesBox.getSelectedItem(), Operation.Scope.ONTOLOGY);
    }

    private void addOperation(Operation operation) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.createOperation(operation);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void update(Operation operation) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.updateOperation(selectedOperation.getId(), operation);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    public static Optional<Operation> showDialog(OWLEditorKit editorKit) {
        OperationDialogPanel panel = new OperationDialogPanel(editorKit);
        Optional<Operation> operation = showDialog(editorKit, panel, "Add New Operation");
        if(operation.isPresent()) {
            panel.addOperation(operation.get());
            return Optional.of(operation.get());
        }
        return Optional.empty();
    }

    public static Optional<Operation> showDialog(OWLEditorKit editorKit, Operation selectedOperation) {
        OperationDialogPanel panel = new OperationDialogPanel(editorKit);
        panel.setIsEditing(selectedOperation);
        Optional<Operation> operation = showDialog(editorKit, panel, "Edit Operation '" + selectedOperation.getName().get() + "'");
        if(operation.isPresent()) {
            panel.update(operation.get());
            return Optional.of(operation.get());
        }
        return Optional.empty();
    }

    private static Optional<Operation> showDialog(OWLEditorKit editorKit, OperationDialogPanel panel, String header) {
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), header, panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createOperation());
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
