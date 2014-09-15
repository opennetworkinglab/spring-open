package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.topology.ITopologyService;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingManager implements IFloodlightModule {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

    private List<ArpEntry> arpEntries;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

        l.add(IFloodlightProviderService.class);
        l.add(IConfigInfoService.class);
        l.add(ITopologyService.class);
        l.add(IPacketService.class);
        l.add(IFlowPusherService.class);
        l.add(ITopologyService.class);

        return l;

    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {

        ArpHandler aprHandler = new ArpHandler(context, this);
        IcmpHandler icmpHandler = new IcmpHandler(context, this);
        arpEntries = new ArrayList<ArpEntry>();

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ARP Cache using ARP packets
     * It is used to set destination MAC address to forward packets to known hosts.
     * But, it will be replace with Host information of Topology service later.
     *
     * @param arp APR packets to use for updating ARP entries
     */
    public void updateArpCache(ARP arp) {

        ArpEntry arpEntry = new ArpEntry(arp.getSenderHardwareAddress(), arp.getSenderProtocolAddress());
        // TODO: Need to check the duplication
        arpEntries.add(arpEntry);
    }

    /**
     * Get MAC address to known hosts
     *
     * @param destinationAddress IP address to get MAC address
     * @return MAC Address to given IP address
     */
    public byte[] getMacAddressFromIpAddress(int destinationAddress) {

        // Can't we get the host IP address from the TopologyService ??

        Iterator<ArpEntry> iterator = arpEntries.iterator();

        IPv4Address ipAddress = IPv4Address.of(destinationAddress);
        byte[] ipAddressInByte = ipAddress.getBytes();

        while (iterator.hasNext() ) {
            ArpEntry arpEntry = iterator.next();
            byte[] address = arpEntry.targetIpAddress;

            IPv4Address a = IPv4Address.of(address);
            IPv4Address b = IPv4Address.of(ipAddressInByte);

            if ( a.equals(b)) {
                log.debug("Found an arp entry");
                return arpEntry.targetMacAddress;
            }
        }

        return null;
    }


    /**
     * Temporary class to to keep ARP entry
     *
     */
    private class ArpEntry {

        byte[] targetMacAddress;
        byte[] targetIpAddress;

        private ArpEntry(byte[] macAddress, byte[] ipAddress) {
            this.targetMacAddress = macAddress;
            this.targetIpAddress = ipAddress;
        }

    }

}
