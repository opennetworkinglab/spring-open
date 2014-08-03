package net.onrc.onos.core.util.serializers;

/**
 * Hazelcast serializer related constants.
 */
public final class HazelcastSerializationConstants {

    // Type ID

    /**
     * Type ID for {@link net.onrc.onos.core.datastore.hazelcast.HZTable.VersionedValue}.
     */
    public static final int VERSIONED_VALUE_TYPE_ID = 1;

    /**
     * Type ID for {@link net.onrc.onos.core.util.distributed.sharedlog.internal.LogValue}.
     */
    public static final int LOG_VALUE_TYPE_ID = 2;


    // Factory ID

    // WARNING: hard coded value exist in hazelcast.xml
    /**
     * Factory ID for {@link net.onrc.onos.core.datastore.hazelcast.VersionedValueSerializableFactory}.
     */
    public static final int VERSIONED_VALUE_SERIALIZABLE_FACTORY_ID = 1;

    /**
     * Avoid instantiation.
     */
    private HazelcastSerializationConstants() {}
}
