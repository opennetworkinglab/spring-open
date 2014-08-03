package net.onrc.onos.core.datastore.hazelcast;

import net.onrc.onos.core.util.serializers.HazelcastSerializationConstants;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * IdentifiedDataSerializableFactory for HZTable.VersionedValue class.
 */
public class VersionedValueSerializableFactory implements
        DataSerializableFactory {

    @Override
    public IdentifiedDataSerializable create(final int typeId) {
        switch (typeId) {
            case HazelcastSerializationConstants.VERSIONED_VALUE_TYPE_ID:
                return new HZTable.VersionedValue();

            default:
                return null;
        }
    }
}
