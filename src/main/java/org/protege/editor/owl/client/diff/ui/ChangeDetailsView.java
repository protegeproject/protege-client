package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeDetailsView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -2828025387418723568L;
    private ChangeDetailsPanel logDiffPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        logDiffPanel = new ChangeDetailsPanel(getOWLModelManager(), getOWLEditorKit());
        add(logDiffPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        logDiffPanel.dispose();
    }
}
