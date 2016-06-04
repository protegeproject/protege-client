package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RoleView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -1870386127171642047L;
    private RolePanel rolePanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        rolePanel = new RolePanel(getOWLEditorKit());
        add(rolePanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        rolePanel.dispose();
    }
}
