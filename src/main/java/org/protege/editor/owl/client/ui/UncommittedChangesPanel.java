package org.protege.editor.owl.client.ui;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

public class UncommittedChangesPanel extends JPanel {

    private static final long serialVersionUID = -7076342089755329250L;

    private OWLEditorKit editorKit;
    private OWLOntology ontology;
    private VersionedOWLOntology vont;

    public UncommittedChangesPanel(VersionedOWLOntology vont, OWLEditorKit editorKit) {
        this.vont = vont;
        this.editorKit = editorKit;
        this.ontology = editorKit.getOWLModelManager().getActiveOntology();
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        add(new JScrollPane(getChangeListTable()), BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);
    }

    private JTable getChangeListTable() {
        ChangeHistory baseline = vont.getChangeHistory();
        List<OWLOntologyChange> uncommittedChanges = ClientUtils.getUncommittedChanges(ontology, baseline);
        if (uncommittedChanges.isEmpty()) {
            Window window = SwingUtilities.getWindowAncestor(UncommittedChangesPanel.this);
            JOptionPaneEx.showConfirmDialog(window, "Message", new JLabel("No uncommitted changes"),
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
            closeDialog();
        }
        ChangeListTableModel tableModel = new ChangeListTableModel(uncommittedChanges);
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(editorKit));
        return table;
    }

    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton closeButton = new JButton("Close");
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        };
        closeButton.addActionListener(listener);
        closeButton.setMargin(new Insets(closeButton.getInsets().top, 12, closeButton.getInsets().bottom, 12));

        buttonPanel.add(closeButton);
        return buttonPanel;
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(UncommittedChangesPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
