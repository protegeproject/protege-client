package org.protege.editor.owl.client.api;

import javax.annotation.Nonnull;

public class UserInfo {

    private String id;
    private String name;
    private String emailAddress;
    // used as token for authentication of calls after login
    private String nonce;

    public UserInfo(@Nonnull String id, String name, String emailAddress, String tok) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
        this.nonce = tok;
    }
    
    public String getNonce() {
    	return nonce;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ID: " + id);
        if (name != null || !name.isEmpty()) {
            sb.append(", Name: " + name);
        }
        if (emailAddress != null || !emailAddress.isEmpty()) {
            sb.append(", Email: " + emailAddress);
        }
        return "[" + sb.toString() + "]";
    }
}
