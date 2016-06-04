package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class DetailsView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -8157752220550037688L;
    private DetailsPanel detailsPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        detailsPanel = new DetailsPanel(getOWLEditorKit());
        add(detailsPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        detailsPanel.dispose();
    }
}
