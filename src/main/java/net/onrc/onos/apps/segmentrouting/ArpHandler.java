/*******************************************************************************
 * Copyright (c) 2014 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
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
 * The module is for handling ARP requests to switches. It sends ARP response for any known
 * hosts to the controllers.
 * TODO: need to check the network config file for all hosts and packets
 */
public class ArpHandler {

    private static final Logger log = LoggerFactory
            .getLogger(ArpHandler.class);

    private IFloodlightProviderService floodlightProvider;
    private IFlowPusherService flowPusher;
    private ITopologyService topologyService;
    private MutableTopology mutableTopology;
    //private List<ArpEntry> arpEntries;
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
    public ArpHandler(FloodlightModuleContext context, SegmentRoutingManager segmentRoutingManager) {

        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.srManager = segmentRoutingManager;
        this.mutableTopology = topologyService.getTopology();

        Log.debug("Arp Handler is initialized");

    }

    public void processPacketIn(Switch sw, Port inPort, Ethernet payload){

    	log.debug("ArpHandler: Received a ARP packet from sw {} ", sw.getDpid());

        ARP arp = (ARP)payload.getPayload();

        if (arp.getOpCode() == ARP.OP_REQUEST) {
        	log.debug("ArpHandler: Received a ARP Requestfrom sw {} ", sw.getDpid());
            handleArpRequest(sw, inPort, payload);
        }
        byte[] senderMacAddressByte = arp.getSenderHardwareAddress();
        IPv4Address hostIpAddress = IPv4Address.of(arp.getSenderProtocolAddress());
    	log.debug("ArpHandler: Add IP route to Host {} ", hostIpAddress);
        srManager.addRouteToHost(sw,hostIpAddress.getInt(), senderMacAddressByte);
    }

    /**
     * Send an ARP response for the ARP request to the known switches
     *
     * @param sw Switch
     * @param inPort port to send ARP response to
     * @param arpRequest ARP request packet to handle
     */
    private void handleArpRequest(Switch sw, Port inPort, Ethernet payload) {

    	ARP arpRequest = (ARP)payload.getPayload();
        List<String> subnetGatewayIPs = getSubnetGatewayIps(sw);
        String switchMacAddressStr = sw.getStringAttribute("routerMac");
        if (!subnetGatewayIPs.isEmpty()) {
            IPv4Address targetProtocolAddress = IPv4Address.of(arpRequest.getTargetProtocolAddress());
            // Do we have to check port also ??
            if (subnetGatewayIPs.contains(targetProtocolAddress.toString())) {
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

                sendPacketOut(sw, eth, inPort.getPortNumber().shortValue());
            }
        }
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
                    String subnetIp = subnetIpSlash.substring(0, subnetIpSlash.indexOf('/'));
                    gatewayIps.add(subnetIp);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return gatewayIps;
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

        sendPacketOut(sw, eth, (short)-1);

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
                    OFAction outport = factory.actions().output(OFPort.of(p.getNumber().shortValue()),
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

}

