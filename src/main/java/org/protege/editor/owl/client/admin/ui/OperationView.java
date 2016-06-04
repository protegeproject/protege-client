package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OperationView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -2403697841733368114L;
    private OperationPanel operationPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        operationPanel = new OperationPanel(getOWLEditorKit());
        add(operationPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        operationPanel.dispose();
    }
}
