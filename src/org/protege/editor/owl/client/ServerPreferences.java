package org.protege.editor.owl.client;

import java.util.ArrayList;
import java.util.List;

public class ServerPreferences {
    private List<ServerPreferencesListener> listeners = new ArrayList<ServerPreferencesListener>();
    private boolean autoCommit;
    private boolean autoUpdate;
    
    public ServerPreferences() {
        this(true, true);
    }
    
    public ServerPreferences(boolean autoCommit, boolean autoUpdate) {
        this.autoCommit = autoCommit;
        this.autoUpdate = autoUpdate;
    }
    
    public boolean isAutoCommit() {
        return autoCommit;
    }
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        fireUpdate();
    }
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        fireUpdate();
    }
    
    public void addListener(ServerPreferencesListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ServerPreferencesListener listener) {
        listeners.remove(listener);
    }
    
    private void fireUpdate() {
        for (ServerPreferencesListener listener :listeners){
            listener.serverPreferencesChanged();
        }
    }
    
    public interface ServerPreferencesListener {
        void serverPreferencesChanged();
    }
    
}
