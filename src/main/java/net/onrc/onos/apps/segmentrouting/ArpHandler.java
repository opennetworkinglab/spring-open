/*******************************************************************************
 * Copyright (c) 2014 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package net.onrc.onos.apps.segmentrouting;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.types.IPv4Address;
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
public class ArpHandler implements IPacketListener  {

    private static final Logger log = LoggerFactory
            .getLogger(ArpHandler.class);

    private IFloodlightProviderService floodlightProvider;
    private IPacketService packetService;
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
        this.packetService = context.getServiceImpl(IPacketService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.srManager = segmentRoutingManager;
        this.mutableTopology = topologyService.getTopology();

        packetService.registerPacketListener(this);
        //arpEntries = new ArrayList<ArpEntry>();

        Log.debug("Arp Handler is initialized");

    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet payload) {
        log.debug("Received a packet {} from sw {} ", payload.toString(), sw.getDpid());

        if (payload.getEtherType() == Ethernet.TYPE_ARP) {

            ARP arp = (ARP)payload.getPayload();
            srManager.updateArpCache(arp);

            if (arp.getOpCode() == ARP.OP_REQUEST) {

                handleArpRequest(sw, inPort, arp);
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






}
