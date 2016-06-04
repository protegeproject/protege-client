package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class PolicyView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -4912508388925521516L;
    private PolicyPanel policyPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        policyPanel = new PolicyPanel(getOWLEditorKit());
        add(policyPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        policyPanel.dispose();
    }
}
