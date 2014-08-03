package net.onrc.onos.core.util.distributed.sharedlog.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import net.onrc.onos.core.util.distributed.sharedlog.SharedLogObjectID;

/**
 * Simple String implementation of SharedLogObjectID.
 */
@Beta
public class StringID implements SharedLogObjectID {

    private final String id;

    /**
     * Constructor.
     *
     * @param id String
     */
    public StringID(final String id) {
        this.id = checkNotNull(id);
        checkArgument(!id.isEmpty(), "id cannot be empty String");
    }


    @Override
    public String getObjectName() {
        return id;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StringID other = (StringID) obj;
        return Objects.equal(id, other.id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
