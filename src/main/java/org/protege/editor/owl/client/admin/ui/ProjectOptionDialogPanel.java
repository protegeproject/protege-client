package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.admin.exception.ProjectOptionAlreadyExistsException;
import org.protege.editor.owl.client.admin.model.ProjectOption;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.UIHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOptionDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = -5212405369962121458L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField key;
    private JLabel keyLbl, valueLbl;
    private JList<String> values;
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JButton addValueBtn, removeValueBtn, editValueBtn;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH * 2);
    private boolean currentlyValid = false, isEditing = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private String selectedKey, selectedValue;
    private Set<String> projectOptionKeys;

    /**
     * Constructor
     */
    public ProjectOptionDialogPanel(OWLEditorKit editorKit, Set<String> projectOptionKeys) {
        this.editorKit = checkNotNull(editorKit);
        this.projectOptionKeys = checkNotNull(projectOptionKeys);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        key = new AugmentedJTextField(FIELD_WIDTH, "Option key");
        values = new JList<>(listModel);
        values.addListSelectionListener(listSelectionListener);

        keyLbl = new JLabel("Key:");
        valueLbl = new JLabel("Value(s):");

        addListener(key);
    }

    private void initUi() {
        setLayout(new GridBagLayout());
        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;

        add(keyLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        add(key, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        add(valueLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        add(new JScrollPane(values), new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.BOTH, new Insets(0, 2, 0, 2), 0, 0));
        rowIndex++;

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addValueBtn = new JButton(GuiUtils.getIcon("plus.png", 14, 14));
        removeValueBtn = new JButton(GuiUtils.getIcon("minus.png", 14, 14));
        editValueBtn = new JButton(GuiUtils.getIcon("edit.png", 16, 16));
        enableButton(false, removeValueBtn, editValueBtn);

        addValueBtn.setPreferredSize(new Dimension(30, 30));
        addValueBtn.setToolTipText("Add new value");
        addValueBtn.addActionListener(addValueBtnListener);

        removeValueBtn.setPreferredSize(new Dimension(30, 30));
        removeValueBtn.addActionListener(removeValueBtnListener);
        removeValueBtn.setToolTipText("Remove selected value");

        editValueBtn.setPreferredSize(new Dimension(30, 30));
        editValueBtn.addActionListener(editValueBtnListener);
        editValueBtn.setToolTipText("Edit selected value");

        buttonsPanel.add(addValueBtn);
        buttonsPanel.add(removeValueBtn);
        buttonsPanel.add(editValueBtn);

        add(buttonsPanel, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;

        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        add(errorArea, new GridBagConstraints(0, rowIndex, 2, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    private ListSelectionListener listSelectionListener = e -> {
        String selectedObj = values.getSelectedValue();
        if (selectedObj != null) {
            if(values.getSelectedIndices().length > 1) {
                if(!removeValueBtn.isEnabled()) {
                    enableButton(true, removeValueBtn);
                }
                if(editValueBtn.isEnabled()) {
                    enableButton(false, editValueBtn);
                }
            } else {
                if(!removeValueBtn.isEnabled()) {
                    enableButton(true, removeValueBtn);
                }
                if(!editValueBtn.isEnabled()) {
                    enableButton(true, editValueBtn);
                }
                selectedValue = values.getSelectedValue();
            }
        }
        else {
            enableButton(false, removeValueBtn, editValueBtn);
        }
    };

    private ActionListener addValueBtnListener = e -> {
        Optional<String> inputValue = showValueDialog("", "Add New Project Option Value");
        if(inputValue.isPresent()) {
            addValue(inputValue.get());
        }
    };

    private ActionListener editValueBtnListener = e -> {
        Optional<String> inputValue = showValueDialog(selectedValue, "Edit Project Option Value");
        if(inputValue.isPresent()) {
            updateValue(selectedValue, inputValue.get());
        }
    };

    private ActionListener removeValueBtnListener = e -> removeValue();

    private void updateValue(String oldValue, String newValue) {
        checkNotNull(oldValue);
        checkNotNull(newValue);
        List<String> values = getValues();
        values.remove(oldValue);
        values.add(newValue);
        Collections.sort(values);
        this.values.setListData(values.toArray(new String[values.size()]));
        handleValueChange();
    }

    private void addValue(String value) {
        checkNotNull(value);
        if(!value.isEmpty()) {
            List<String> values = getValues();
            values.add(value);
            Collections.sort(values);
            this.values.setListData(values.toArray(new String[values.size()]));
            handleValueChange();
        }
    }

    private void removeValue() {
        List<String> values = getValues();
        this.values.getSelectedValuesList().forEach(values::remove);
        Collections.sort(values);
        this.values.setListData(values.toArray(new String[values.size()]));
        handleValueChange();
    }

    private Optional<String> showValueDialog(String value, String dialogHeader) {
        JPanel valuePanel = new JPanel(new GridBagLayout());
        AugmentedJTextField valueField = new AugmentedJTextField(FIELD_WIDTH*2, "Project option value");
        if(!value.isEmpty()) {
            valueField.setText(value);
        }
        Insets insets = new Insets(0, 2, 2, 2);
        valuePanel.add(new JLabel("Value:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        valuePanel.add(valueField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));

        int response = JOptionPaneEx.showConfirmDialog(
                editorKit.getOWLWorkspace(), dialogHeader, valuePanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(valueField.getText());
        }
        return Optional.empty();
    }

    private void setIsEditing(String key, Set<String> values) {
        selectedKey = checkNotNull(key);
        isEditing = true;

        this.key.setText(selectedKey);
        this.values.setListData(values.toArray(new String[values.size()]));
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
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
        } catch (ProjectOptionAlreadyExistsException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if (cause != null) {
                errorArea.setText(cause.getMessage());
            } else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws ProjectOptionAlreadyExistsException {
        boolean allValid = true;
        if (key.getText().trim().isEmpty()) {
            allValid = false;
        }
        if (getValues().isEmpty()) {
            allValid = false;
        }
        if ((!isEditing && exists(key.getText())) || (isEditing && !key.getText().equals(selectedKey) && exists(key.getText()))) {
            throw new ProjectOptionAlreadyExistsException("A project option with the given key already exists");
        }
        return allValid;
    }

    private boolean exists(String key) {
        return projectOptionKeys.contains(key);
    }

    private ProjectOption createOption() {
        return new ProjectOption(key.getText(), getValues());
    }

    private List<String> getValues() {
        ListModel<String> model = values.getModel();
        List<String> values = new ArrayList<>();
        for(int i = 0; i < model.getSize(); i++) {
            values.add(model.getElementAt(i));
        }
        return values;
    }

    public static Optional<ProjectOption> showDialog(OWLEditorKit editorKit, Set<String> projectOptionKeys) {
        ProjectOptionDialogPanel panel = new ProjectOptionDialogPanel(editorKit, projectOptionKeys);
        int response = new UIHelper(editorKit).showValidatingDialog("Add New Project Option", panel, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createOption());
        }
        return Optional.empty();
    }

    public static Optional<ProjectOption> showDialog(OWLEditorKit editorKit, Set<String> projectOptionKeys, String key, Set<String> values) {
        ProjectOptionDialogPanel panel = new ProjectOptionDialogPanel(editorKit, projectOptionKeys);
        panel.setIsEditing(key, values);
        int response = new UIHelper(editorKit).showValidatingDialog("Edit Project Option", panel, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createOption());
        }
        return Optional.empty();
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners) {
            l.verifiedStatusChanged(currentlyValid);
        }
    }

    private void enableButton(boolean enable, JButton... buttons) {
        for (JButton button : buttons) {
            button.setEnabled(enable);
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
