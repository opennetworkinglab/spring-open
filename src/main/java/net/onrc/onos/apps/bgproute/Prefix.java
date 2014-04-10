package net.onrc.onos.apps.bgproute;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.google.common.net.InetAddresses;

/**
 * Represents an IP prefix.
 * <p/>
 * It is made up of an IP address and a number of significant bits in the
 * prefix (i.e. the size of the network part of the address).
 * E.g. {@code 192.168.0.0/16}.
 * <p/>
 * Currently only IPv4 is supported, so a prefix length can be up to 32 bits.
 */
public class Prefix {
    /**
     * The length of addresses this class can represent prefixes of, in bytes.
     */
    public static final int ADDRESS_LENGTH = 4;

    private final int prefixLength;
    private final byte[] address;

    // For verifying the arguments and pretty printing
    private final InetAddress inetAddress;

    /**
     * Class constructor, taking an byte array representing and IP address and
     * a prefix length.
     * <p/>
     * The valid values for addr and prefixLength are bounded by
     * {@link #ADDRESS_LENGTH}.
     * <p/>
     * A valid addr array satisfies
     * {@code addr.length == }{@value #ADDRESS_LENGTH}.
     * <p/>
     * A valid prefixLength satisfies
     * {@code (prefixLength > 0 && prefixLength <=} {@link Byte#SIZE}
     * {@code * }{@value #ADDRESS_LENGTH}{@code )}.
     *
     * @param addr a byte array representing the address
     * @param prefixLength length of the prefix of the specified address
     */
    public Prefix(byte[] addr, int prefixLength) {
        if (addr == null || addr.length != ADDRESS_LENGTH ||
                prefixLength < 0 || prefixLength > ADDRESS_LENGTH * Byte.SIZE) {
            throw new IllegalArgumentException();
        }

        address = canonicalizeAddress(addr, prefixLength);
        this.prefixLength = prefixLength;

        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Couldn't parse IP address", e);
        }
    }

    /**
     * Class constructor, taking an address in String format and a prefix
     * length. The address must be in dot-notation form (e.g. {@code 0.0.0.0}).
     *
     * @param strAddress a String representing the address
     * @param prefixLength length of the prefix of the specified address
     */
    public Prefix(String strAddress, int prefixLength) {
        byte[] addr = null;
        addr = InetAddresses.forString(strAddress).getAddress();

        if (addr == null || addr.length != ADDRESS_LENGTH ||
                prefixLength < 0 || prefixLength > ADDRESS_LENGTH * Byte.SIZE) {
            throw new IllegalArgumentException();
        }

        address = canonicalizeAddress(addr, prefixLength);
        this.prefixLength = prefixLength;

        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Couldn't parse IP address", e);
        }
    }

    private byte[] canonicalizeAddress(byte[] addressValue,
                                       int prefixLengthValue) {
        byte[] result = new byte[addressValue.length];

        if (prefixLengthValue == 0) {
            for (int i = 0; i < ADDRESS_LENGTH; i++) {
                result[i] = 0;
            }

            return result;
        }

        result = Arrays.copyOf(addressValue, addressValue.length);

        //Set all bytes after the end of the prefix to 0
        int lastByteIndex = (prefixLengthValue - 1) / Byte.SIZE;
        for (int i = lastByteIndex; i < ADDRESS_LENGTH; i++) {
            result[i] = 0;
        }

        byte lastByte = addressValue[lastByteIndex];
        byte mask = 0;
        byte msb = (byte) 0x80;
        int lastBit = (prefixLengthValue - 1) % Byte.SIZE;
        for (int i = 0; i < Byte.SIZE; i++) {
            if (i <= lastBit) {
                mask |= (msb >> i);
            }
        }

        result[lastByteIndex] = (byte) (lastByte & mask);

        return result;
    }

    /**
     * Gets the length of the prefix of the address.
     *
     * @return the prefix length
     */
    public int getPrefixLength() {
        return prefixLength;
    }

    /**
     * Gets the address.
     *
     * @return the address as a byte array
     */
    public byte[] getAddress() {
        return Arrays.copyOf(address, address.length);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Prefix)) {
            return false;
        }

        Prefix otherPrefix = (Prefix) other;

        return (Arrays.equals(address, otherPrefix.address)) &&
                (prefixLength == otherPrefix.prefixLength);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + prefixLength;
        hash = 31 * hash + Arrays.hashCode(address);
        return hash;
    }

    @Override
    public String toString() {
        return inetAddress.getHostAddress() + "/" + prefixLength;
    }

    /**
     * Print the prefix to a String showing the bits of the address.
     *
     * @return the bit-string of the prefix
     */
    public String printAsBits() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < address.length; i++) {
            byte b = address[i];
            for (int j = 0; j < Byte.SIZE; j++) {
                byte mask = (byte) (0x80 >>> j);
                result.append(((b & mask) == 0) ? "0" : "1");
                if (i * Byte.SIZE + j == prefixLength - 1) {
                    return result.toString();
                }
            }
            result.append(' ');
        }
        return result.substring(0, result.length() - 1);
    }
}
