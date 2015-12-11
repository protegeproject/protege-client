package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangesView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 8062695577507306066L;
    private ChangesPanel changesPanel;
    private ReviewButtonsPanel reviewButtonsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        changesPanel = new ChangesPanel(getOWLModelManager(), getOWLEditorKit());
        reviewButtonsPanel = new ReviewButtonsPanel(getOWLModelManager(), getOWLEditorKit());
        add(changesPanel, BorderLayout.CENTER);
        add(reviewButtonsPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void disposeOWLView() {
        changesPanel.dispose();
        reviewButtonsPanel.dispose();
    }
}
