package org.protege.editor.owl.client.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
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

public class ChangeHistoryPanel extends JDialog {

	private static final long serialVersionUID = -372532962143290188L;
    private OWLEditorKit editorKit;
    private OWLOntology ontology;
    private ChangeHistory changes;
    private JTable changeListTable;
    private ChangeListTableModel changeListTableModel;
    
	public ChangeHistoryPanel(OWLEditorKit editorKit, ChangeHistory changes) {
		this.editorKit = editorKit;
        this.ontology = editorKit.getOWLModelManager().getActiveOntology();
		this.changes = changes;
		initUI();
	}
	
	private void initUI() {
		String fragment = ontology.getOntologyID().getOntologyIRI().getFragment();
		setTitle("Change History for " + fragment);
        setPreferredSize(new Dimension(800, 600));
		setModal(true);
		
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		
		// Changes list
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JLabel label = new JLabel("Changes List");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		Font font = label.getFont().deriveFont(Font.BOLD);
		label.setFont(font);
		panel.add(label);
		panel.add(Box.createRigidArea(new Dimension(0,5)));
		panel.add(getHistoryComponent());
		panel.add(Box.createRigidArea(new Dimension(0,11)));
		
		// Change details
		label = new JLabel("Change Details");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setFont(font);
		panel.add(label);
		panel.add(Box.createRigidArea(new Dimension(0,5)));
		panel.add(getChangeListComponent());
		panel.add(Box.createRigidArea(new Dimension(0,17)));

		wrapper.add(panel, BorderLayout.CENTER);
		
		// OK button
		JButton button = new JButton("OK");
		ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		};
		button.addActionListener(listener);
		button.setMargin(new Insets(button.getInsets().top, 12, button.getInsets().bottom, 12));
		panel = new JPanel();
		panel.add(button);
		
		wrapper.add(panel, BorderLayout.SOUTH);

		getContentPane().add(wrapper);
		
		// TODO Why doesn't this work?
		//getRootPane().setDefaultButton(button);
		
		pack();
		setResizable(true);
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
}
