package net.onrc.onos.core.util;

import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.primitives.UnsignedInts;

/**
 * Immutable class representing a port number.
 * <p/>
 * Current implementation supports only OpenFlow 1.0 (16 bit unsigned) port number.
 */
public final class PortNumber {

    private final int value;

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
        this.value = other.value;
    }

    /**
     * Constructor from a short integer value.
     *
     * @param value the value to use.
     */
    public PortNumber(short value) {
        this.value = (int) shortToUnsignedLong(value);
    }

    /**
     * Constructor from an int.
     *
     * @param value the value to use. (Value will not be validated in any way.)
     */
    PortNumber(int value) {
        this.value = value;
    }

    // TODO We may want a factory method version
    //      which does the range validation of parsed value.
    /**
     * Constructor from decimal string.
     *
     * @param decStr decimal string representation of a port number
     */
    public PortNumber(String decStr) {
        this(decStr, 10);
    }

    /**
     * Constructor from string.
     *
     * @param s string representation of a port number
     * @param radix the radix to use while parsing {@code s}
     */
    public PortNumber(String s, int radix) {
        this(UnsignedInts.parseUnsignedInt(s, radix));
    }

    /**
     * Convert unsigned short to unsigned long.
     *
     * @param portno unsigned integer representing port number
     * @return port number as unsigned long
     */
    public static long shortToUnsignedLong(short portno) {
        return UnsignedInts.toLong(0xffff & portno);
    }

    /**
     * Gets the port number as short.
     * <p/>
     * Note: User of this method needs to be careful, handling unsigned value.
     * @return number as short
     */
    public short shortValue() {
        return (short) value;
    }

    /**
     * Gets the value of the port as unsigned integer.
     *
     * @return the value of the port.
     */
    @JsonProperty("value")
    public long value() {
        // TODO Will require masking when we start storing 32bit port number.
        return value;
    }

    /**
     * Convert the port value as unsigned integer to a string.
     *
     * @return the port value as a string.
     */
    @Override
    public String toString() {
        return UnsignedInts.toString(value);
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
