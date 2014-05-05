package net.onrc.onos.core.datastore.utils;

/**
 * Interface to serialize object into byte[].
 *
 * Serializer instance is expected to be functional even if the
 * instance was shared among multiple threads.
 */
public interface Serializer {
    /**
     * Serializes a given object.
     *
     * @param obj the object to serialize
     * @return binary representation of the serialized object
     */
    public byte[] serialize(final Object obj);

    /**
     * Deserializes a given byte array.
     *
     * @param bytes binary representation of an Object
     * @return deserialized object
     */
    public <T> T deserialize(final byte[] bytes);
}
