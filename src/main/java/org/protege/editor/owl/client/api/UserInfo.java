package org.protege.editor.owl.client.api;

import javax.annotation.Nonnull;

public class UserInfo {

    private String id;
    private String name;
    private String emailAddress;

    public UserInfo(@Nonnull String id, String name, String emailAddress) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
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
