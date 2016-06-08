package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.Host;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListItem;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.AdminTabManager;
import org.protege.editor.owl.client.admin.model.AdminTabEvent;
import org.protege.editor.owl.client.admin.model.AdminTabListener;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.server.api.exception.AuthorizationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ServerSettingsPanel extends JPanel implements Disposable {
    private static final long serialVersionUID = 928076570820861254L;
    private OWLEditorKit editorKit;
    private AdminTabManager configManager;
    private MList hostList, rootList, settingsList;
    private PropertyListItem selectedProperty;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public ServerSettingsPanel(OWLEditorKit editorKit) {
        this.editorKit = checkNotNull(editorKit);
        configManager = AdminTabManager.get(editorKit);
        configManager.addListener(tabListener);
        initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.CONFIGURATION_CHANGED)) {
            removeAll();
            initUi();
        }
    };

    private ListSelectionListener settingsListListener = e -> {
        if(settingsList.getSelectedValue() != null && !e.getValueIsAdjusting()) {
            if(settingsList.getSelectedValue() instanceof PropertyListItem) {
                selectedProperty = (PropertyListItem) settingsList.getSelectedValue();
            }
            hostList.clearSelection();
            rootList.clearSelection();
        }
    };

    private ListSelectionListener rootListListener = e -> {
        if(rootList.getSelectedValue() != null && !e.getValueIsAdjusting()) {
            hostList.clearSelection();
            settingsList.clearSelection();
        }
    };

    private ListSelectionListener hostListListener = e -> {
        if(hostList.getSelectedValue() != null && !e.getValueIsAdjusting()) {
            rootList.clearSelection();
            settingsList.clearSelection();
        }
    };

    private void initUi() {
        setupLists();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(GuiUtils.MATTE_BORDER);

        JPanel hostPanel = new JPanel(new BorderLayout());
        JScrollPane hostScrollPane = new JScrollPane(hostList);
        hostScrollPane.setBorder(new EmptyBorder(3, 0, 0, 0));
        hostPanel.add(hostScrollPane, BorderLayout.CENTER);
        hostPanel.setMinimumSize(new Dimension(0, 70));
        hostPanel.setPreferredSize(new Dimension(0, 70));
        hostPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));

        JPanel rootPanel = new JPanel(new BorderLayout());
        JScrollPane rootScrollPane = new JScrollPane(rootList);
        rootScrollPane.setBorder(new EmptyBorder(3, 0, 0, 0));
        rootPanel.add(rootScrollPane, BorderLayout.CENTER);
        rootPanel.setMinimumSize(new Dimension(0, 70));
        rootPanel.setPreferredSize(new Dimension(0, 70));
        rootPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));

        JPanel settingsPanel = new JPanel(new BorderLayout());
        JScrollPane settingScrollPane = new JScrollPane(settingsList);
        settingScrollPane.setBorder(new EmptyBorder(3, 0, 0, 0));
        settingsPanel.add(settingScrollPane, BorderLayout.CENTER);

        add(hostPanel);
        add(rootPanel);
        add(settingsPanel);

        listProperties();
        listRoot();
        listHost();
    }

    private void setupLists() {
        hostList = new MList() {
            @Override
            protected void handleEdit() {
                editHost();
            }
        };
        hostList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hostList.setCellRenderer(new HostListCellRenderer());
        hostList.addListSelectionListener(hostListListener);

        rootList = new MList() {
            @Override
            protected void handleEdit() {
                editRoot();
            }
        };
        rootList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rootList.setCellRenderer(new RootListCellRenderer());
        rootList.addListSelectionListener(rootListListener);

        settingsList = new MList() {
            @Override
            protected void handleAdd() {
                addProperty();
            }

            @Override
            protected void handleDelete() {
                deleteProperty();
            }

            @Override
            protected void handleEdit() {
                editProperty();
            }
        };
        settingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        settingsList.setCellRenderer(new PropertyListCellRenderer());
        settingsList.addListSelectionListener(settingsListListener);
    }

    private void addProperty() {
        boolean added = PropertyDialogPanel.showDialog(editorKit);
        if(added) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
        }
        refresh();
    }

    private void deleteProperty() {
        Object selectedObj = settingsList.getSelectedValue();
        if (selectedObj instanceof PropertyListItem) {
            String propertyName = ((PropertyListItem) selectedObj).getPropertyName();
            int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Property '" + propertyName + "'",
                    new JLabel("Proceed to delete property '" + propertyName + "'?"),
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
            if (res != JOptionPane.OK_OPTION) {
                return;
            }
            Client client = ClientSession.getInstance(editorKit).getActiveClient();
            try {
                client.unsetServerProperty(propertyName);
            } catch (AuthorizationException | ClientRequestException | RemoteException e) {
                ErrorLogPanel.showErrorDialog(e);
            }
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            refresh();
        }
    }

    private void editProperty() {
        boolean edited = PropertyDialogPanel.showDialog(editorKit, selectedProperty);
        if(edited) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
        }
        refresh();
    }

    private void editRoot() {
        boolean edited = RootDialogPanel.showDialog(editorKit, ((RootListItem)rootList.getSelectedValue()).getRoot());
        if(edited) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
        }
        refresh();
    }

    private void editHost() {
        boolean edited = HostDialogPanel.showDialog(editorKit, ((HostListItem)hostList.getSelectedValue()).getHost());
        if(edited) {
            configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
        }
        refresh();
    }

    private void listHost() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        ArrayList<Object> data = new ArrayList<>();
        data.add(new HostListHeaderItem());
        try {
            if(client != null) {
                data.add(new HostListItem(client.getHost()));
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        hostList.setListData(data.toArray());
    }

    private void listRoot() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        ArrayList<Object> data = new ArrayList<>();
        data.add(new RootListHeaderItem());
        try {
            if(client != null) {
                data.add(new RootListItem(client.getRootDirectory()));
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        rootList.setListData(data.toArray());
    }

    private void listProperties() {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        ArrayList<Object> data = new ArrayList<>();
        data.add(new PropertyListHeaderItem());
        try {
            if(client != null) {
                Map<String, String> propertiesMap = client.getServerProperties();
                List<String> keyset = new ArrayList<>(propertiesMap.keySet());
                Collections.sort(keyset);
                data.addAll(keyset.stream().map(key -> new PropertyListItem(key, propertiesMap.get(key))).collect(Collectors.toList()));
            }
        } catch (AuthorizationException | ClientRequestException | RemoteException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
        settingsList.setListData(data.toArray());
    }

    private void refresh() {
        revalidate();
        repaint();
    }

    /**
     * Host list header item
     */
    public class HostListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Host";
        }

        @Override
        public boolean canAdd() {
            return false;
        }
    }

    /**
     * Host list item
     */
    public class HostListItem implements MListItem {
        private Host host;

        /**
         * Constructor
         *
         * @param host Host
         */
        public HostListItem(Host host) {
            this.host = checkNotNull(host);
        }

        public Host getHost() {
            return host;
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return false;
        }

        @Override
        public boolean handleDelete() {
            return false;
        }

        @Override
        public String getTooltip() {
            return host.getUri().toString();
        }
    }

    /**
     * Root folder list header item
     */
    public class RootListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Server Root";
        }

        @Override
        public boolean canAdd() {
            return false;
        }
    }

    /**
     * Root folder list item
     */
    public class RootListItem implements MListItem {
        private String root;

        /**
         * Constructor
         *
         * @param root Server root folder
         */
        public RootListItem(String root) {
            this.root = checkNotNull(root);
        }

        public String getRoot() {
            return root;
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return false;
        }

        @Override
        public boolean handleDelete() {
            return false;
        }

        @Override
        public String getTooltip() {
            return root;
        }
    }

    /**
     * Properties list header item
     */
    public class PropertyListHeaderItem implements MListSectionHeader {

        @Override
        public String getName() {
            return "Properties";
        }

        @Override
        public boolean canAdd() {
            return true;
        }
    }

    /**
     * Properties list item
     */
    public class PropertyListItem implements MListItem {
        private String propertyName, propertyValue;

        /**
         * Constructor
         *
         * @param propertyName  Property name
         * @param propertyValue Property value
         */
        public PropertyListItem(String propertyName, String propertyValue) {
            this.propertyName = checkNotNull(propertyName);
            this.propertyValue = checkNotNull(propertyValue);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyValue() {
            return propertyValue;
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return true;
        }

        @Override
        public boolean handleDelete() {
            return true;
        }

        @Override
        public String getTooltip() {
            return propertyName;
        }
    }

    @Override
    public void dispose() {
        hostList.removeListSelectionListener(hostListListener);
        rootList.removeListSelectionListener(rootListListener);
        settingsList.removeListSelectionListener(settingsListListener);
        configManager.removeListener(tabListener);
    }
}
