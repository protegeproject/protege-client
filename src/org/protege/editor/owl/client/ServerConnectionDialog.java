package org.protege.editor.owl.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.protege.owl.server.api.ClientConnection;
import org.protege.owl.server.api.ServerOntologyInfo;
import org.protege.owl.server.connection.servlet.ServletClientConnection;
import org.protege.owl.server.exception.RemoteQueryException;
import org.protege.owlapi.model.ProtegeOWLOntologyManager;

public class ServerConnectionDialog extends JDialog {
    private static final long serialVersionUID = -1055466094886319846L;
    private static Logger logger = Logger.getLogger(ServerConnectionDialog.class);

    private ProtegeOWLOntologyManager manager;
    private JTextField host;
    private ClientConnection connection;
    
    private JTable connectionResults;
    private JPanel connectionInfoPanel;
    private ServerOntologyInfo[] info = new ServerOntologyInfo[0];
    
    
    public ServerConnectionDialog(Window owner, ProtegeOWLOntologyManager manager) {
        super(owner);
        this.manager = manager;
        setModal(true);
        setLayout(new BorderLayout());
        
        JScrollPane content = new JScrollPane(getConnectionResults());
        content.setVerticalScrollBar(new JScrollBar());
        add(content, BorderLayout.CENTER);
        add(getConnectionInfoPanel(), BorderLayout.SOUTH);
        pack();
    }
    
    protected JTable getConnectionResults() {
        if (connectionResults == null) {
            connectionResults = new JTable();
            connectionResults.setModel(new ConnectionResultsTableModel());
            connectionResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel columnModel = connectionResults.getColumnModel();
            Dimension sampleShortNameSize = new JLabel("NCIThesaurusXXX").getPreferredSize();
            Dimension sampleOntologyIRISize = new JLabel("http://www.co-ode.org/ontologies/amino-acid/2006/05/18/amino-acid.owl").getPreferredSize();
            columnModel.getColumn(Column.SHORT_NAME.ordinal()).setPreferredWidth((int) sampleShortNameSize.getWidth());
            columnModel.getColumn(Column.ONTOLOGY_NAME.ordinal()).setPreferredWidth((int) sampleOntologyIRISize.getWidth());
        }
        return connectionResults;      
    }
    
    public JComponent getConnectionInfoPanel() {
        if (connectionInfoPanel == null) {
            connectionInfoPanel = new JPanel();
            connectionInfoPanel.setLayout(new FlowLayout());
            host = new JTextField();
            host.setPreferredSize(new JTextField("smi-protege.stanford.edu").getPreferredSize());
            connectionInfoPanel.add(host);
            final JButton connect = new JButton("Connect");
            connect.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getClientConnection() == null) {
                        try {
                            setClientConnection(new ServletClientConnection(manager, host.getText()));
                        }
                        catch (RemoteQueryException rqe) {
                            logger.warn("Could not connect to server " + host.getText(), rqe);
                        }
                    }
                    else {
                        try {
                            getClientConnection().dispose();
                        }
                        finally {
                            try {
                                setClientConnection(null);
                            }
                            catch (RemoteQueryException rqe) {
                                logger.error("Shouldn't", rqe);
                            }
                        }
                    }
                    connect.setText(connection != null ? "Disconnect" : "Connect");
                }
            });
            connectionInfoPanel.add(connect);
        }
        return connectionInfoPanel;
    }
    
    public ClientConnection getClientConnection() {
        return connection;
    }
    
    protected void setClientConnection(ClientConnection connection) throws RemoteQueryException {
        this.connection = connection;
        if (connection == null) {
            info = new ServerOntologyInfo[0];
        }
        else {
            Map<String, ServerOntologyInfo> map = connection.getOntologyInfoByShortName(true);
            List<String> shortNames = new ArrayList<String>(map.keySet());
            Collections.sort(shortNames);
            info = new ServerOntologyInfo[shortNames.size()];
            for (int i = 0; i < shortNames.size(); i++) {
                info[i] = map.get(shortNames.get(i));
            }
        }
        ((AbstractTableModel) getConnectionResults().getModel()).fireTableStructureChanged();
    }
    
    public ServerOntologyInfo getSelectedOntology() {
        int row = getConnectionResults().getSelectedRow();
        if (row < 0) {
            return null;
        }
        return info[row];
    }

    public enum Column {
        SHORT_NAME {
            @Override
            public Object getValue(ServerOntologyInfo info) {
                return info.getShortName();
            }
        },
        ONTOLOGY_NAME {
            @Override
            public Object getValue(ServerOntologyInfo info) {
                return info.getOntologyName();
            }
        };
        
        public abstract Object getValue(ServerOntologyInfo info);
    }
    
    protected class ConnectionResultsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 2080023622581853513L;

        @Override
        public int getColumnCount() {
            return Column.values().length;
        }
        
        @Override
        public String getColumnName(int column) {
            switch (Column.values()[column]) {
            case SHORT_NAME:
                return "Short Name";
            case ONTOLOGY_NAME:
                return "Ontology IRI";
            default:
                return "";
            }
        }

        @Override
        public int getRowCount() {
            return info.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return Column.values()[columnIndex].getValue(info[rowIndex]);
        }
    }
    
    public static class Result {
        private ClientConnection connection;
        private ServerOntologyInfo info;
        public Result(ClientConnection connection, ServerOntologyInfo info) {
            this.connection = connection;
            this.info = info;
        }
        public ClientConnection getConnection() {
            return connection;
        }
        public ServerOntologyInfo getInfo() {
            return info;
        }
    }
    
    
}
