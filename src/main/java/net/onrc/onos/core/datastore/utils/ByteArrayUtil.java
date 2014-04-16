package net.onrc.onos.core.datastore.utils;

import java.nio.ByteBuffer;

public final class ByteArrayUtil {

    // Suppresses default constructor, ensuring non-instantiability.
    private ByteArrayUtil() {
    }

    /**
     * Returns a StringBuilder with each byte in {@code bytes}
     * converted to a String with {@link Integer#toHexString(int)},
     * separated by {@code sep}.
     *
     * @param bytes byte array to convert
     * @param sep   separator between each bytes
     * @return {@code bytes} converted to a StringBuilder
     */
    public static StringBuilder toHexStringBuilder(final byte[] bytes,
                                                 final String sep) {
        return toHexStringBuilder(bytes, sep, new StringBuilder());
    }

    /**
     * Returns a StringBuilder with each byte in {@code bytes}
     * converted to a String with {@link Integer#toHexString(int)},
     * separated by {@code sep}.
     *
     * @param bytes byte array to convert
     * @param sep   separator between each bytes
     * @param buf   StringBuilder to append to.
     * @return {@code buf}
     */
    public static StringBuilder toHexStringBuilder(final byte[] bytes,
                                                 final String sep, final StringBuilder buf) {
        if (bytes == null) {
            return buf;
        }

        ByteBuffer wrap = ByteBuffer.wrap(bytes);

        boolean hasWritten = false;
        while (wrap.hasRemaining()) {
            if (hasWritten) {
                buf.append(sep);
            }
            buf.append(Integer.toHexString(wrap.get()));
            hasWritten = true;
        }

        return buf;
    }
}
