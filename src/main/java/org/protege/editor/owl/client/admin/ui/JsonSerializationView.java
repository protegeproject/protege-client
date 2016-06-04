package org.protege.editor.owl.client.admin.ui;

import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class JsonSerializationView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = 6269299425107449908L;
    private JsonSerializationPanel serializationPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        setBorder(GuiUtils.MATTE_BORDER);
        serializationPanel = new JsonSerializationPanel(getOWLEditorKit());
        add(serializationPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        serializationPanel.dispose();
    }
}
