/*******************************************************************************
 * Copyright (c) 2014 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;

import org.json.JSONArray;
import org.json.JSONException;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.minlog.Log;

/**
 * Handling ARP requests to switches for Segment Routing.
 * <p/>
 * The module is for handling ARP requests to switches. It sends ARP response
 * for any known hosts to the controllers. TODO: need to check the network
 * config file for all hosts and packets
 */
public class ArpHandler {

    private static final Logger log = LoggerFactory
            .getLogger(ArpHandler.class);

    private IFloodlightProviderService floodlightProvider;
    private IFlowPusherService flowPusher;
    private ITopologyService topologyService;
    private MutableTopology mutableTopology;
    // private List<ArpEntry> arpEntries;
    private SegmentRoutingManager srManager;

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

    /*
     * Default Constructor
     */
    public ArpHandler(FloodlightModuleContext context,
            SegmentRoutingManager segmentRoutingManager) {

        this.floodlightProvider = context
                .getServiceImpl(IFloodlightProviderService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.srManager = segmentRoutingManager;
        this.mutableTopology = topologyService.getTopology();

        Log.debug("Arp Handler is initialized");

    }

    /**
     * process ARP packets from switches It add a IP routing rule to the host If
     * it is an ARP response, then flush out all pending packets to the host
     * 
     * @param sw
     * @param inPort
     * @param payload
     */
    public void processPacketIn(Switch sw, Port inPort, Ethernet payload) {

        log.debug("ArpHandler: Received a ARP packet from sw {} ", sw.getDpid());

        ARP arp = (ARP) payload.getPayload();

        byte[] senderMacAddressByte = arp.getSenderHardwareAddress();
        IPv4Address hostIpAddress = IPv4Address.of(arp.getSenderProtocolAddress());
        log.debug("ArpHandler: Add IP route to Host {} ", hostIpAddress);
        srManager.addRouteToHost(sw, hostIpAddress.getInt(), senderMacAddressByte);

        if (arp.getOpCode() == ARP.OP_REQUEST) {
            log.debug("ArpHandler: Received a ARP Requestfrom sw {} ", sw.getDpid());
            handleArpRequest(sw, inPort, payload);
        }
        else {
            byte[] destIp = arp.getSenderProtocolAddress();
            for (IPv4 ipPacket : srManager.getIpPacketFromQueue(destIp)) {
                if (ipPacket != null && !inSameSubnet(sw, ipPacket)) {
                    Ethernet eth = new Ethernet();
                    eth.setDestinationMACAddress(payload.getSourceMACAddress());
                    eth.setSourceMACAddress(sw.getStringAttribute("routerMac"));
                    eth.setEtherType(Ethernet.TYPE_IPV4);
                    eth.setPayload(ipPacket);
                    sendPacketOut(sw, eth, inPort.getNumber().shortValue());
                }
            }
        }
    }

    /**
     * Send an ARP response for the ARP request to the known switches
     * 
     * @param sw Switch
     * @param inPort port to send ARP response to
     * @param arpRequest ARP request packet to handle
     */
    private void handleArpRequest(Switch sw, Port inPort, Ethernet payload) {

        ARP arpRequest = (ARP) payload.getPayload();
        MACAddress targetMac = null;

        if (isArpReqForSwitch(sw, arpRequest)) {
            String switchMacAddressStr = sw.getStringAttribute("routerMac");
            targetMac = MACAddress.valueOf(switchMacAddressStr);
            log.debug("ArpHandler: Received a ARP query for a sw {} ", sw.getDpid());
        }
        else {
            Host knownHost = isArpReqForKnownHost(sw, arpRequest);
            if (knownHost != null) {
                targetMac = knownHost.getMacAddress();
                log.debug("ArpHandler: Received a ARP query for a known host {} ",
                        IPv4Address.of(knownHost.getIpAddress()));
            }
        }

        if (targetMac != null) {
            /* ARP Destination is known. Packet out ARP Reply */
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

            sendPacketOut(sw, eth, inPort.getPortNumber().shortValue());
        }
        else
        {
            /* Broadcast the received ARP request to all switch ports
             * that subnets are connected to except the port from which
             * ARP request is received
             */
            IPv4Address targetAddress =
                    IPv4Address.of(arpRequest.getTargetProtocolAddress());
            log.debug("ArpHandler: Received a ARP query for unknown host {} ",
                    IPv4Address.of(arpRequest.getTargetProtocolAddress()));
            for (Integer portNo : getSwitchSubnetPorts(sw, targetAddress)) {
                if (portNo.shortValue() == inPort.getPortNumber().shortValue())
                    continue;
                log.debug("ArpHandler: Sending ARP request on switch {} port {}",
                        sw.getDpid(), portNo.shortValue());
                sendPacketOut(sw, payload, portNo.shortValue());
            }
        }
    }

    /**
     * Check if the ARP request is to known hosts
     * 
     * @param sw Switch
     * @param arpRequest ARP request to check
     */
    private Host isArpReqForKnownHost(Switch sw, ARP arpRequest) {
        Host knownHost = null;

        IPv4Address targetIPAddress = IPv4Address.of(
                arpRequest.getTargetProtocolAddress());

        for (Host host : sw.getHosts()) {
            if (host.getIpAddress() == targetIPAddress.getInt()) {
                knownHost = host;
                break;
            }
        }
        return knownHost;

    }

    /**
     * 
     * Check if the ARP is for the switch
     * 
     * @param sw Switch
     * @param arpRequest ARP request to check
     * @return true if the ARP is for the switch
     */
    private boolean isArpReqForSwitch(Switch sw, ARP arpRequest) {
        List<String> subnetGatewayIPs = getSubnetGatewayIps(sw);
        boolean isArpForSwitch = false;
        if (!subnetGatewayIPs.isEmpty()) {
            IPv4Address targetProtocolAddress = IPv4Address.of(arpRequest
                    .getTargetProtocolAddress());
            if (subnetGatewayIPs.contains(targetProtocolAddress.toString())) {
                isArpForSwitch = true;
            }
        }
        return isArpForSwitch;
    }

    /**
     * Retrieve Gateway IP address of all subnets defined in net config file
     * 
     * @param sw Switch to retrieve subnet GW IPs for
     * @return list of GW IP addresses for all subnets
     */
    private List<String> getSubnetGatewayIps(Switch sw) {

        List<String> gatewayIps = new ArrayList<String>();

        String subnets = sw.getStringAttribute("subnets");
        try {
            JSONArray arry = new JSONArray(subnets);
            for (int i = 0; i < arry.length(); i++) {
                String subnetIpSlash = (String) arry.getJSONObject(i).get("subnetIp");
                if (subnetIpSlash != null) {
                    String subnetIp = subnetIpSlash.substring(0,
                            subnetIpSlash.indexOf('/'));
                    gatewayIps.add(subnetIp);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return gatewayIps;
    }

    private HashSet<Integer> getSwitchSubnetPorts(Switch sw, IPv4Address targetAddress) {
        HashSet<Integer> switchSubnetPorts = new HashSet<Integer>();

        String subnets = sw.getStringAttribute("subnets");
        try {
            JSONArray arry = new JSONArray(subnets);
            for (int i = 0; i < arry.length(); i++) {
                String subnetIpSlash = (String) arry.getJSONObject(i).get("subnetIp");
                if (srManager.netMatch(subnetIpSlash, targetAddress.toString())) {
                    Integer subnetPort = (Integer) arry.getJSONObject(i).get("portNo");
                    switchSubnetPorts.add(subnetPort);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return switchSubnetPorts;
    }

    /**
     * Send an ARP request
     * 
     * @param sw Switch
     * @param targetAddress Target IP address
     * @param inPort Port to send the ARP request
     * 
     */
    public void sendArpRequest(Switch sw, int targetAddressInt, Port inPort) {

        IPv4Address targetAddress = IPv4Address.of(targetAddressInt);
        String senderMacAddressStr = sw.getStringAttribute("routerMac");
        String senderIpAddressSlash = sw.getStringAttribute("routerIp");
        if (senderMacAddressStr == null || senderIpAddressSlash == null)
            return;
        String senderIpAddressStr =
                senderIpAddressSlash.substring(0, senderIpAddressSlash.indexOf('/'));
        byte[] senderMacAddress = MacAddress.of(senderMacAddressStr).getBytes();
        byte[] senderIpAddress = IPv4Address.of(senderIpAddressStr).getBytes();

        ARP arpRequest = new ARP();
        arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) IPv4.ADDRESS_LENGTH)
                .setOpCode(ARP.OP_REQUEST)
                .setSenderHardwareAddress(senderMacAddress)
                .setTargetHardwareAddress(MacAddress.NONE.getBytes())
                .setSenderProtocolAddress(senderIpAddress)
                .setTargetProtocolAddress(targetAddress.getBytes());

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(MacAddress.BROADCAST.getBytes())
                .setSourceMACAddress(senderMacAddress)
                .setEtherType(Ethernet.TYPE_ARP).setPayload(arpRequest);

        /* Broadcast the ARP request to all switch ports
         * that subnets are connected to except the port from which
         * ARP request is received
         */
        for (Integer portNo : getSwitchSubnetPorts(sw, targetAddress)) {
            if (portNo.shortValue() == inPort.getPortNumber().shortValue())
                continue;
            log.debug("ArpHandler: Sending ARP request on switch {} port {}",
                    sw.getDpid(), portNo.shortValue());
            sendPacketOut(sw, eth, portNo.shortValue());
        }
    }

    /**
     * Send PACKET_OUT packet to switch
     * 
     * @param sw Switch to send the packet to
     * @param packet Packet to send
     * @param switchPort port to send (if -1, broadcast)
     */
    private void sendPacketOut(Switch sw, Ethernet packet, short port) {

        IOFSwitch ofSwitch = floodlightProvider.getMasterSwitch(sw.getDpid().value());
        OFFactory factory = ofSwitch.getFactory();

        List<OFAction> actions = new ArrayList<>();

        if (port > 0) {
            OFAction outport = factory.actions().output(OFPort.of(port), Short.MAX_VALUE);
            actions.add(outport);
        }
        else {
            Iterator<Port> iter = sw.getPorts().iterator();
            while (iter.hasNext()) {
                Port p = iter.next();
                int pnum = p.getPortNumber().shortValue();
                if (U32.of(pnum).compareTo(U32.of(OFPort.MAX.getPortNumber())) < 1) {
                    OFAction outport = factory.actions().output(
                            OFPort.of(p.getNumber().shortValue()),
                            Short.MAX_VALUE);
                    actions.add(outport);
                }
            }
        }

        OFPacketOut po = factory.buildPacketOut()
                .setData(packet.serialize())
                .setActions(actions)
                .build();

        flowPusher.add(sw.getDpid(), po);
    }

    /**
     * Check if the source IP and destination IP are in the same subnet
     * 
     * @param sw Switch
     * @param ipv4 IP address to check
     * @return return true if the IP packet is within the same subnet
     */
    private boolean inSameSubnet(Switch sw, IPv4 ipv4) {

        String gwIpSrc = getGwIpForSubnet(ipv4.getSourceAddress());
        String gwIpDest = getGwIpForSubnet(ipv4.getDestinationAddress());

        if (gwIpSrc.equals(gwIpDest)) {
            return true;
        }
        else
            return false;
    }

    /**
     * Get router IP address for the given IP address
     * 
     * @param sourceAddress
     * @return
     */
    private String getGwIpForSubnet(int sourceAddress) {

        String gwIp = null;
        IPv4Address srcIp = IPv4Address.of(sourceAddress);

        for (Switch sw : mutableTopology.getSwitches()) {

            String subnets = sw.getStringAttribute("subnets");
            try {
                JSONArray arry = new JSONArray(subnets);
                for (int i = 0; i < arry.length(); i++) {
                    String subnetIpSlash = (String) arry.getJSONObject(i).get("subnetIp");
                    if (srManager.netMatch(subnetIpSlash, srcIp.toString())) {
                        gwIp = subnetIpSlash;
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return gwIp;
    }

}
