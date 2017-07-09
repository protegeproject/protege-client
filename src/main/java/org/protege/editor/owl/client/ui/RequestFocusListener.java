package org.protege.editor.owl.client.ui;

import javax.swing.*;
import javax.swing.event.*;

/**
 * @author Rafael Gonçalves <br>
 * Center for Biomedical Informatics Research <br>
 * Stanford University
 */
public class RequestFocusListener implements AncestorListener {
    private boolean removeListener;

    public RequestFocusListener() {
        this(true);
    }

    public RequestFocusListener(boolean removeListener) {
        this.removeListener = removeListener;
    }

    @Override
    public void ancestorAdded(AncestorEvent e) {
        JComponent component = e.getComponent();
        component.requestFocusInWindow();
        if (removeListener) {
            component.removeAncestorListener(this);
        }
    }

    @Override
    public void ancestorMoved(AncestorEvent e) {
        /* no-op */
    }

    @Override
    public void ancestorRemoved(AncestorEvent e) {
        /* no-op */
    }
}