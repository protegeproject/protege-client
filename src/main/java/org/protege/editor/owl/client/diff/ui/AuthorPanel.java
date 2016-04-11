package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.LogDiffEvent;
import org.protege.editor.owl.client.diff.model.LogDiffManager;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.owl.server.changes.ChangeMetaData;
import org.protege.owl.server.changes.OntologyDocumentRevision;
import org.protege.owl.server.changes.api.ChangeHistory;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntologyChangeListener;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = -211414461074963460L;
    private OWLModelManager modelManager;
    private LogDiffManager diffManager;
    private JList<UserId> authorsList = new JList<>();

    /**
     * Constructor
     *
     * @param modelManager  OWL model manager
     * @param editorKit OWL editor kit
     */
    public AuthorPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.modelManager = modelManager;
        diffManager = LogDiffManager.get(modelManager, editorKit);
        setLayout(new BorderLayout(20, 20));
        setupList();

        // listeners
        modelManager.getOWLOntologyManager().addOntologyChangeListener(ontologyChangeListener);
        modelManager.addListener(ontologyLoadListener);

        JScrollPane scrollPane = new JScrollPane(authorsList);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        add(scrollPane, BorderLayout.CENTER);
        listAuthors();
    }

    private ListSelectionListener listSelectionListener = e -> {
        UserId selection = authorsList.getSelectedValue();
        if (selection != null && !e.getValueIsAdjusting()) {
            diffManager.setSelectedAuthor(selection);
        }
    };

    private OWLOntologyChangeListener ontologyChangeListener = changes -> {
        diffManager.clearSelections();
        listAuthors();
        diffManager.statusChanged(LogDiffEvent.ONTOLOGY_UPDATED);
    };

    private OWLModelManagerListener ontologyLoadListener = event -> {
        if(event.isType(EventType.ONTOLOGY_LOADED) || event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED)) {
            diffManager.clearSelections();
            listAuthors();
            diffManager.statusChanged(LogDiffEvent.ONTOLOGY_UPDATED);
        }
    };

    private void setupList() {
        authorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        authorsList.addListSelectionListener(listSelectionListener);
        authorsList.setCellRenderer(new AuthorListCellRenderer());
        authorsList.setFixedCellHeight(35);
        authorsList.setBorder(GuiUtils.MATTE_BORDER);
    }

    private void listAuthors() {
        if(diffManager.getVersionedOntologyDocument().isPresent()) {
            VersionedOntologyDocument vont = diffManager.getVersionedOntologyDocument().get();
            ChangeHistory changes = vont.getLocalHistory();
            List<UserId> users = new ArrayList<>();
            OntologyDocumentRevision rev = changes.getStartRevision();
            while (changes.getMetaData(rev) != null) {
                ChangeMetaData metaData = changes.getMetaData(rev);
                UserId user = metaData.getAuthorId();
                if (!users.contains(user)) {
                    users.add(user);
                }
                rev = rev.next();
            }
//            Collections.sort(users); TODO: To review later
            users.add(0, LogDiffManager.ALL_AUTHORS);
            authorsList.setListData(users.toArray(new UserId[users.size()]));
        }
        else {
            authorsList.setListData(new UserId[0]);
        }
    }

    @Override
    public void dispose() {
        modelManager.removeListener(ontologyLoadListener);
        modelManager.getOWLOntologyManager().removeOntologyChangeListener(ontologyChangeListener);
        authorsList.removeListSelectionListener(listSelectionListener);
    }
}
