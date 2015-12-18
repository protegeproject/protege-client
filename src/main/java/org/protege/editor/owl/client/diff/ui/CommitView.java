package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 8979240527400492916L;
    private CommitPanel commitPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        commitPanel = new CommitPanel(getOWLModelManager(), getOWLEditorKit());
        add(commitPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        commitPanel.dispose();
    }
}