package net.onrc.onos.apps.bgproute;

import java.util.Collections;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;

import org.codehaus.jackson.annotate.JsonProperty;
import org.openflow.util.HexString;

/**
 * Contains the configuration data for SDN-IP that has been read from a
 * JSON-formatted configuration file.
 */
public class Configuration {
    private long bgpdAttachmentDpid;
    private short bgpdAttachmentPort;
    private MACAddress bgpdMacAddress;
    private short vlan;
    private List<String> switches;
    private List<Interface> interfaces;
    private List<BgpPeer> peers;

    /**
     * Default constructor.
     */
    public Configuration() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets the switch that BGPd is attached to.
     *
     * @return the dpid of BGPd's attachment point
     */
    public long getBgpdAttachmentDpid() {
        return bgpdAttachmentDpid;
    }

    /**
     * Sets the switch that BGPd is attached to.
     *
     * @param bgpdAttachmentDpid the dpid of BGPd's attachment point
     */
    @JsonProperty("bgpdAttachmentDpid")
    public void setBgpdAttachmentDpid(String bgpdAttachmentDpid) {
        this.bgpdAttachmentDpid = HexString.toLong(bgpdAttachmentDpid);
    }

    /**
     * Gets the port that BGPd is attached to.
     *
     * @return the port number on the switch where BGPd is attached
     */
    public short getBgpdAttachmentPort() {
        return bgpdAttachmentPort;
    }

    /**
     * Sets the port that BGPd is attached to.
     *
     * @param bgpdAttachmentPort the port number on the switch where BGPd is
     * attached
     */
    @JsonProperty("bgpdAttachmentPort")
    public void setBgpdAttachmentPort(short bgpdAttachmentPort) {
        this.bgpdAttachmentPort = bgpdAttachmentPort;
    }

    /**
     * Gets the MAC address of the BGPd host interface.
     *
     * @return the MAC address
     */
    public MACAddress getBgpdMacAddress() {
        return bgpdMacAddress;
    }

    /**
     * Sets the MAC address of the BGPd host interface.
     *
     * @param strMacAddress the MAC address
     */
    @JsonProperty("bgpdMacAddress")
    public void setBgpdMacAddress(String strMacAddress) {
        this.bgpdMacAddress = MACAddress.valueOf(strMacAddress);
    }

    /**
     * Gets a list of the DPIDs of all switches in the system. The DPIDs are
     * in String format represented in hexadecimal.
     *
     * @return the list of DPIDs
     */
    public List<String> getSwitches() {
        return Collections.unmodifiableList(switches);
    }

    /**
     * Sets a list of DPIDs of all switches in the system.
     *
     * @param switches the list of DPIDs
     */
    @JsonProperty("switches")
    public void setSwitches(List<String> switches) {
        this.switches = switches;
    }

    /**
     * Gets the VLAN number of the VLAN the system is running in, if any.
     * 0 means we are not running in a VLAN.
     *
     * @return the VLAN number if we are running in a VLAN, otherwise 0
     */
    public short getVlan() {
        return vlan;
    }

    /**
     * Sets the VLAN number of the VLAN the system is running in.
     *
     * @param vlan the VLAN number
     */
    @JsonProperty("vlan")
    public void setVlan(short vlan) {
        this.vlan = vlan;
    }

    /**
     * Gets a list of interfaces in the system, represented by
     * {@link Interface} objects.
     *
     * @return the list of interfaces
     */
    public List<Interface> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     * Sets a list of interfaces in the system.
     *
     * @param interfaces the list of interfaces
     */
    @JsonProperty("interfaces")
    public void setInterfaces(List<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    /**
     * Gets a list of BGP peers we are configured to peer with. Peers are
     * represented by {@link BgpPeer} objects.
     *
     * @return the list of BGP peers
     */
    public List<BgpPeer> getPeers() {
        return Collections.unmodifiableList(peers);
    }

    /**
     * Sets a list of BGP peers we are configured to peer with.
     *
     * @param peers the list of BGP peers
     */
    @JsonProperty("bgpPeers")
    public void setPeers(List<BgpPeer> peers) {
        this.peers = peers;
    }

}
