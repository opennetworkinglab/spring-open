package net.onrc.onos.core.util.distributed.sharedlog;

import com.google.common.annotations.Beta;

/**
 * ID object to identify each SharedLogObject.
 * <p/>
 * Class implementing this interface must implement equals and hashCode.
 */
@Beta
public interface SharedLogObjectID {

    /**
     * String name for SharedLogObject.
     *
     * @return String name for SharedLogObject
     */
    public String getObjectName();
}
