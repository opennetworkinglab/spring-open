package net.onrc.onos.core.datastore.hazelcast;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * IdentifiedDataSerializableFactory for HZTable.VersionedValue class.
 */
public class VersionedValueSerializableFactory implements
        DataSerializableFactory {
    // revisit these magic numbers
    /**
     * IdentifiedDataSerializable Factory ID.
     */
    public static final int FACTORY_ID = 1;

    /**
     * IdentifiedDataSerializable type ID for HZTable.VersionedValue class.
     */
    public static final int VERSIONED_VALUE_ID = 1;

    @Override
    public IdentifiedDataSerializable create(final int typeId) {
        switch (typeId) {
            case VERSIONED_VALUE_ID:
                return new HZTable.VersionedValue();

            default:
                return null;
        }
    }
}
