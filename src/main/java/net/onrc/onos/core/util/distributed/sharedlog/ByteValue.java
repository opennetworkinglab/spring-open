package net.onrc.onos.core.util.distributed.sharedlog;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.Beta;

import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.SnapShotValue;

// TODO register to Kryo?
// TODO Should this be final, or should we allow sub-class
// TODO Not sure if reusing for SnapShotValue is good idea
/**
 * Regular Log Map Value.
 */
@Beta
@Immutable
public final class ByteValue implements LogValue, SnapShotValue {

    private final byte[] bytes;

    /**
     * Construct ByteValue.
     *
     * @param bytes must not be null
     */
    public ByteValue(final byte[] bytes) {
        this.bytes = Arrays.copyOf(checkNotNull(bytes), bytes.length);
    }

    /**
     * Gets the stored bytes.
     *
     * @return serialized bytes
     */
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    // TODO toString?
}
