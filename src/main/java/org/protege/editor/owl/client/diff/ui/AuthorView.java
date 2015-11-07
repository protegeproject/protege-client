package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;

import java.awt.*;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AuthorView extends AbstractOWLViewComponent {
    private static final long serialVersionUID = -5601683043252978396L;
    private AuthorPanel authorPanel;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        authorPanel = new AuthorPanel(getOWLModelManager(), getOWLEditorKit());
        add(authorPanel, BorderLayout.CENTER);
    }

    @Override
    protected void disposeOWLView() {
        authorPanel.dispose();
    }
}
