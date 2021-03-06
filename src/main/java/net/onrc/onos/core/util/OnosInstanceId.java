package net.onrc.onos.core.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.Immutable;

/**
 * The class representing an ONOS Instance ID.
 *
 * This class is immutable.
 */
@Immutable
public final class OnosInstanceId {
    private final String id;

    /**
     * Default constructor for serializer.
     */
    @Deprecated
    protected OnosInstanceId() {
        id = "(should never see this)";
    }

    /**
     * Constructor from a string value.
     *
     * @param id the value to use.
     */
    public OnosInstanceId(String id) {
        this.id = checkNotNull(id);
        checkArgument(!id.isEmpty(), "Empty ONOS Instance ID");
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof OnosInstanceId)) {
            return false;
        }

        OnosInstanceId that = (OnosInstanceId) obj;
        return this.id.equals(that.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
