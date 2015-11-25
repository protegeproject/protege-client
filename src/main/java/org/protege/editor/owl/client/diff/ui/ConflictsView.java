package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ConflictsView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -4033667243827397396L;
    private ConflictsPanel conflictsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        conflictsPanel = new ConflictsPanel(getOWLModelManager(), getOWLEditorKit());
        add(conflictsPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        conflictsPanel.dispose();
    }
}
