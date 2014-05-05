package net.onrc.onos.core.datastore.utils;

import com.esotericsoftware.kryo.Kryo;

/**
 * Helper class to create thread local instance of Kryo.
 */
public final class ThreadLocalKryo extends ThreadLocal<Kryo> {
    private final Class<?>[] expectedTypes;

    /**
     * Constructor specifying expected classes to be serialized using this
     * Kryo instance.
     * <p/>
     * @param expectedTypes list of .class to register to Kryo
     */
    public ThreadLocalKryo(Class<?>... expectedTypes) {
        this.expectedTypes = expectedTypes;
    }

    @Override
    protected Kryo initialValue() {
        Kryo kryo = new Kryo();
        //            kryo.setRegistrationRequired(true);
        //            kryo.setReferences(false);
        for (Class<?> type : expectedTypes) {
            kryo.register(type);
        }
        return kryo;
    }
}
