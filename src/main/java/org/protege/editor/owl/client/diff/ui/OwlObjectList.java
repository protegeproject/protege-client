package org.protege.editor.owl.client.diff.ui;

import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListItem;
import org.protege.editor.owl.OWLEditorKit;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.Vector;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OwlObjectList extends MList {
    private static final long serialVersionUID = -6582688251102782964L;

    /**
     * Constructor
     *
     * @param editorKit OWL editor kit
     */
    public OwlObjectList(OWLEditorKit editorKit) {
        setCellRenderer(new ListItemRenderer(editorKit, true, true));
        setFixedCellHeight(30);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(new DisabledItemSelectionModel());
    }

    public <E extends Object> void setObjects(Set<E> objects) {
        Vector<ListItem<E>> items = new Vector<>();
        for(E e : objects) {
            items.add(new ListItem<>(e));
        }
        setListData(items);
    }

    public <E extends Object> void setObject(E object) {
        Vector<ListItem<E>> items = new Vector<>();
        items.add(new ListItem<>(object));
        setListData(items);
    }


    private class ListItemRenderer<E extends Object> implements ListCellRenderer<ListItem<E>> {
        private OwlCellRenderer renderer;

        public ListItemRenderer(OWLEditorKit owlEditorKit, boolean renderExpression, boolean renderIcon) {
            renderer = new OwlCellRenderer(owlEditorKit, renderExpression, renderIcon);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ListItem<E>> list, ListItem<E> value, int index, boolean isSelected, boolean cellHasFocus) {
            return renderer.getListCellRendererComponent(list, value.getObject(), index, isSelected, cellHasFocus);
        }
    }


    private class ListItem<E extends Object> implements MListItem {
        private E object;

        public ListItem(E object) {
            this.object = checkNotNull(object);
        }

        public boolean isEditable() {
            return false;
        }

        public void handleEdit() {
            // do nothing
        }

        public boolean isDeleteable() {
            return false;
        }

        public boolean handleDelete() {
            return false;
        }

        public String getTooltip() {
            return "";
        }

        public E getObject() {
            return object;
        }
    }


    private class DisabledItemSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(-1, -1);
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            super.addSelectionInterval(-1, -1);
        }
    }
}