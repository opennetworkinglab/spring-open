package net.onrc.onos.core.util.distributed.sharedlog.hazelcast;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.onrc.onos.core.util.distributed.sharedlog.ByteValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue;
import net.onrc.onos.core.util.distributed.sharedlog.internal.NoOp;
import net.onrc.onos.core.util.serializers.HazelcastSerializationConstants;

import com.google.common.annotations.Beta;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

/**
 * Serializer for LogValue.
 */
@Beta
public class LogValueSerializer implements StreamSerializer<LogValue> {

    private static final Logger log = LoggerFactory
            .getLogger(LogValueSerializer.class);

    //
    // | LogValue type (1 byte) | (type specific) |
    //

    // 1st byte identifying LogValue type
    private static final int NO_OP = 0;
    private static final int BYTE_VALUE = 1;

    @Override
    public int getTypeId() {
        return HazelcastSerializationConstants.LOG_VALUE_TYPE_ID;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void write(ObjectDataOutput out, LogValue object) throws IOException {

        if (object instanceof ByteValue) {
            out.writeByte(BYTE_VALUE);
            ByteValue bytes = (ByteValue) object;
            out.writeObject(bytes.getBytes());

        } else if (object instanceof NoOp) {
            out.writeByte(NO_OP);

        } else {
            log.error("Unexpected type encountered: {}", object);
        }
    }

    @Override
    public LogValue read(ObjectDataInput in) throws IOException {

        final int typeId = in.readByte();
        switch(typeId) {
        case BYTE_VALUE:
            return new ByteValue(in.<byte[]>readObject());

        case NO_OP:
            return NoOp.VALUE;

        default:
        }

        log.error("Unexpected type encountered: {}", typeId);
        return null;
    }
}
