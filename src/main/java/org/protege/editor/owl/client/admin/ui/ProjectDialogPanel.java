package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.Manager;
import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.*;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.admin.exception.InvalidInputFileException;
import org.protege.editor.owl.client.admin.model.ProjectOption;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.ui.UIHelper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = 2010269799445634450L;
    private OWLEditorKit editorKit;
    private static final int FIELD_WIDTH = 20;
    private AugmentedJTextField name;
    private AugmentedJTextArea description;
    private JTextField id;
    private JLabel idLbl, nameLbl, descriptionLbl, fileLbl, fileSelectionLbl, ownerLbl;
    private JComboBox<User> ownerSelection;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH * 2);
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private JButton fileBtn = new JButton("Browse");
    private JButton addOptionBtn, removeOptionBtn, editOptionBtn;
    private Map<String, Set<String>> projectOptions = new HashMap<>();
    private File file;
    private UserId ownerId;
    private boolean currentlyValid = false, isEditing = false;
    private ProjectOptionsTableModel optionsTableModel;
    private JTable optionsTable;
    private Project selectedProject;
    private List<String> selectedOptionKeys = new ArrayList<>();

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public ProjectDialogPanel(OWLEditorKit editorKit) {
        this(editorKit, false);
    }

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     * @param isEditing true if project is being edited, false otherwise
     */
    public ProjectDialogPanel(OWLEditorKit editorKit, boolean isEditing) {
        this.editorKit = checkNotNull(editorKit);
        this.isEditing = checkNotNull(isEditing);
        initInputFields();
        initUi();
    }

    private ListSelectionListener rowSelectionListener = e -> {
        if (e.getValueIsAdjusting()) {
            return;
        }
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (!lsm.isSelectionEmpty()) {
            if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                if (!removeOptionBtn.isEnabled()) {
                    enableButton(true, removeOptionBtn);
                }
                if (editOptionBtn.isEnabled()) {
                    enableButton(false, editOptionBtn);
                }
            } else {
                enableButton(true, editOptionBtn, removeOptionBtn);
            }
            selectedOptionKeys = new ArrayList<>();
            for (int i = 0; i < optionsTable.getSelectedRowCount(); i++) {
                String key = optionsTableModel.getKey(optionsTable.convertRowIndexToModel(optionsTable.getSelectedRows()[i]));
                selectedOptionKeys.add(key);
            }
        } else {
            enableButton(false, removeOptionBtn, editOptionBtn);
        }
    };

    private void initInputFields() {
        id = new JTextField(FIELD_WIDTH);
        name = new AugmentedJTextField(FIELD_WIDTH, "Project name");
        description = new AugmentedJTextArea(4, FIELD_WIDTH, "Project description");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(GuiUtils.EMPTY_BORDER);

        if (isEditing) {
            initComboBox();
            ItemListener comboBoxListener = e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ownerId = ((User) e.getItem()).getId();
                    handleValueChange();
                }
            };
            ownerSelection.addItemListener(comboBoxListener);
        }

        Insets insets = id.getBorder().getBorderInsets(id);
        id.setText(UUID.randomUUID().toString());
        id.setEditable(false);
        id.setBorder(new EmptyBorder(insets));
        id.setBackground(getBackground());
        id.setForeground(Color.GRAY.darker().darker());

        idLbl = new JLabel("Project Id:");
        nameLbl = new JLabel("Name:");
        descriptionLbl = new JLabel("Description:");
        fileLbl = new JLabel("File:");
        ownerLbl = new JLabel("Owner:");
        fileSelectionLbl = new JLabel();

        addListener(id.getDocument());
        addListener(name.getDocument());
        addListener(description.getDocument());
        fileBtn.addActionListener(new UploadActionListener());
        ownerId = getOwnerId();
    }

    private class UploadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == fileBtn) {
                file = UIUtil.openFile(ProjectDialogPanel.this.getRootPane(), "Choose file to upload", "OWL File", UIHelper.OWL_EXTENSIONS);
                if (file != null) {
                    fileSelectionLbl.setText(file.getName());
                    handleValueChange();
                }
            }
        }
    }

    private void initUi() {
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        tabbedPane.addTab("Details", detailsPanel);
        tabbedPane.addTab("Options", getProjectOptionsPanel());
        add(tabbedPane);

        JScrollPane descriptionScrollPane = new JScrollPane(description);
        descriptionScrollPane.setBorder(GuiUtils.MATTE_BORDER);

        Insets insets = new Insets(0, 2, 2, 2);
        int rowIndex = 0;
        detailsPanel.add(idLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, new Insets(12, 0, 2, 2), 0, 0));
        detailsPanel.add(id, new GridBagConstraints(1, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        detailsPanel.add(nameLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        detailsPanel.add(name, new GridBagConstraints(1, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        detailsPanel.add(descriptionLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        detailsPanel.add(descriptionScrollPane, new GridBagConstraints(1, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        if (isEditing) {
            detailsPanel.add(ownerLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
            detailsPanel.add(ownerSelection, new GridBagConstraints(1, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            rowIndex++;
        }
        if(!isEditing) {
            detailsPanel.add(new JSeparator(), new GridBagConstraints(0, rowIndex, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 2), 0, 0));
            rowIndex++;
            detailsPanel.add(fileLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
            detailsPanel.add(fileBtn, new GridBagConstraints(1, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.RELATIVE, new Insets(0, 0, 2, 0), 0, 0));
            detailsPanel.add(fileSelectionLbl, new GridBagConstraints(2, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
            rowIndex++;
        }
        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        detailsPanel.add(errorArea, new GridBagConstraints(0, rowIndex, 3, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    private JPanel getProjectOptionsPanel() {
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsTableModel = new ProjectOptionsTableModel();
        optionsTable = new ProjectOptionsTable(optionsTableModel);
        optionsTable.setPreferredScrollableViewportSize(optionsTable.getPreferredSize());
        optionsTable.getSelectionModel().addListSelectionListener(rowSelectionListener);
        setColumnsWidth(optionsTable, 125, 300);

        JScrollPane scrollPane = new JScrollPane(optionsTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(GuiUtils.MATTE_BORDER);
        optionsPanel.add(scrollPane, BorderLayout.CENTER);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(optionsTableModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(ProjectOptionsTableModel.Column.KEY.ordinal(), SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        optionsTable.setRowSorter(sorter);

        // buttons to add, remove, and edit project options
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addOptionBtn = new JButton(GuiUtils.getIcon("plus.png", 14, 14));
        removeOptionBtn = new JButton(GuiUtils.getIcon("minus.png", 14, 14));
        editOptionBtn = new JButton(GuiUtils.getIcon("edit.png", 16, 16));

        enableButton(false, removeOptionBtn, editOptionBtn);
        addOptionBtn.setPreferredSize(new Dimension(30, 30));
        addOptionBtn.setToolTipText("Add new project option");
        addOptionBtn.addActionListener(addOptionBtnListener);

        removeOptionBtn.setPreferredSize(new Dimension(30, 30));
        removeOptionBtn.addActionListener(removeOptionBtnListener);
        removeOptionBtn.setToolTipText("Remove selected project option");

        editOptionBtn.setPreferredSize(new Dimension(30, 30));
        editOptionBtn.addActionListener(editOptionBtnListener);
        editOptionBtn.setToolTipText("Edit selected project option");

        buttonsPanel.add(addOptionBtn);
        buttonsPanel.add(removeOptionBtn);
        buttonsPanel.add(editOptionBtn);

        optionsPanel.add(buttonsPanel, BorderLayout.SOUTH);
        return optionsPanel;
    }

    private ActionListener removeOptionBtnListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (!selectedOptionKeys.isEmpty()) {
                for (String s : selectedOptionKeys) {
                    projectOptions.remove(s);
                    optionsTableModel.setOptions(createProjectOptions(projectOptions));
                    updateProjectWithOptions();
                    handleValueChange();
                    refresh();
                }
            }
        }
    };

    private ActionListener addOptionBtnListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Optional<ProjectOption> optionOpt = ProjectOptionDialogPanel.showDialog(editorKit, projectOptions.keySet());
            if (optionOpt.isPresent()) {
                projectOptions.put(optionOpt.get().getKey(), new TreeSet<>(optionOpt.get().getValues()));
                optionsTableModel.setOptions(createProjectOptions(projectOptions));
                handleValueChange();
                updateProjectWithOptions();
                refresh();
            }
        }
    };

    private ActionListener editOptionBtnListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (!(selectedOptionKeys.size() > 1) && !selectedOptionKeys.isEmpty()) {
                String key = selectedOptionKeys.get(0);
                Set<String> values = optionsTableModel.getValues(key);
                Optional<ProjectOption> optionOpt = ProjectOptionDialogPanel.showDialog(editorKit, projectOptions.keySet(), key, values);
                if (optionOpt.isPresent()) {
                    projectOptions.put(optionOpt.get().getKey(), new TreeSet<>(optionOpt.get().getValues()));
                    if (!optionOpt.get().getKey().equals(key)) {
                        projectOptions.remove(key);
                        optionsTableModel.setOptions(createProjectOptions(projectOptions));
                        handleValueChange();
                        updateProjectWithOptions();
                    }
                }
            }
        }
    };

    private void updateProjectWithOptions() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.updateProject(selectedProject.getId(), createProject());
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private UserId getOwnerId() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        return client.getAuthToken().getUser().getId();
    }

    private void enableButton(boolean enable, JButton... buttons) {
        for (JButton button : buttons) {
            button.setEnabled(enable);
        }
    }

    private ProjectOptions createProjectOptions(Map<String, Set<String>> map) {
        return Manager.getFactory().getProjectOptions(map);
    }

    private void initComboBox() {
        User[] users = new User[0];
        try {
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            List<User> userList = client.getAllUsers();
            Collections.sort(userList);
            users = userList.toArray(new User[userList.size()]);
        } catch (RemoteException | ClientRequestException | AuthorizationException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        ownerSelection = new JComboBox<>(users);
        ownerSelection.setRenderer(new MetaprojectObjectComboBoxRenderer());
    }

    private void setIsEditing(Project project) {
        selectedProject = checkNotNull(project);
        id.setText(project.getId().get());
        name.setText(project.getName().get());
        description.setText(project.getDescription().get());
        file = project.getFile();
        fileSelectionLbl.setText(file.getName());
        for (int i = 0; i < ownerSelection.getModel().getSize(); i++) {
            User user = ownerSelection.getModel().getElementAt(i);
            if (user.getId().equals(project.getOwner())) {
                ownerSelection.setSelectedItem(user);
            }
        }
        if (project.getOptions().isPresent()) {
            ProjectOptions options = project.getOptions().get();
            projectOptions.putAll(options.getOptions());
            optionsTableModel.setOptions(options);
        }
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
        } catch (InvalidInputFileException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if (cause != null) {
                errorArea.setText(cause.getMessage());
            } else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws InvalidInputFileException {
        boolean allValid = true;
        if (name.getText().trim().isEmpty()) {
            allValid = false;
        }
        if (description.getText().trim().isEmpty()) {
            allValid = false;
        }
        if (ownerId == null) {
            allValid = false;
        }
        if (!isEditing && file == null) {
            allValid = false;
        }
        return allValid;
    }

    private Project createProject() {
        MetaprojectFactory f = Manager.getFactory();
        return f.getProject(f.getProjectId(id.getText()), f.getName(name.getText()), f.getDescription(description.getText()),
                file, ownerId, Optional.ofNullable(f.getProjectOptions(projectOptions)));
    }

    private void addProject(Project project) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {            
            ((LocalHttpClient) client).createProject(project);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void update(Project project) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.updateProject(selectedProject.getId(), project);
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    public static Optional<Project> showDialog(OWLEditorKit editorKit) {
        ProjectDialogPanel panel = new ProjectDialogPanel(editorKit);
        Optional<Project> project = showDialog(editorKit, panel, "Add New Project");
        if (project.isPresent()) {
            panel.addProject(project.get());
            return Optional.of(project.get());
        }
        return Optional.empty();
    }

    public static Optional<Project> showDialog(OWLEditorKit editorKit, Project selectedProject) {
        ProjectDialogPanel panel = new ProjectDialogPanel(editorKit, true);
        panel.setIsEditing(selectedProject);
        Optional<Project> project = showDialog(editorKit, panel, "Edit Project '" + selectedProject.getName().get() + "'");
        if (project.isPresent()) {
            panel.update(project.get());
            return Optional.of(project.get());
        }
        return Optional.empty();
    }

    private static Optional<Project> showDialog(OWLEditorKit editorKit, ProjectDialogPanel panel, String header) {
        int response = JOptionPaneEx.showValidatingConfirmDialog(
                editorKit.getOWLWorkspace(), header, panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createProject());
        }
        return Optional.empty();
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners) {
            l.verifiedStatusChanged(currentlyValid);
        }
    }

    private void setColumnsWidth(JTable table, double... values) {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) values[i]);
        }
    }

    private void refresh() {
        revalidate();
        repaint();
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
