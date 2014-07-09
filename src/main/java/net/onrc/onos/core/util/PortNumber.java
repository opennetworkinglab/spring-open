package net.onrc.onos.core.util;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Immutable class representing a port number.
 * <p/>
 * Current implementation supports only OpenFlow 1.0 (16 bit unsigned) port number.
 */
public final class PortNumber {

    private final short value;

    /**
     * Default constructor.
     */
    protected PortNumber() {
        this.value = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from.
     */
    public PortNumber(PortNumber other) {
        this.value = other.value();
    }

    /**
     * Constructor from a short integer value.
     *
     * @param value the value to use.
     */
    public PortNumber(short value) {
        this.value = value;
    }

    /**
     * Get the value of the port.
     *
     * @return the value of the port.
     */
    @JsonProperty("value")
    public short value() {
        return value;
    }

    /**
     * Convert the port value to a string.
     *
     * @return the port value as a string.
     */
    @Override
    public String toString() {
        return Short.toString(this.value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PortNumber)) {
            return false;
        }

        PortNumber otherPort = (PortNumber) other;

        return value == otherPort.value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
