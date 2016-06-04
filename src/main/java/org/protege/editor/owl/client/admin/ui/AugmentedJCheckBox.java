package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.api.MetaprojectObject;

import javax.swing.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class AugmentedJCheckBox<T extends MetaprojectObject> extends JCheckBox {
    private T obj;

    public AugmentedJCheckBox(T obj) {
        super();
        this.obj = checkNotNull(obj);
    }

    public T getObject() {
        return obj;
    }
}
