package net.onrc.onos.core.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * {@link PortNumber} related utilities.
 */
public final class PortNumberUtils {

    /**
     * Gets {@link PortNumber} for specified port.
     *
     * @param ofVersion OF version to use translating {@code portNumber}
     * @param portNumber treated as uint16 if {@link OFVersion#OF_10}
     *                  uint32 otherwise
     * @return {@link PortNumber}
     */
    public static PortNumber openFlow(OFVersion ofVersion, int portNumber) {
        if (ofVersion == OFVersion.OF_10) {
            return PortNumber.uint16(toOF10(portNumber));
        } else {
            return PortNumber.uint32(portNumber);
        }
    }

    /**
     * Gets {@link PortNumber} for specified port.
     *
     * @param desc {@link OFPortDesc}
     * @return {@link PortNumber}
     */
    public static PortNumber openFlow(final OFPortDesc desc) {
        if (checkNotNull(desc).getVersion() == OFVersion.OF_10) {
            return PortNumber.uint16(desc.getPortNo().getShortPortNumber());
        } else {
            return PortNumber.uint32(desc.getPortNo().getPortNumber());
        }
    }

    /**
     * Validate OF1.0 port number.
     *
     * @param of10PortNumber 0F1.0 port number or {@link OFPort#getPortNumber()}
     * @return OF1.0 port number
     * @throws IllegalArgumentException if out of valid OF1.0 range
     */
    public static short toOF10(final int of10PortNumber) {
        try {
            // if the input was from OFPort#getPortNumber()
            // some named/reserved port number to OF1.0 constants.
            return OFPort.ofInt(of10PortNumber).getShortPortNumber();
        } catch (IllegalArgumentException e) {
            // OFPort#getShortPortNumber will rejects OF1.0 special ports
            if (0xFFf8 <= of10PortNumber && of10PortNumber <= 0xFFff) {
                // allow OF1.0 Fake output "ports" to pass
                return (short) (0xFFFF & of10PortNumber);
            } else if (0xFF00 == of10PortNumber) {
                // allow OFPP_MAX
                return (short) (0xFFFF & of10PortNumber);
            }
            throw e;
        }
    }

    /**
     * Avoid instantiation.
     */
    private PortNumberUtils() {}

}
