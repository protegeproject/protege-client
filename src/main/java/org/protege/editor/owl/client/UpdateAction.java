package org.protege.editor.owl.client;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ServerPreferences.ServerPreferencesListener;

public class UpdateAction extends ProtegeAction {
    private static final long serialVersionUID = 1693797438703528655L;
    private ServerPreferences preferences;

    public UpdateAction() {

    }

    private void updateEnabled() {
        setEnabled(preferences != null && !preferences.isAutoUpdate());
    }

    @Override
    public void initialise() throws Exception {
        preferences = ClientOntologyBuilder.getServerPreferences((OWLEditorKit) getEditorKit());
        updateEnabled();
        if (preferences != null) {
            preferences.addListener(new ServerPreferencesListener() {
                
                @Override
                public void serverPreferencesChanged() {
                    updateEnabled();
                }
            });
        }
    }

    @Override
    public void dispose() throws Exception {

    }
    
    @Override
    public boolean isEnabled() {
        ServerPreferences preferences = ClientOntologyBuilder.getServerPreferences((OWLEditorKit) getEditorKit());
        return preferences != null && !preferences.isAutoUpdate();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        OWLEditorKit kit = (OWLEditorKit) getEditorKit();
        ServerPreferences preferences = ClientOntologyBuilder.getServerPreferences(kit);
        if (preferences == null) return;
        ClientOntologyBuilder.update(kit);
    }
    
    public void setMenuItem(JMenuItem menu) {
        ((JCheckBoxMenuItem) menu).setSelected(true);
    }

}
