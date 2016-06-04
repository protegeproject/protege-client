package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 2508840513658115516L;
    private ProjectPanel projectPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        projectPanel = new ProjectPanel(getOWLEditorKit());
        add(projectPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        projectPanel.dispose();
    }
}
