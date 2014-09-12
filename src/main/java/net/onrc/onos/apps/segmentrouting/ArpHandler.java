/*******************************************************************************
 * Copyright (c) 2014 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package net.onrc.onos.apps.segmentrouting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.minlog.Log;

/**
 * Handling ARP requests to switches for Segment Routing.
 * <p/>
 * The module is for handling ARP requests to switches. It sends ARP response for any known
 * hosts to the controllers.
 * TODO: need to check the network config file for all hosts and packets
 */
public class ArpHandler implements IFloodlightModule, IOFMessageListener, IPacketListener  {

    private static final Logger log = LoggerFactory
            .getLogger(ArpHandler.class);

    private IFloodlightProviderService floodlightProvider;
    private IPacketService packetService;
    private IFlowPusherService flowPusher;
    private ITopologyService topologyService;
    private MutableTopology mutableTopology;
    private List<ArpEntry> arpEntries;

    private static final short IDLE_TIMEOUT = 0;
    private static final short HARD_TIMEOUT = 0;

    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPv4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_META = 4;
    private static final int TABLE_ACL = 5;

    private static final short MAX_PRIORITY = (short) 0xffff;
    private static final short SLASH_24_PRIORITY = (short) 0xfff0;
    private static final short SLASH_16_PRIORITY = (short) 0xff00;
    private static final short SLASH_8_PRIORITY = (short) 0xf000;
    private static final short MIN_PRIORITY = 0x0;


    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
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
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.packetService = context.getServiceImpl(IPacketService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);

        Log.debug("Arp Handler is initialized");

    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw,
            OFMessage msg, FloodlightContext cntx) {

        return Command.CONTINUE;
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {

        packetService.registerPacketListener(this);
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        mutableTopology = topologyService.getTopology();
        arpEntries = new ArrayList<ArpEntry>();
    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet payload) {
        log.debug("Received a packet {} from sw {} ", payload.toString(), sw.getDpid());

        if (payload.getEtherType() == Ethernet.TYPE_ARP) {

            ARP arp = (ARP)payload.getPayload();
            updateArpCache(arp);

            if (arp.getOpCode() == ARP.OP_REQUEST) {

                handleArpRequest(sw, inPort, arp);
            }

        }
        else if (payload.getEtherType() == Ethernet.TYPE_IPV4) {

            IPv4 ipv4 = (IPv4)payload.getPayload();
            if (ipv4.getProtocol() == IPv4.PROTOCOL_ICMP) {

                addRouteToHost(sw, ipv4);

            }

        }

    }

    /**
     * Add routing rules to forward packets to known hosts
     *
     * @param sw Switch
     * @param hostIp Host IP address to forwards packets to
     */
    private void addRouteToHost(Switch sw, IPv4 hostIp) {


        IOFSwitch ofSwitch = floodlightProvider.getMasterSwitch(sw.getDpid().value());
        OFFactory factory = ofSwitch.getFactory();
        int destinationAddress = hostIp.getDestinationAddress();
        // Check APR entries
        byte[] destinationMacAddress = getMacAddressFromIpAddress(destinationAddress);

        // Check TopologyService
        for (Host host: mutableTopology.getHosts()) {
            IPv4Address hostIpAddress = IPv4Address.of(host.getIpAddress());
            if (hostIpAddress != null && hostIpAddress.getInt() == destinationAddress) {
                destinationMacAddress = host.getMacAddress().toBytes();
            }
        }

        // If MAC address is not known to the host, just return
        if (destinationMacAddress == null)
            return;

        OFOxmEthType ethTypeIp = factory.oxms()
                .ethType(EthType.IPv4);
        OFOxmIpv4DstMasked ipPrefix = factory.oxms()
                .ipv4DstMasked(
                        IPv4Address.of(destinationAddress),
                        IPv4Address.NO_MASK); // host addr should be /32
        OFOxmList oxmListSlash32 = OFOxmList.of(ethTypeIp, ipPrefix);
        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(oxmListSlash32).build();
        OFAction setDmac = null;
        OFOxmEthDst dmac = factory.oxms()
                .ethDst(MacAddress.of(destinationMacAddress));
        setDmac = factory.actions().buildSetField()
                .setField(dmac).build();

        OFAction decTtl = factory.actions().decNwTtl();

        // Set the source MAC address with the switch MAC address
        String switchMacAddress = sw.getStringAttribute("routerMac");
        OFOxmEthSrc srcAddr = factory.oxms().ethSrc(MacAddress.of(switchMacAddress));
        OFAction setSA = factory.actions().buildSetField()
                .setField(srcAddr).build();

        List<OFAction> actionList = new ArrayList<OFAction>();
        actionList.add(setDmac);
        actionList.add(decTtl);
        actionList.add(setSA);


        /* TODO : need to check the config file for all packets
        String subnets = sw.getStringAttribute("subnets");
        try {
            JSONArray arry = new JSONArray(subnets);
            for (int i = 0; i < arry.length(); i++) {
                String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                int portNo = (int) arry.getJSONObject(i).get("portNo");

                if (netMatch(subnetIp, IPv4Address.of(hostIp.getDestinationAddress()).toString())) {
                    OFAction out = factory.actions().buildOutput()
                            .setPort(OFPort.of(portNo)).build();
                    actionList.add(out);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */

        // Set output port
        net.onrc.onos.core.topology.Host host = mutableTopology.getHostByMac(MACAddress.valueOf(destinationMacAddress));
        if (host != null) {
            for (Port port: host.getAttachmentPoints()) {
                OFAction out = factory.actions().buildOutput()
                                .setPort(OFPort.of(port.getPortNumber().shortValue())).build();
                actionList.add(out);
            }
        }

        OFInstruction writeInstr = factory.instructions().buildWriteActions()
                .setActions(actionList).build();

        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(writeInstr);

        OFMessage myIpEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_IPv4_UNICAST))
                .setMatch(match)
                .setInstructions(instructions)
                .setPriority(MAX_PRIORITY)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                //.setXid(getNextTransactionId())
                .build();

        log.debug("Sending 'Routing information' OF message to the switch {}.", sw.getDpid().toString());

        flowPusher.add(sw.getDpid(), myIpEntry);


    }

    /**
     * Send an ARP response for the ARP request to the known switches
     *
     * @param sw Switch
     * @param inPort port to send ARP response to
     * @param arpRequest ARP request packet to handle
     */
    private void handleArpRequest(Switch sw, Port inPort, ARP arpRequest) {

        String switchIpAddressSlash = sw.getStringAttribute("routerIp");
        String switchMacAddressStr = sw.getStringAttribute("routerMac");
        if (switchIpAddressSlash != null && switchMacAddressStr != null) {

            String switchIpAddressStr = switchIpAddressSlash.substring(0, switchIpAddressSlash.indexOf('/'));
            IPv4Address switchIpAddress = IPv4Address.of(switchIpAddressStr);
            IPv4Address targetProtocolAddress = IPv4Address.of(arpRequest.getTargetProtocolAddress());
            if (targetProtocolAddress.equals(switchIpAddress)) {
                MACAddress targetMac = MACAddress.valueOf(switchMacAddressStr);

                ARP arpReply = new ARP();
                arpReply.setHardwareType(ARP.HW_TYPE_ETHERNET)
                        .setProtocolType(ARP.PROTO_TYPE_IP)
                        .setHardwareAddressLength(
                                (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                        .setProtocolAddressLength((byte) IPv4.ADDRESS_LENGTH)
                        .setOpCode(ARP.OP_REPLY)
                        .setSenderHardwareAddress(targetMac.toBytes())
                        .setSenderProtocolAddress(arpRequest.getTargetProtocolAddress())
                        .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                        .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress());

                Ethernet eth = new Ethernet();
                eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                        .setSourceMACAddress(targetMac.toBytes())
                        .setEtherType(Ethernet.TYPE_ARP).setPayload(arpReply);

                packetService.sendPacket(eth, new SwitchPort(sw.getDpid(), inPort.getPortNumber()));
            }
        }
    }

    /**
     * Update ARP Cache using ARP packets
     * It is used to set destination MAC address to forward packets to known hosts.
     * But, it will be replace with Host information of Topology service later.
     *
     * @param arp APR packets to use for updating ARP entries
     */
    private void updateArpCache(ARP arp) {

        ArpEntry arpEntry = new ArpEntry(arp.getSenderHardwareAddress(), arp.getSenderProtocolAddress());
        // TODO: Need to check the duplication
        arpEntries.add(arpEntry);
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

    /**
     * Get MAC address to known hosts
     *
     * @param destinationAddress IP address to get MAC address
     * @return MAC Address to given IP address
     */
    private byte[] getMacAddressFromIpAddress(int destinationAddress) {

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
     * The function checks if given IP matches to the given subnet mask
     *
     * @param addr - subnet address to match
     * @param addr1 - IP address to check
     * @return true if the IP address matches to the subnet, otherwise false
     */

    public static boolean netMatch(String addr, String addr1){ //addr is subnet address and addr1 is ip address. Function will return true, if addr1 is within addr(subnet)

        String[] parts = addr.split("/");
        String ip = parts[0];
        int prefix;

        if (parts.length < 2) {
            prefix = 0;
        } else {
            prefix = Integer.parseInt(parts[1]);
        }

        Inet4Address a =null;
        Inet4Address a1 =null;
        try {
            a = (Inet4Address) InetAddress.getByName(ip);
            a1 = (Inet4Address) InetAddress.getByName(addr1);
        } catch (UnknownHostException e){}

        byte[] b = a.getAddress();
        int ipInt = ((b[0] & 0xFF) << 24) |
                         ((b[1] & 0xFF) << 16) |
                         ((b[2] & 0xFF) << 8)  |
                         ((b[3] & 0xFF) << 0);

        byte[] b1 = a1.getAddress();
        int ipInt1 = ((b1[0] & 0xFF) << 24) |
                         ((b1[1] & 0xFF) << 16) |
                         ((b1[2] & 0xFF) << 8)  |
                         ((b1[3] & 0xFF) << 0);

        int mask = ~((1 << (32 - prefix)) - 1);

        if ((ipInt & mask) == (ipInt1 & mask)) {
            return true;
        }
        else {
            return false;
        }
}




}
