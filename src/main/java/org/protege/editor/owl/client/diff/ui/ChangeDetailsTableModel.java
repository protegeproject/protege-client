package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.owl.client.diff.model.Change;

import javax.swing.table.AbstractTableModel;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public abstract class ChangeDetailsTableModel extends AbstractTableModel {

    /**
     * Set the change for this change details table
     *
     * @param change    Change
     */
    abstract void setChange(Change change);

}
