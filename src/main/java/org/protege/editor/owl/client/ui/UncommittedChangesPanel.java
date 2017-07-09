package org.protege.editor.owl.client.ui;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class UncommittedChangesPanel extends JPanel {

    private static final long serialVersionUID = -7076342089755329250L;

    public UncommittedChangesPanel(List<OWLOntologyChange> uncommittedChanges, OWLEditorKit editorKit) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        ChangeListTableModel tableModel = new ChangeListTableModel(uncommittedChanges);
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(editorKit));
        add(new JScrollPane(table), BorderLayout.CENTER);

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
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(UncommittedChangesPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
