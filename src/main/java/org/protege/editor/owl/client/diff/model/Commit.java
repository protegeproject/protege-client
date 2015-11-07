package org.protege.editor.owl.client.diff.model;

import org.protege.owl.server.api.UserId;

import java.util.Date;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public interface Commit extends Comparable<Commit> {

    /**
     * Get the user identifier of the commit author
     *
     * @return User identifier of commit author
     */
    UserId getUserId();

    /**
     * Get the commit date
     *
     * @return Commit date
     */
    Date getDate();

    /**
     * Get the commit comment
     *
     * @return Commit comment
     */
    String getComment();

    /**
     * Get the commit hashcode
     *
     * @return Commit hashcode
     */
    int getHashcode();

}
