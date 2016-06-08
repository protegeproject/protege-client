package org.protege.editor.owl.client.admin.model;

import com.google.common.base.Objects;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ProjectOption {
    private String key;
    private List<String> values;

    public ProjectOption(String key, List<String> values) {
        this.key = checkNotNull(key);
        this.values = checkNotNull(values);
    }

    public String getKey() {
        return key;
    }

    public List<String> getValues() {
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
