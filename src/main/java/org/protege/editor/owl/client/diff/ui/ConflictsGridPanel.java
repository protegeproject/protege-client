package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.client.ui.GuiUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ConflictsGridPanel extends JPanel implements Disposable {
    private OWLEditorKit editorKit;
    private LogDiffManager diffManager;
    private LogDiff diff;
    private Change change;

    /**
     * Constructor
     *
     * @param modelManager OWL model manager
     * @param editorKit    OWL editor kit
     */
    public ConflictsGridPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);
        diff = diffManager.getDiffEngine();

        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        setBackground(GuiUtils.WHITE_BACKGROUND);
        setAlignmentX(LEFT_ALIGNMENT);

        if (!diffManager.getSelectedChanges().isEmpty()) {
            change = diffManager.getFirstSelectedChange();
            addConflictDetails();
        }
    }

    private LogDiffListener diffListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if (event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if (!diffManager.getSelectedChanges().isEmpty()) {
                    change = diffManager.getFirstSelectedChange();
                    removeAll();
                    addConflictDetails();
                }
            } else if (event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) || event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                removeAll(); repaint();
            }
        }
    };

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weighty = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 5, 2, 5);
        return c;
    }

    private void addConflictDetails() {
        if (change != null) {
            Set<Change> changes = change.getConflictingChanges().stream().map(id -> diff.getChange(id)).collect(Collectors.toSet());
            if (!changes.isEmpty()) {
                Set<Change> before = new HashSet<>(), after = new HashSet<>();
                for (Change change : changes) {
                    if (change.getCommitMetadata().getDate().before(this.change.getCommitMetadata().getDate())) {
                        before.add(change);
                    } else {
                        after.add(change);
                    }
                }
                GridBagConstraints bpc = createGridBagConstraints(), apc = createGridBagConstraints();
                String priorConflictsPanelHeader = "Prior commits:", subsequentConflictsPanelHeader = "Subsequent commits:";
                if (!before.isEmpty() && !after.isEmpty()) {
                    JScrollPane beforePnlScrollPane = getConflictsScrollPane(before, bpc, priorConflictsPanelHeader);
                    JScrollPane afterPnlScrollPane = getConflictsScrollPane(after, apc, subsequentConflictsPanelHeader);
                    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, beforePnlScrollPane, afterPnlScrollPane);
                    splitPane.setBorder(GuiUtils.EMPTY_BORDER);
                    add(splitPane);
                    splitPane.setDividerLocation(0.5);
                    splitPane.setResizeWeight(0.5);
                } else if (!before.isEmpty()) {
                    add(getConflictsScrollPane(before, bpc, priorConflictsPanelHeader), BorderLayout.CENTER);
                } else if (!after.isEmpty()) {
                    add(getConflictsScrollPane(after, apc, subsequentConflictsPanelHeader), BorderLayout.CENTER);
                }
            }
            else {
                add(createHeaderLabel("No conflicts", Optional.of(new EmptyBorder(5, 6, 0, 0))), BorderLayout.NORTH);
            }
            repaint();
        }
    }

    private JScrollPane getConflictsScrollPane(Set<Change> changes, GridBagConstraints constraints, String panelHeader) {
        JPanel panel = new JPanel(new GridBagLayout());
        createSubPanel(panel, changes, constraints, panelHeader);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(GuiUtils.EMPTY_BORDER);
        return scrollPane;
    }

    private JLabel createHeaderLabel(String panelHeader, Optional<Border> border) {
        JLabel label = new JLabel(panelHeader);
        label.setFont(getFont().deriveFont(Font.BOLD));
        if(border.isPresent()) {
            label.setBorder(border.get());
        }
        return label;
    }

    private void createSubPanel(JPanel parent, Set<Change> changes, GridBagConstraints con, String panelHeader) {
        parent.add(createHeaderLabel(panelHeader, Optional.empty()), con);
        con.gridy = con.gridy + 1;
        parent.add(new JSeparator(SwingConstants.HORIZONTAL), con);
        con.gridy = con.gridy + 1;

        parent.setAlignmentX(LEFT_ALIGNMENT);
        parent.setBackground(GuiUtils.WHITE_BACKGROUND);
        parent.setBorder(new EmptyBorder(5, 1, 1, 1));
        if (!changes.isEmpty()) {
            createConflictsPanel(parent, changes, con);
        }
        // fill the remaining panel if necessary
        con.weightx = 1;
        con.weighty = 1;
        con.fill = GridBagConstraints.BOTH;
        parent.add(new JLabel(), con);
    }

    private void createConflictsPanel(JPanel panel, Set<Change> changes, GridBagConstraints con) {
        Map<CommitMetadata, Set<OWLOntologyChange>> map = new HashMap<>();
        for (Change change : changes) {
            CommitMetadata metadata = change.getCommitMetadata();
            if (map.containsKey(metadata)) {
                Set<OWLOntologyChange> ontChanges = map.get(metadata);
                ontChanges.addAll(change.getChanges());
                map.put(metadata, ontChanges);
            } else {
                map.put(metadata, change.getChanges());
            }
        }
        int counter = 1;
        for (CommitMetadata cd : map.keySet()) {
            Set<OWLOntologyChange> ontChanges = map.get(cd);
            ConflictingCommitPanel commitPanel = new ConflictingCommitPanel(editorKit, ontChanges, cd.getDate(), cd.getAuthor(), cd.getComment(), counter);
            panel.add(commitPanel, con);
            con.gridy = con.gridy + 1;

            panel.add(new JSeparator(SwingConstants.HORIZONTAL), con);
            con.gridy = con.gridy + 1;
            counter++;
        }
    }

    @Override
    public void dispose() {
        diffManager.removeListener(diffListener);
    }
}
