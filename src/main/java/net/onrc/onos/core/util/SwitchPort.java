package net.onrc.onos.core.util;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The class representing a Switch-Port.
 * This class is immutable.
 */
public final class SwitchPort {
    private final Dpid dpid;        // The DPID of the switch
    private final PortNumber port;        // The port of the switch

    /**
     * Default constructor.
     */
    public SwitchPort() {
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
        this.dpid = dpid;
        this.port = port;
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
     * Get the DPID value of the Switch-Port.
     *
     * @return the DPID value of the Switch-Port.
     */
    @JsonProperty("dpid")
    public Dpid dpid() {
        return dpid;
    }

    /**
     * Get the port value of the Switch-Port.
     *
     * @return the port value of the Switch-Port.
     */
    @JsonProperty("port")
    public PortNumber port() {
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
        int hash = 17;
        hash += 31 * hash + dpid.hashCode();
        hash += 31 * hash + port.hashCode();
        return hash;
    }
}
