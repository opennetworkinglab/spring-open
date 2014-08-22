package net.onrc.onos.core.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import net.onrc.onos.core.util.serializers.SwitchPortSerializer;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * The class representing a Switch-Port.
 * This class is immutable.
 */
@JsonSerialize(using = SwitchPortSerializer.class)
@Immutable
public final class SwitchPort {
    private final Dpid dpid;            // The DPID of the switch
    private final PortNumber port;      // The port number on the switch

    /**
     * Default constructor for Serializer to use.
     */
    protected SwitchPort() {
        this.dpid = null;
        this.port = null;
    }

    /**
     * Constructor for a given DPID and a port.
     *
     * @param dpid the DPID to use.
     * @param port the port to use.
     */
    public SwitchPort(Dpid dpid, PortNumber port) {
        this.dpid = checkNotNull(dpid);
        this.port = checkNotNull(port);
    }

    /**
     * Constructor for the specified primitive values of a DPID and port.
     *
     * @param dpid the long DPID to use
     * @param port the short port number to use
     */
    public SwitchPort(long dpid, short port) {
        this.dpid = new Dpid(dpid);
        this.port = new PortNumber(port);
    }

    /**
     * Constructor for the specified primitive values of a DPID and port.
     *
     * @param dpid the DPID to use
     * @param port the port number to use
     */
    public SwitchPort(Long dpid, Long port) {
        this.dpid = new Dpid(dpid);
        this.port = new PortNumber(port.shortValue());
    }

    /**
     * Get the DPID value of the Switch-Port.
     *
     * @return the DPID value of the Switch-Port.
     */
    public Dpid getDpid() {
        return dpid;
    }

    /**
     * Get the port number of the Switch-Port.
     *
     * @return the port number of the Switch-Port.
     */
    public PortNumber getPortNumber() {
        return port;
    }

    /**
     * Convert the Switch-Port value to a string.
     * <p/>
     * The string has the following form:
     * 01:02:03:04:05:06:07:08/1234
     *
     * @return the Switch-Port value as a string.
     */
    @Override
    public String toString() {
        return this.dpid.toString() + "/" + this.port;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SwitchPort)) {
            return false;
        }

        SwitchPort otherSwitchPort = (SwitchPort) other;

        return (dpid.equals(otherSwitchPort.dpid) &&
                port.equals(otherSwitchPort.port));
    }

    @Override
    public int hashCode() {
        return Objects.hash(dpid, port);
    }
}
