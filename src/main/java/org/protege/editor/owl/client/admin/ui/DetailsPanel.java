package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.admin.model.Detail;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;

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
            revalidate();
            repaint();
            initUi();
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGOUT)) {
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

    private void initUi(PolicyObject obj) {
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
                JScrollPane scrollPane = new JScrollPane(detailsPanel);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
                add(scrollPane, BorderLayout.CENTER);
            }
        }
        revalidate();
        repaint();
    }

    private JPanel getDetails(String header, List<Detail> details) {
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
            JLabel label = new JLabel("<html><p style=\"font-weight:500; color:#797D7F\">" + detail.getName() + ":</p></html>");
            userDetailsPanel.add(label, getGridBagConstraints(0, rowIndex, insets, detail.isLast()));
            if (detail.isLabel()) {
                userDetailsPanel.add(new JLabel(detail.getValue()), getGridBagConstraints(1, rowIndex, insets, detail.isLast()));
            } else if (detail.isTextArea()) {
                JTextArea textArea = new JTextArea(detail.getValue());
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setBorder(GuiUtils.EMPTY_BORDER);
                userDetailsPanel.add(textArea, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            }
            rowIndex++;
        }
        return userDetailsPanel;
    }

    private JPanel getUserDetailsPanel(User user) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", user.getId().get()));
        details.add(new Detail("Name", user.getName().get()));
        Detail email = new Detail("E-mail", user.getEmailAddress().get());
        email.setIsLast(true);
        details.add(email);
        return getDetails("User details", details);
    }

    private JPanel getProjectDetails(Project project) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", project.getId().get()));
        details.add(new Detail("Name", project.getName().get()));
        Detail description = new Detail("Description", project.getDescription().get());
        description.setIsTextArea(true);
        details.add(description);
        details.add(new Detail("Owner", project.getOwner().get()));
        Detail file = new Detail("File", project.getFile().getPath());
        file.setIsLast(true);
        details.add(file);
        return getDetails("Project details", details);
    }

    private JPanel getOperationDetails(Operation operation) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", operation.getId().get()));
        details.add(new Detail("Name", operation.getName().get()));
        Detail description = new Detail("Description", operation.getDescription().get());
        description.setIsTextArea(true);
        details.add(description);
        details.add(new Detail("Type", operation.getType().getName()));
        String scope = "";
        if (operation.getScope().equals(Operation.Scope.POLICY)) {
            scope = "Policy";
        } else if (operation.getScope().equals(Operation.Scope.ONTOLOGY)) {
            scope = "Ontology";
        } else if (operation.getScope().equals(Operation.Scope.SERVER)) {
            scope = "Server";
        }
        Detail scopeDetail = new Detail("Scope", scope);
        scopeDetail.setIsLast(true);
        details.add(scopeDetail);
        return getDetails("Operation details", details);
    }

    private JPanel getRoleDetails(Role role) {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Identifier", role.getId().get()));
        details.add(new Detail("Name", role.getName().get()));
        Detail description = new Detail("Description", role.getDescription().get());
        description.setIsTextArea(true);
        details.add(description);
        String operations = "";
        for (OperationId op : role.getOperations()) {
            operations += op.get() + "\n";
        }
        Detail operationDetails = new Detail("Operations", operations);
        operationDetails.setIsTextArea(true);
        operationDetails.setIsLast(true);
        details.add(operationDetails);
        return getDetails("Role details", details);
    }

    private GridBagConstraints getGridBagConstraints(int gridx, int rowIndex, Insets insets, boolean isLast) {
        if (gridx == 0) {
            if(isLast) {
                return new GridBagConstraints(gridx, rowIndex, 1, 1, 0.0, 1.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.VERTICAL, insets, 0, 0);
            } else {
                return new GridBagConstraints(gridx, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, insets, 0, 0);
            }
        } else if (gridx == 1) {
            if(isLast) {
                return new GridBagConstraints(gridx, rowIndex, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.BOTH, insets, 0, 0);
            } else {
                return new GridBagConstraints(gridx, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0);
            }
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
