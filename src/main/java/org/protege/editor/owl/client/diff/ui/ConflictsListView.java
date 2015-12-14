package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ConflictsListView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -2322371761068725012L;
    private ConflictsListPanel conflictsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        conflictsPanel = new ConflictsListPanel(getOWLModelManager(), getOWLEditorKit());
        add(conflictsPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        conflictsPanel.dispose();
    }
}
