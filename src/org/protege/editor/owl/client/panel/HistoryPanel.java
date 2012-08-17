package org.protege.editor.owl.client.panel;

import javax.swing.JPanel;

import org.protege.owl.server.api.Client;
import org.protege.owl.server.api.VersionedOWLOntology;

public class HistoryPanel extends JPanel {
    private Client client;
    private VersionedOWLOntology vont;
    
    public HistoryPanel(Client client, VersionedOWLOntology vont) {
        this.client = client;
        this.vont = vont;
    }
}
