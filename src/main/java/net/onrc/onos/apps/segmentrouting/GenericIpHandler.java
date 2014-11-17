package net.onrc.onos.apps.segmentrouting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.action.SetDAAction;
import net.onrc.onos.core.matchaction.action.SetSAAction;
import net.onrc.onos.core.matchaction.match.Ipv4Match;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.SwitchPort;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericIpHandler {

    private MutableTopology mutableTopology;
    private ITopologyService topologyService;
    private IFloodlightProviderService floodlightProvider;
    private IFlowPusherService flowPusher;
    private SegmentRoutingManager srManager;

    private static final Logger log = LoggerFactory
            .getLogger(GenericIpHandler.class);

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

    public GenericIpHandler(FloodlightModuleContext context, SegmentRoutingManager sr) {
        this.floodlightProvider = context
                .getServiceImpl(IFloodlightProviderService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.mutableTopology = topologyService.getTopology();
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
        this.srManager = sr;

    }

    public void processPacketIn(Switch sw, Port inPort, Ethernet payload) {
        // TODO Auto-generated method stub
        log.debug("GenericIPHandler: Received a IP packet {} from sw {} ",
                payload.toString(), sw.getDpid());
        IPv4 ipv4 = (IPv4) payload.getPayload();
        int destinationAddress = ipv4.getDestinationAddress();

        // Check if the destination is any host known to TopologyService
        for (net.onrc.onos.core.topology.Host host : mutableTopology.getHosts()) {
            IPv4Address hostIpAddress = IPv4Address.of(host.getIpAddress());
            if (hostIpAddress != null && hostIpAddress.getInt() == destinationAddress) {
                byte[] destinationMacAddress = host.getMacAddress().toBytes();
                addRouteToHost(sw, destinationAddress, destinationMacAddress);
                return;
            }
        }

        // Check if the destination is within subnets of the swtich
        if (isWithinSubnets(sw, IPv4Address.of(destinationAddress).toString())) {
            srManager.addPacketToPacketBuffer(ipv4);
            srManager.sendArpRequest(sw, destinationAddress, inPort);
        }
    }

    private boolean isWithinSubnets(Switch sw, String ipAddress) {

        return true;
    }



    /**
     */
    public void addRouteToHost(Switch sw, int destinationAddress,
            byte[] destinationMacAddress) {

        // If we do not know the host, then we cannot set the forwarding rule
        net.onrc.onos.core.topology.Host host = mutableTopology.getHostByMac(MACAddress
                .valueOf(destinationMacAddress));
        if ((host == null) || (host.getAttachmentPoints()==null)) {
            log.error("addRouteToHost: Invalid Host object");
            return;
        }

        IPv4Address destIpAddress = IPv4Address.of(destinationAddress);
        Ipv4Match ipMatch = new Ipv4Match(destIpAddress.toString()+"/32");
        List<Action> actions = new ArrayList<>();

        MacAddress destMacAddress = MacAddress.of(destinationMacAddress);
        SetDAAction setDAAction = new SetDAAction(destMacAddress);

        String routerMacAddress = sw.getStringAttribute("routerMac");
        MacAddress srcMacAddress = MacAddress.of(routerMacAddress);
        SetSAAction setSAAction = new SetSAAction(srcMacAddress);

        actions.add(setDAAction);
        actions.add(setSAAction);

        for (Port port : host.getAttachmentPoints()) {
            OutputAction outputAction = new OutputAction(port.getPortNumber());
            actions.add(outputAction);
        }

        MatchAction matchAction = new MatchAction(srManager.getMatchActionId(),
                new SwitchPort((long) 0, (short) 0), ipMatch, actions);

        net.onrc.onos.core.matchaction.MatchActionOperations.Operator operator =  Operator.ADD;

        MatchActionOperationEntry maEntry =
                new MatchActionOperationEntry(operator, matchAction);

        IOF13Switch sw13 = (IOF13Switch) floodlightProvider.getMasterSwitch(
                sw.getDpid().value());

        if (sw13 != null) {
            try {
                sw13.pushFlow(maEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Add routing rules to forward packets to known hosts
     *
     * @param sw Switch
     * @param hostIp Host IP address to forwards packets to
    public void addRouteToHost(Switch sw, int destinationAddress,
            byte[] destinationMacAddress) {

        // If we do not know the host, then we cannot set the forwarding rule
        net.onrc.onos.core.topology.Host host = mutableTopology.getHostByMac(MACAddress
                .valueOf(destinationMacAddress));
        if (host == null) {
            return;
        }

        IOFSwitch ofSwitch = floodlightProvider.getMasterSwitch(sw.getDpid().value());
        OFFactory factory = ofSwitch.getFactory();

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

        // Set output port
        for (Port port : host.getAttachmentPoints()) {
            OFAction out = factory.actions().buildOutput()
                    .setPort(OFPort.of(port.getPortNumber().shortValue())).build();
            actionList.add(out);
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
                // .setXid(getNextTransactionId())
                .build();

        log.debug("Sending 'Routing information' OF message to the switch {}.", sw
                .getDpid().toString());

        flowPusher.add(sw.getDpid(), myIpEntry);

    }
    */

}
