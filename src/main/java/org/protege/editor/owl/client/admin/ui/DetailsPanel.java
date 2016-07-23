package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.ClientSessionChangeEvent;
import org.protege.editor.owl.client.ClientSessionListener;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.admin.model.Detail;
import org.protege.editor.owl.client.diff.ui.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class DetailsPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = 3603869997542571318L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public DetailsPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        ClientSession.getInstance(editorKit).addListener(sessionListener);
        initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.SELECTION_CHANGED)) {
            removeAll();
            initUi(configManager.getSelection());
        } else if (event.equals(AdminTabEvent.POLICY_ITEM_SELECTION_CHANGED)) {
            removeAll();
            initUi(configManager.getPolicySelection());
        } else if(event.equals(AdminTabEvent.CONFIGURATION_CHANGED)) {
            removeAll();
            initUi();
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.CLEAR_SESSION)) {
            removeAll();
            revalidate();
            repaint();
            initUi();
        }
    };

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(GuiUtils.MATTE_BORDER);
    }

    private void initUi(MetaprojectObject obj) {
        initUi();
        if (obj != null) {
            JPanel detailsPanel = null;
            if (obj.isUser()) {
                detailsPanel = getUserDetailsPanel((User) obj);
            } else if (obj.isProject()) {
                detailsPanel = getProjectDetails((Project) obj);
            } else if (obj.isOperation()) {
                detailsPanel = getOperationDetails((Operation) obj);
            } else if (obj.isRole()) {
                detailsPanel = getRoleDetails((Role) obj);
            }
            if (detailsPanel != null) {
                add(detailsPanel, BorderLayout.PAGE_START);
            }
        }
        revalidate();
        repaint();
    }

    private JPanel getDetails(String header, Detail... details) {
        int rowIndex = 0;
        Insets insets = new Insets(2, 15, 2, 5);
        JPanel userDetailsPanel = new JPanel(new GridBagLayout());
        userDetailsPanel.setBackground(Color.WHITE);
        userDetailsPanel.setBorder(GuiUtils.EMPTY_BORDER);
        JLabel headerLbl = new JLabel(header);
        headerLbl.setFont(getFont().deriveFont(Font.BOLD, getFont().getSize() + 1));
        userDetailsPanel.add(headerLbl, // add header
                new GridBagConstraints(0, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, new Insets(6, 6, 1, 6), 0, 0));
        rowIndex++;
        userDetailsPanel.add(new JSeparator(), // add separator
                new GridBagConstraints(0, rowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 6, 4, 6), 0, 0));
        rowIndex++;
        for (Detail detail : details) { // add details
            userDetailsPanel.add(new JLabel(detail.getName() + ":"), getGridBagConstraints(0, rowIndex, insets));
            if (detail.isLabel()) {
                userDetailsPanel.add(new JLabel(detail.getValue()), getGridBagConstraints(1, rowIndex, insets));
            } else if (detail.isTextArea()) {
                JTextArea textArea = new JTextArea(detail.getValue());
                textArea.setRows(5);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setBorder(GuiUtils.EMPTY_BORDER);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                userDetailsPanel.add(scrollPane, getGridBagConstraints(1, rowIndex, insets));
            }
            rowIndex++;
        }
        return userDetailsPanel;
    }

    private JPanel getUserDetailsPanel(User user) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", user.getId().get()));
        details.add(new Detail("Name", user.getName().get()));
        details.add(new Detail("E-mail", user.getEmailAddress().get()));
        return getDetails("User details", details.toArray(new Detail[details.size()]));
    }

    private JPanel getProjectDetails(Project project) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", project.getId().get()));
        details.add(new Detail("Name", project.getName().get()));
        details.add(new Detail("Description", project.getDescription().get()));
        details.add(new Detail("Owner", project.getOwner().get()));
        details.add(new Detail("File", project.getFile().getPath()));
        return getDetails("Project details", details.toArray(new Detail[details.size()]));
    }

    private JPanel getOperationDetails(Operation operation) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", operation.getId().get()));
        details.add(new Detail("Name", operation.getName().get()));
        details.add(new Detail("Description", operation.getDescription().get()));
        details.add(new Detail("Type", operation.getType().getName()));
        String scope = "";
        if (operation.getScope().equals(Operation.Scope.METAPROJECT)) {
            scope = "Metaproject";
        } else if (operation.getScope().equals(Operation.Scope.ONTOLOGY)) {
            scope = "Ontology";
        } else if (operation.getScope().equals(Operation.Scope.SERVER)) {
            scope = "Server";
        }
        details.add(new Detail("Scope", scope));
        return getDetails("Operation details", details.toArray(new Detail[details.size()]));
    }

    private JPanel getRoleDetails(Role role) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", role.getId().get()));
        details.add(new Detail("Name", role.getName().get()));
        details.add(new Detail("Description", role.getDescription().get()));
        String operations = "";
        for (OperationId op : role.getOperations()) {
            operations += op.get() + "\n";
        }
        Detail operationDetails = new Detail("Operations", operations);
        operationDetails.setIsTextArea(true);
        details.add(operationDetails);
        return getDetails("Role details", details.toArray(new Detail[details.size()]));
    }

    private GridBagConstraints getGridBagConstraints(int gridx, int rowIndex, Insets insets) {
        if (gridx == 0) {
            return new GridBagConstraints(gridx, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, insets, 0, 0);
        } else if (gridx == 1) {
            return new GridBagConstraints(gridx, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0);
        } else {
            return null;
        }
    }

    @Override
    public void dispose() {
        configManager.removeListener(tabListener);
        ClientSession.getInstance(editorKit).removeListener(sessionListener);
    }
}
