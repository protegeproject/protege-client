package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.diff.model.*;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsPanel extends JPanel implements Disposable {
    private OWLEditorKit editorKit;
    private LogDiffManager diffManager;
    private Change change;

    /**
     * Constructor
     *
     * @param modelManager OWL model manager
     * @param editorKit    OWL editor kit
     */
    public ChangeDetailsPanel(OWLModelManager modelManager, OWLEditorKit editorKit) {
        this.editorKit = editorKit;

        diffManager = LogDiffManager.get(modelManager, editorKit);
        diffManager.addListener(diffListener);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(GuiUtils.MATTE_BORDER);

        setBackground(GuiUtils.WHITE_BACKGROUND);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    private LogDiffListener diffListener = new LogDiffListener() {
        @Override
        public void statusChanged(LogDiffEvent event) {
            if (event.equals(LogDiffEvent.CHANGE_SELECTION_CHANGED)) {
                if(!diffManager.getSelectedChanges().isEmpty()) {
                    change = diffManager.getFirstSelectedChange();
                    removeAll(); repaint();
                    listDiffDetails();
                }
            }
            else if(event.equals(LogDiffEvent.AUTHOR_SELECTION_CHANGED) || event.equals(LogDiffEvent.COMMIT_SELECTION_CHANGED) || event.equals(LogDiffEvent.ONTOLOGY_UPDATED)) {
                removeAll(); repaint();
            }
        }
    };

    private void listDiffDetails() {
        if (change != null) {
            boolean hasCompBefore = false;
            if (change.getChangeAxioms() != null && !change.getChangeAxioms().isEmpty()) {
                hasCompBefore = true;
                add(new ChangeDetailLabel("Change(s)"));
                OwlObjectList objectList = new OwlObjectList(editorKit);
                objectList.setAlignmentX(LEFT_ALIGNMENT);
                objectList.setObjects(change.getChangeAxioms());
                add(objectList);
            }
            // the following only exist / should be shown for annotation property value or ontology IRI changes
            if(change.getAnnotationProperty().isPresent()) {
                Set<OWLObject> set = new HashSet<>();
                set.add(change.getAnnotationProperty().get());
                addChangeDetail("Property", set, hasCompBefore, Optional.empty());
                hasCompBefore = true;
            }
            if(change.getValue().isPresent() && !change.getType().equals(BuiltInChangeType.LOGICAL)) {
                Set<String> set = new HashSet<>();
                String newValue = change.getValue().get();
                set.add(newValue);
                addChangeDetail("Value", set, hasCompBefore, Optional.empty());
                hasCompBefore = true;
            }
            if(change.getPriorValue().isPresent()) {
                Set<String> set = new HashSet<>();
                set.add(change.getPriorValue().get());
                addChangeDetail("Prior Value", set, hasCompBefore, Optional.empty());
            }
            if(change.getBaselineChange().isPresent()) {
                Set<OWLAxiom> axioms = new HashSet<>();
                axioms.add(change.getBaselineChange().get().getAxiom());
                addChangeDetail("Baseline Change", axioms, hasCompBefore, Optional.empty());
            }
            addConflictDetails(change.getConflictingChanges());
        }
    }

    private void addConflictDetails(Set<Change> changes) {
        if(!changes.isEmpty()) {
            int counter = 1;
            for (Change change : changes) {
                String verb = "";
                switch(change.getChangeMode()) {
                    case ADDITION:
                        verb = "added"; break;
                    case REMOVAL:
                        verb = "removed"; break;
                    case ONTOLOGY_IRI:
                        verb = "modified IRI"; break;
                }
                addChangeDetail("Conflicting Change Set " + counter + " (" + change.getDate() + "). " + change.getAuthor() + " " + verb + ":", getObjectsToDisplay(change), true, Optional.of("warning.png"));
                counter++;
            }
        }
    }

    private Set<?> getObjectsToDisplay(Change c) {
        if(!c.getChangeAxioms().isEmpty()) {
            return c.getChangeAxioms();
        }
        else if(c.getValue().isPresent()) {
            Set<String> valueSet = new HashSet<>();
            valueSet.add(c.getValue().get());
            return valueSet;
        }
        else {
            throw new IllegalStateException("Tried to display objects from a change that contains none (" + c.toString() + ")");
        }
    }

    private void addChangeDetail(String labelText, Set<?> objects, boolean includeSeparation, Optional<String> iconFileName) {
        OwlObjectList objectList = new OwlObjectList(editorKit);
        if(objects != null) {
            objectList.setObjects(objects);
        }
        objectList.setAlignmentX(LEFT_ALIGNMENT);

        if(includeSeparation) {
            Component box = Box.createRigidArea(new Dimension(0, 20));
            add(box);
            toggleVisibility(objectList, box);
        }

        JLabel label = new ChangeDetailLabel(labelText);
        if(iconFileName.isPresent()) {
            label.setIcon(GuiUtils.getUserIcon(iconFileName.get(), 24, 24));
            label.setIconTextGap(8);
        }

        add(label);
        add(objectList);
        toggleVisibility(objectList, label, objectList);
    }

    private void toggleVisibility(Object obj, Component... components) {
        if(obj == null) {
            makeInvisible(components);
        }
        else {
            makeVisible(components);
        }
    }

    private void makeVisible(Component... components) {
        for(Component c : components) {
            c.setVisible(true);
        }
    }

    private void makeInvisible(Component... components) {
        for(Component c : components) {
            c.setVisible(false);
        }
    }

    @Override
    public void dispose() {
        diffManager.removeListener(diffListener);
    }
}
