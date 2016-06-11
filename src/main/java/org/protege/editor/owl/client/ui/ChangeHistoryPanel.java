package org.protege.editor.owl.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ChangeUtils;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeHistoryPanel extends JPanel {

    private static final long serialVersionUID = -372532962143290188L;

    private OWLEditorKit editorKit;
    private OWLOntology ontology;
    private VersionedOWLOntology vont;

    private JTable changeListTable;
    private ChangeListTableModel changeListTableModel;

    public ChangeHistoryPanel(VersionedOWLOntology vont, OWLEditorKit editorKit) throws ClientRequestException {
        this.vont = vont;
        this.editorKit = editorKit;
        this.ontology = editorKit.getOWLModelManager().getActiveOntology();
        initUI();
    }

    private void initUI() throws ClientRequestException {
        String shortOntologyName = "";
        OWLOntologyID ontologyId = ontology.getOntologyID();
        if (!ontologyId.isAnonymous()) {
            shortOntologyName = ontology.getOntologyID().getOntologyIRI().get().getRemainder().get();
        }
        if (shortOntologyName.isEmpty()) {
            shortOntologyName = ontologyId.toString();
        }

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Changes list
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Changes List");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        Font font = label.getFont().deriveFont(Font.BOLD);
        label.setFont(font);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(getHistoryComponent());
        panel.add(Box.createRigidArea(new Dimension(0, 11)));

        // Change details
        label = new JLabel("Change Details");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setFont(font);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(getChangeListComponent());
        panel.add(Box.createRigidArea(new Dimension(0, 17)));

        add(panel, BorderLayout.CENTER);

        add(getButtonPanel(), BorderLayout.SOUTH);
    }

    private JComponent getHistoryComponent() throws ClientRequestException {
        ChangeHistory remoteChanges = LocalHttpClient.current_user().getAllChanges(vont.getServerDocument());
        HistoryTableModel model = new HistoryTableModel(remoteChanges);
        final JTable table = new JTable(model);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                List<OWLOntologyChange> changesToDisplay = new ArrayList<OWLOntologyChange>();
                DocumentRevision baseRevision = remoteChanges.getBaseRevision();
                for (int row : table.getSelectedRows()) {
                    DocumentRevision start = baseRevision.next(table.convertRowIndexToModel(row));
                    ChangeHistory subChangeHistory = ChangeHistoryUtils.crop(remoteChanges, start, 1);
                    List<OWLOntologyChange> subChanges = ChangeHistoryUtils.getOntologyChanges(subChangeHistory, ontology);
                    changesToDisplay.addAll(subChanges);
                }
                changeListTableModel.setChangeList(changesToDisplay);
            }
        });

        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy hh:mm a, z");
        TableCellRenderer renderer = new FormatRenderer(format);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(renderer);

        // Allow user to sort
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
        table.setRowSorter(sorter);

        // Sort initially by the date column in descending order
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
    }

    private JComponent getChangeListComponent() {
        changeListTableModel = new ChangeListTableModel(new ArrayList<OWLOntologyChange>());
        changeListTable = new JTable(changeListTableModel);
        changeListTable.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(editorKit));
        JScrollPane scrollPane = new JScrollPane(changeListTable);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
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
        Window window = SwingUtilities.getWindowAncestor(ChangeHistoryPanel.this);
        window.setVisible(false);
        window.dispose();
    }
}
