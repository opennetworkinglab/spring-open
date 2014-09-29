
/*******************************************************************************
 * Copyright (c) 2014 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.ICMP;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Host;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.SwitchPort;

import org.json.JSONArray;
import org.json.JSONException;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmInPort;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabel;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpHandler {

    private SegmentRoutingManager srManager;
    private IFloodlightProviderService floodlightProvider;
    private MutableTopology mutableTopology;
    private ITopologyService topologyService;
    private static final Logger log = LoggerFactory
            .getLogger(IcmpHandler.class);

    private IFlowPusherService flowPusher;
    private boolean controllerPortAllowed = false;

    private Queue<IPv4> icmpQueue = new LinkedList<IPv4>();

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

    private static final int ICMP_TYPE_ECHO = 0x08;
    private static final int ICMP_TYPE_REPLY = 0x00;


    public IcmpHandler(FloodlightModuleContext context, SegmentRoutingManager manager) {

        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.mutableTopology = topologyService.getTopology();

        this.srManager = manager;
    }

    /**
     * handle ICMP packets
     * If it is for ICMP echo to router IP or any subnet GW IP,
     * then send ICMP response on behalf of the switch.
     * If it is for any hosts in subnets of the switches, but if the MAC
     * address is not known, then send an ARP request to the subent.
     * If the MAC address is known, then set the routing rule to the switch
     *
     * @param sw
     * @param inPort
     * @param payload
     */
    public void processPacketIn(Switch sw, Port inPort, Ethernet payload) {

        if (payload.getEtherType() == Ethernet.TYPE_IPV4) {

            IPv4 ipv4 = (IPv4)payload.getPayload();

            if (ipv4.getProtocol() == IPv4.PROTOCOL_ICMP) {

                log.debug("ICMPHandler: Received a ICMP packet {} from sw {} ",
                        payload.toString(), sw.getDpid());
                IPv4Address destinationAddress =
                        IPv4Address.of(ipv4.getDestinationAddress());

                // Check if it is ICMP request to the switch
                String switchIpAddressSlash = sw.getStringAttribute("routerIp");
                if (switchIpAddressSlash != null) {
                    String switchIpAddressStr
                        = switchIpAddressSlash.substring(0, switchIpAddressSlash.indexOf('/'));
                    IPv4Address switchIpAddress = IPv4Address.of(switchIpAddressStr);
                    List<String> gatewayIps = getSubnetGatewayIps(sw);
                    if (((ICMP)ipv4.getPayload()).getIcmpType() == ICMP_TYPE_ECHO &&
                            (destinationAddress.getInt() == switchIpAddress.getInt() ||
                             gatewayIps.contains(destinationAddress.toString()))) {
                        log.debug("ICMPHandler: ICMP packet for sw {} and "
                                + "sending ICMP response ", sw.getDpid());
                        sendICMPResponse(sw, inPort, payload);
                        srManager.getIpPacketFromQueue(destinationAddress.getBytes());
                        return;
                    }
                }

                /* Check if ICMP is for any switch known host */
                for (Host host: sw.getHosts()) {
                    IPv4Address hostIpAddress =
                            IPv4Address.of(host.getIpAddress());
                    if (hostIpAddress != null &&
                            hostIpAddress.equals(destinationAddress)) {
                        /* TODO: We should not have come here as ARP itself
                         * would have installed a Route to the host. See if
                         * we can remove this code
                         */
                        log.debug("ICMPHandler: ICMP request for known host {}",
                                         hostIpAddress);
                        byte[] destinationMacAddress = host.getMacAddress().toBytes();
                        srManager.addRouteToHost(sw,
                                destinationAddress.getInt(), destinationMacAddress);
                        return;
                    }
                }
                /* ICMP for an unknown host */
                log.debug("ICMPHandler: ICMP request for unknown host {}"
                        + " and sending ARP request", destinationAddress);
                srManager.sendArpRequest(sw, destinationAddress.getInt(), inPort);
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
     * Send ICMP reply back
     *
     * @param sw Switch
     * @param inPort Port the ICMP packet is forwarded from
     * @param icmpRequest the ICMP request to handle
     * @param destinationAddress destination address to send ICMP response to
     */
    private void sendICMPResponse(Switch sw, Port inPort, Ethernet icmpRequest) {

        Ethernet icmpReplyEth = new Ethernet();

        IPv4 icmpRequestIpv4 = (IPv4) icmpRequest.getPayload();
        IPv4 icmpReplyIpv4 = new IPv4();
        int destAddress = icmpRequestIpv4.getDestinationAddress();
        icmpReplyIpv4.setDestinationAddress(icmpRequestIpv4.getSourceAddress());
        icmpReplyIpv4.setSourceAddress(destAddress);
        icmpReplyIpv4.setTtl((byte)64);
        icmpReplyIpv4.setChecksum((short)0);


        ICMP icmpReply = (ICMP)icmpRequestIpv4.getPayload().clone();
        icmpReply.setIcmpCode((byte)0x00);
        icmpReply.setIcmpType((byte) ICMP_TYPE_REPLY);
        icmpReply.setChecksum((short)0);

        icmpReplyIpv4.setPayload(icmpReply);

        icmpReplyEth.setPayload(icmpReplyIpv4);
        icmpReplyEth.setEtherType(Ethernet.TYPE_IPV4);
        icmpReplyEth.setDestinationMACAddress(icmpRequest.getSourceMACAddress());
        icmpReplyEth.setSourceMACAddress(icmpRequest.getDestinationMACAddress());

        sendPacketOut(sw, icmpReplyEth, new SwitchPort(sw.getDpid(), inPort.getPortNumber()), false);

        log.debug("Send an ICMP response {}", icmpReplyIpv4.toString());

    }

    /**
     * Send PACKET_OUT message with actions
     * If switches support OFPP_TABLE action, it sends out packet to TABLE port
     * Otherwise, it sends the packet to the port the packet came from
     * (in this case, MPLS label is added if the packet needs go through transit switches)
     *
     * @param sw  Switch the packet came from
     * @param packet Ethernet packet to send
     * @param switchPort port to send the packet
     */
    private void sendPacketOut(Switch sw, Ethernet packet, SwitchPort switchPort, boolean supportOfppTable) {

        boolean sameSubnet = false;
        IOFSwitch ofSwitch = floodlightProvider.getMasterSwitch(sw.getDpid().value());
        OFFactory factory = ofSwitch.getFactory();

        List<OFAction> actions = new ArrayList<>();

        // If OFPP_TABLE action is not supported in the switch, MPLS label needs to be set
        // if the packet needs to be delivered crossing switches
        if (!supportOfppTable) {
            // Check if the destination is the host attached to the switch
            int destinationAddress = ((IPv4)packet.getPayload()).getDestinationAddress();
            for (net.onrc.onos.core.topology.Host host: mutableTopology.getHosts(switchPort)) {
                IPv4Address hostIpAddress = IPv4Address.of(host.getIpAddress());
                if (hostIpAddress != null && hostIpAddress.getInt() == destinationAddress) {
                    sameSubnet = true;
                    break;
                }
            }

            IPv4Address targetAddress = IPv4Address.of(((IPv4)packet.getPayload()).getDestinationAddress());
            String destMacAddress = packet.getDestinationMAC().toString();
            // If the destination host is not attached in the switch
            // and the destination is not the neighbor switch, then add MPLS label
            String targetMac = getRouterMACFromConfig(targetAddress);
            if (!sameSubnet && !targetMac.equals(destMacAddress)) {
                int mplsLabel = getMplsLabelFromConfig(targetAddress);
                if (mplsLabel > 0) {
                    OFAction pushlabel = factory.actions().pushMpls(EthType.MPLS_UNICAST);
                    OFOxmMplsLabel l = factory.oxms()
                            .mplsLabel(U32.of(mplsLabel));
                    OFAction setlabelid = factory.actions().buildSetField()
                            .setField(l).build();
                    OFAction copyTtlOut = factory.actions().copyTtlOut();
                    actions.add(pushlabel);
                    actions.add(setlabelid);
                    actions.add(copyTtlOut);
                }
            }

            OFAction outport = factory.actions().output(OFPort.of(switchPort.getPortNumber().shortValue()), Short.MAX_VALUE);
            actions.add(outport);
        }
        // If OFPP_TABLE action is supported, first set a rule to allow packet from CONTROLLER port.
        // Then, send the packet to the table port
        else {
            if (!controllerPortAllowed) {
                addControlPortInVlanTable(sw);
                controllerPortAllowed = true;
            }
            OFAction outport = factory.actions().output(OFPort.TABLE, Short.MAX_VALUE);
            actions.add(outport);
        }

        OFPacketOut po = factory.buildPacketOut()
                .setData(packet.serialize())
                .setActions(actions)
                .build();

        flowPusher.add(sw.getDpid(), po);
    }

    /**
     * Get MPLS label for the target address from the network config file
     *
     * @param targetAddress - IP address of the target host
     * @return MPLS label of the switch to send packets to the target address
     */
    private int getMplsLabelFromConfig(IPv4Address targetAddress) {

        int mplsLabel = -1;

        for (Switch sw: mutableTopology.getSwitches()) {

            String subnets = sw.getStringAttribute("subnets");
            try {
                JSONArray arry = new JSONArray(subnets);
                for (int i = 0; i < arry.length(); i++) {
                    String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                    if (srManager.netMatch(subnetIp, targetAddress.toString())) {
                        String mplsLabelStr = sw.getStringAttribute("nodeSid");
                        if (mplsLabelStr != null)
                            mplsLabel = Integer.parseInt(mplsLabelStr);
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return mplsLabel;
    }


    /**
     * Get Router MAC Address for the target address from the network config file
     *
     * @param targetAddress - IP address of the target host
     * @return Router MAC of the switch to send packets to the target address
     */
    private String getRouterMACFromConfig(IPv4Address targetAddress) {

        String routerMac = null;

        for (Switch sw: mutableTopology.getSwitches()) {

            String subnets = sw.getStringAttribute("subnets");
            try {
                JSONArray arry = new JSONArray(subnets);
                for (int i = 0; i < arry.length(); i++) {
                    String subnetIp = (String) arry.getJSONObject(i).get("subnetIp");
                    if (srManager.netMatch(subnetIp, targetAddress.toString())) {
                             routerMac = sw.getStringAttribute("routerMac");
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return routerMac;
    }

    /**
     * Add a new rule to VLAN table to forward packets from any port to the next table
     * It is required to forward packets from controller to pipeline
     *
     * @param sw  Switch the packet came from
     */
    private void addControlPortInVlanTable(Switch sw) {

        IOFSwitch ofSwitch = floodlightProvider.getMasterSwitch(sw.getDpid().value());
        OFFactory factory = ofSwitch.getFactory();

        OFOxmInPort oxp = factory.oxms().inPort(OFPort.CONTROLLER);
        OFOxmVlanVid oxv = factory.oxms()
                .vlanVid(OFVlanVidMatch.UNTAGGED);
        OFOxmList oxmList = OFOxmList.of(oxv);

        /* Cqpd switch does not seems to support CONTROLLER port as in_port match rule */
        //OFOxmList oxmList = OFOxmList.of(oxp, oxv);

        OFMatchV3 match = factory.buildMatchV3()
                .setOxmList(oxmList)
                .build();

        OFInstruction gotoTbl = factory.instructions().buildGotoTable()
                .setTableId(TableId.of(TABLE_TMAC)).build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        instructions.add(gotoTbl);
        OFMessage flowEntry = factory.buildFlowAdd()
                .setTableId(TableId.of(TABLE_VLAN))
                .setMatch(match)
                .setInstructions(instructions)
                .setPriority(1000) // does not matter - all rules
                                   // exclusive
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                //.setXid(getNextTransactionId())
                .build();

        flowPusher.add(sw.getDpid(), flowEntry);;
        log.debug("Adding a new vlan-rules in sw {}", sw.getDpid());

    }

}
