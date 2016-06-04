package org.protege.editor.owl.client.admin.model;

import com.google.common.base.Objects;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOption {
    private String key;
    private Set<String> values;

    public ProjectOption(String key, Set<String> values) {
        this.key = checkNotNull(key);
        this.values = checkNotNull(values);
    }

    public String getKey() {
        return key;
    }

    public Set<String> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectOption)) {
            return false;
        }
        ProjectOption that = (ProjectOption) o;
        return Objects.equal(key, that.key) &&
                Objects.equal(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, values);
    }
}
