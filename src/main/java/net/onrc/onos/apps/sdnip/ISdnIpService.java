package net.onrc.onos.apps.sdnip;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * The API exported by the main SDN-IP class. This is the interface between the
 * REST handlers and the SDN-IP module.
 */
public interface ISdnIpService extends IFloodlightService {

    /**
     * Gets a reference to SDN-IP's PATRICIA tree which stores the route table.
     *
     * XXX This is a poor API because it exposes internal state of SDN-IP.
     *
     * @return the PATRICIA tree.
     */
    public IPatriciaTree<RibEntry> getPtree();

    /**
     * Gets the IP address of REST server on the BGPd side. This is used to
     * communicate with BGPd.
     *
     * @return the IP address as a String
     */
    public String getBgpdRestIp();

    /**
     * Gets the router ID, which is sent to BGPd to identify the route table
     * we're interested in.
     *
     * @return the router ID as a String
     */
    public String getRouterId();

    /**
     * Clears SDN-IP's route table.
     */
    public void clearPtree();

    /**
     * Pass a RIB update to the {@link ISdnIpService}.
     *
     * @param update a {@link RibUpdate} object containing details of the
     * update
     */
    public void newRibUpdate(RibUpdate update);

    /**
     * Start SDN-IP Routing.
     */
    public void beginRoutingNew();
}
