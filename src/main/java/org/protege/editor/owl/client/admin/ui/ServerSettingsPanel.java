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
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.util.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
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
    private ClientSession session;
    private Config config = null;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public ServerSettingsPanel(OWLEditorKit editorKit) {
    	this.editorKit = checkNotNull(editorKit);
    	configManager = AdminTabManager.get(editorKit);
    	configManager.addListener(tabListener);
    	session = ClientSession.getInstance(editorKit);
    	session.addListener(sessionListener);
    	if (session.getActiveClient() != null) {
    		config = session.getActiveClient().getConfig();
    	}
    	initUi();
    }

    private AdminTabListener tabListener = event -> {
        if (event.equals(AdminTabEvent.CONFIGURATION_CHANGED)) {
        	config = session.getActiveClient().getConfig();
            removeAll();
            initUi();
        }
    };

    private ClientSessionListener sessionListener = event -> {
        if(event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGIN) || event.hasCategory(ClientSessionChangeEvent.EventCategory.USER_LOGOUT)) {
            if (session.getActiveClient() != null) {
            	config = session.getActiveClient().getConfig();
            } else {
            	config = null;
            }
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
        hostList.addKeyListener(hostKeyListener);
        hostList.addMouseListener(hostMouseListener);

        rootList = new MList() {
            @Override
            protected void handleEdit() {
                editRoot();
            }
        };
        rootList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rootList.setCellRenderer(new RootListCellRenderer());
        rootList.addListSelectionListener(rootListListener);
        rootList.addKeyListener(rootKeyListener);
        rootList.addMouseListener(rootMouseListener);

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
        settingsList.addKeyListener(keyAdapter);
        settingsList.addMouseListener(mouseAdapter);
    }

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (settingsList.getSelectedValue() instanceof PropertyListHeaderItem) {
                    addProperty();
                } else {
                    editProperty();
                }
            }
        }
    };

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                if (settingsList.getSelectedValue() instanceof PropertyListHeaderItem) {
                    addProperty();
                } else {
                    editProperty();
                }
            }
        }
    };

    private KeyAdapter rootKeyListener = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && rootList.getSelectedValue() instanceof RootListItem) {
                editRoot();
            }
        }
    };

    private MouseListener rootMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && rootList.getSelectedValue() instanceof RootListItem) {
                editRoot();
            }
        }
    };

    private KeyAdapter hostKeyListener = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && hostList.getSelectedValue() instanceof HostListItem) {
                editHost();
            }
        }
    };

    private MouseListener hostMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && hostList.getSelectedValue() instanceof HostListItem) {
                editHost();
            }
        }
    };

    private void addProperty() {
        if(config != null && config.canUpdateServerConfig()) {
            boolean added = PropertyDialogPanel.showDialog(editorKit);
            if (added) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            }
            refresh();
        }
    }

    private void deleteProperty() {
        if(config != null && config.canUpdateServerConfig()) {
            Object selectedObj = settingsList.getSelectedValue();
            if (selectedObj instanceof PropertyListItem) {
                String propertyName = ((PropertyListItem) selectedObj).getPropertyName();
                int res = JOptionPaneEx.showConfirmDialog(editorKit.getWorkspace(), "Delete Property '" + propertyName + "'",
                        new JLabel("Proceed to delete property '" + propertyName + "'?"),
                        JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                if (res != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    config.unsetServerProperty(propertyName);
                } catch (AuthorizationException | ClientRequestException e) {
                    ErrorLogPanel.showErrorDialog(e);
                }
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
                refresh();
            }
        }
    }

    private void editProperty() {
        if(config != null && config.canUpdateServerConfig()) {
            boolean edited = PropertyDialogPanel.showDialog(editorKit, selectedProperty);
            if (edited) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            }
            refresh();
        }
    }

    private void editRoot() {
        if(config != null && config.canUpdateServerConfig()) {
            boolean edited = RootDialogPanel.showDialog(editorKit, ((RootListItem) rootList.getSelectedValue()).getRoot());
            if (edited) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            }
            refresh();
        }
    }

    private void editHost() {
        if(config != null && config.canUpdateServerConfig()) {
            boolean edited = HostDialogPanel.showDialog(editorKit, ((HostListItem) hostList.getSelectedValue()).getHost());
            if (edited) {
                configManager.statusChanged(AdminTabEvent.CONFIGURATION_CHANGED);
            }
            refresh();
        }
    }

    private void listHost() {
    	ArrayList<Object> data = new ArrayList<>();
    	data.add(new HostListHeaderItem());
    	if(config != null) {
    		if (config.canUpdateServerConfig()) {
    			data.add(new HostListItem(config.getHost()));
    		}
    	}
    	hostList.setListData(data.toArray());
    }

    private void listRoot() {
    	ArrayList<Object> data = new ArrayList<>();
    	data.add(new RootListHeaderItem());
    	if(config != null) {
    		if (config.canUpdateServerConfig()) {
    			data.add(new RootListItem(config.getRootDirectory()));
    		}
    	}
    	rootList.setListData(data.toArray());
    }

    private void listProperties() {
    	ArrayList<Object> data = new ArrayList<>();
    	data.add(new PropertyListHeaderItem());
    	if(config != null) {
    		if (config.canUpdateServerConfig()) {
    			Map<String, String> propertiesMap = config.getServerProperties();
    			List<String> keyset = new ArrayList<>(propertiesMap.keySet());
    			Collections.sort(keyset);
    			data.addAll(keyset.stream().map(key -> new PropertyListItem(key, propertiesMap.get(key))).collect(Collectors.toList()));
    		}
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
            return (config != null && config.canUpdateServerConfig());
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
            return (config != null && config.canUpdateServerConfig());
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
            return (config != null && config.canUpdateServerConfig());
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
            return (config != null && config.canUpdateServerConfig());
        }

        @Override
        public void handleEdit() {

        }

        @Override
        public boolean isDeleteable() {
            return (config != null && config.canUpdateServerConfig());
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
        session.removeListener(sessionListener);
    }
}
