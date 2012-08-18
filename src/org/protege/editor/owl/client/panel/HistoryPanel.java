package org.protege.editor.owl.client.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLCellRenderer;
import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.OntologyDocumentRevision;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

public class HistoryPanel extends JPanel {
    private static final long serialVersionUID = 5102935053934335530L;
    private OWLEditorKit editorKit;
    private OWLOntology ontology;
    private ChangeHistory changes;
    private JTable changeListTable;
    private ChangeListTableModel changeListTableModel;
    
    public HistoryPanel(OWLEditorKit editorKit, ChangeHistory changes) {
        this.editorKit = editorKit;
        this.ontology = editorKit.getOWLModelManager().getActiveOntology();
        this.changes = changes;
    }
    
    public void initialise() {
        setLayout(new GridLayout(0,1));
        add(getHistoryComponent());
        add(getChangeListComponent());
        setPreferredSize(new Dimension(1000, 600));
        setVisible(true);
    }
    
    private JComponent getHistoryComponent() {
        HistoryTableModel model = new HistoryTableModel(changes);
        final JTable table = new JTable(model);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                List<OWLOntologyChange> changesToDisplay = new ArrayList<OWLOntologyChange>();
                for (int row : table.getSelectedRows()) {
                    OntologyDocumentRevision start = changes.getStartRevision().add(row);
                    changesToDisplay.addAll(changes.cropChanges(start, start.next()).getChanges(ontology));
                }
                changeListTableModel.setChangeList(changesToDisplay);
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    }
    
    private JComponent getChangeListComponent() {
        changeListTableModel = new ChangeListTableModel(new ArrayList<OWLOntologyChange>());
        changeListTable = new JTable(changeListTableModel);
        changeListTable.setDefaultRenderer(OWLObject.class, new OWLCellRenderer(editorKit));
        JScrollPane scrollPane = new JScrollPane(changeListTable);
        return scrollPane;
    }
}
