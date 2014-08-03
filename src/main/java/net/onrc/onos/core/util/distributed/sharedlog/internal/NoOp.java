package net.onrc.onos.core.util.distributed.sharedlog.internal;

import com.google.common.annotations.Beta;


// TODO register to Kryo?
// TODO Not sure if reusing for SnapShotValue is good idea
/**
 * Value representing this log entry was abandoned.
 */
@Beta
public final class NoOp implements LogValue, SnapShotValue {

    /**
     * The NoOp.
     */
    public static final NoOp VALUE = new NoOp();

    /**
     * Constructor.
     */
    protected NoOp() {}

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NoOp) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
