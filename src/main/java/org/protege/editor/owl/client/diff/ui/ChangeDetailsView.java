package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 4203564449172661511L;
    private ChangeDetailsPanel detailsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        detailsPanel = new ChangeDetailsPanel(getOWLModelManager(), getOWLEditorKit());
        add(detailsPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        detailsPanel.dispose();
    }
}
