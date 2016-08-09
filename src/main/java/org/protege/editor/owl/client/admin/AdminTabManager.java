package org.protege.editor.owl.client.admin;

import edu.stanford.protege.metaproject.api.PolicyObject;
import org.protege.editor.core.Disposable;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AdminTabManager implements Disposable {
    private Set<AdminTabListener> listeners = new HashSet<>();
    private PolicyObject selection, policySelection;

    /**
     * Get the server configuration manager
     *
     * @param editorKit Protege OWL editor kit
     * @return Log diff manager
     */
    public static AdminTabManager get(OWLEditorKit editorKit) {
        AdminTabManager configManager = editorKit.getOWLModelManager().get(AdminTabManager.class);
        if(configManager == null) {
            configManager = new AdminTabManager();
            editorKit.getOWLModelManager().put(AdminTabManager.class, configManager);
        }
        return configManager;
    }

    /**
     * No-args constructor
     */
    public AdminTabManager() { }

    public void setSelection(PolicyObject object) {
        this.selection = checkNotNull(object);
        statusChanged(AdminTabEvent.SELECTION_CHANGED);
    }

    public void setPolicySelection(PolicyObject object) {
        this.policySelection = checkNotNull(object);
        statusChanged(AdminTabEvent.POLICY_ITEM_SELECTION_CHANGED);
    }

    public PolicyObject getSelection() {
        return selection;
    }

    public boolean hasSelection() {
        return selection != null;
    }

    public PolicyObject getPolicySelection() {
        return policySelection;
    }

    public void addListener(AdminTabListener listener) {
        listeners.add(checkNotNull(listener));
    }

    public void removeListener(AdminTabListener listener) {
        listeners.remove(checkNotNull(listener));
    }

    public void statusChanged(AdminTabEvent event) {
        checkNotNull(event);
        for(AdminTabListener listener : listeners) {
            listener.statusChanged(event);
        }
    }

    public void clearSelection() {
        selection = null;
        statusChanged(AdminTabEvent.SELECTION_CHANGED);
    }

    public void clearPolicySelection() {
        policySelection = null;
        statusChanged(AdminTabEvent.POLICY_ITEM_SELECTION_CHANGED);
    }

    @Override
    public void dispose() throws Exception {

    }
}
