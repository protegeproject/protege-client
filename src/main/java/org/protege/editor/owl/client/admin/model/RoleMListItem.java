package org.protege.editor.owl.client.admin.model;

import edu.stanford.protege.metaproject.api.Role;
import org.protege.editor.core.ui.list.MListItem;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface RoleMListItem extends MListItem {

    Role getRole();

}
