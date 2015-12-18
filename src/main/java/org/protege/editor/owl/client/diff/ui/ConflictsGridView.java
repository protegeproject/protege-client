package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ConflictsGridView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -108220117637124754L;
    private ConflictsGridPanel conflictsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        conflictsPanel = new ConflictsGridPanel(getOWLModelManager(), getOWLEditorKit());
        add(conflictsPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        conflictsPanel.dispose();
    }
}
