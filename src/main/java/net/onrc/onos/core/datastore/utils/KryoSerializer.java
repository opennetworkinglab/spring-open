package net.onrc.onos.core.datastore.utils;

import javax.annotation.concurrent.ThreadSafe;

import net.onrc.onos.core.datastore.DataStoreClient;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * {@link Serializer} implementation using Kryo.
 */
@ThreadSafe
public final class KryoSerializer
            implements Serializer {

    private final ThreadLocalKryo kryo;

    /**
     * Thread safe Serializer implementation using Kryo.
     *
     * @param expectedTypes list of classes expected to be serialized
     */
    public KryoSerializer(Class<?>... expectedTypes) {
        kryo = new ThreadLocalKryo(expectedTypes);
    }

    @Override
    public byte[] serialize(Object obj) {
        // 1MB RAMCloud limit
        Output out = new Output(DataStoreClient.MAX_VALUE_BYTES);
        kryo.get().writeClassAndObject(out, obj);
        return out.toBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        Input in = new Input(bytes);
        @SuppressWarnings("unchecked")
        T obj = (T) kryo.get().readClassAndObject(in);
        return obj;
    }
}
