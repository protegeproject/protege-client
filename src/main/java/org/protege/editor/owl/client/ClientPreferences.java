package org.protege.editor.owl.client;

import java.util.ArrayList;
import java.util.List;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;

/**
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientPreferences {

    private static ClientPreferences instance;

    private static final String CLIENT_PREFERENCES = "org.protege.editor.owl.client";

    private static final String CURRENT_USERNAME = "CURRENT_USERNAME";

    private static final String SERVER_LOCATIONS = "SERVER_LOCATIONS";

    private static final String LAST_SERVER_LOCATION = "LAST_SERVER_LOCATION";

    public static synchronized ClientPreferences getInstance() {
        if (instance == null) {
            instance = new ClientPreferences();
        }
        return instance;
    }

    public String getCurrentUsername() {
        Preferences prefs = getPreferences();
        return prefs.getString(CURRENT_USERNAME, null);
    }

    public void setCurrentUsername(String username) {
        Preferences prefs = getPreferences();
        prefs.putString(CURRENT_USERNAME, username);
    }

    public List<String> getServerLocations() {
        ArrayList<String> serverLocations = new ArrayList<String>();
        serverLocations
                .addAll(getPreferences().getStringList(SERVER_LOCATIONS, new ArrayList<String>()));
        return serverLocations;
    }

    public void setServerLocations(ArrayList<String> serverLocations) {
        Preferences prefs = getPreferences();
        prefs.putStringList(SERVER_LOCATIONS, serverLocations);
    }

    public String getLastServerLocation() {
        Preferences prefs = getPreferences();
        return prefs.getString(LAST_SERVER_LOCATION, null);
    }

    public void setLastServerLocation(String lastServerLocation) {
        Preferences prefs = getPreferences();
        prefs.putString(LAST_SERVER_LOCATION, lastServerLocation);
    }

    protected static Preferences getPreferences() {
        return PreferencesManager.getInstance().getApplicationPreferences(CLIENT_PREFERENCES);
    }
}
