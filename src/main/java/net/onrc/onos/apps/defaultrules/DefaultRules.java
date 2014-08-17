package net.onrc.onos.apps.defaultrules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.util.Dpid;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pushing default flows for SDN IP.
 * <p/>
 * The module is a temporary solution to push default flows
 * upon switch connection. Specifically, when a switch connects
 * is pushed a default drop flow, a flow that allow ARP and a flow
 * that allow LLDP.
 * The module is not synchronized with other potential applications
 * sending different flows.
 * TODO: The module should talk to the Intent or MatchAction framework
 * to synchronize its behavior with the rest of the system.
 */
public class DefaultRules implements IFloodlightModule, IOFSwitchListener {

    private IFloodlightProviderService floodlightProvider;
    private IFlowPusherService flowPusher;
    private static final Logger log = LoggerFactory.getLogger(DefaultRules.class);
    private static final String APP_NAME = "DefaultRules";

    private static final short IDLE_TIMEOUT = 0;
    private static final short HARD_TIMEOUT = 0;
    private static final short DROP_RULE_PRIORITY = 1;
    private static final short DEFAULT_RULE_PRIORITY = 2;
    private static final MacAddress LLDP_MAC_ADDRESS =
            MacAddress.of("01:80:c2:00:00:0e");

    @Override
    public String getName() {
        return DefaultRules.APP_NAME;
    }

    @Override
    public void switchActivatedMaster(long swId) {
         // Install default drop rule sending a FlowMod
        this.writeDefaultDrop(swId);
         // Permit standard LLDP and ARP
        this.permitStdLLDP(swId);
        this.permitARP(swId);
    }

    /**
     * Installs a default FlowMod on the switch to DROP by default all packet-ins.
     *
     * @param swId The switch ID
     */
    private void writeDefaultDrop(long swId) {
        IOFSwitch sw = floodlightProvider.getMasterSwitch(swId);
        OFFactory factory = sw.getFactory();
        OFFlowMod.Builder builder = factory.buildFlowAdd();
        Match match = factory.buildMatch().build();
        List<OFAction> actionList = new ArrayList<OFAction>();
        builder.setMatch(match)
            .setActions(actionList)
            .setIdleTimeout(IDLE_TIMEOUT)
            .setHardTimeout(HARD_TIMEOUT)
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(DROP_RULE_PRIORITY);
        OFMessage ofMessage = builder.build();
        log.debug("Sending 'default drop' OF messsage to switch {}.", swId);
        flowPusher.add(new Dpid(swId), ofMessage);
    }

    /**
     * Installs a default FlowMod on the switch to allow LLDP traffic.
     * LLDP flows have an higher priority than the DROP ones.
     *
     * @param swId The switch ID
     */
    private void permitStdLLDP(long swId) {
        IOFSwitch sw = floodlightProvider.getMasterSwitch(swId);
        OFFactory factory = sw.getFactory();
        OFFlowMod.Builder builder = factory.buildFlowAdd();
        Match match = factory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.LLDP)
                .setExact(MatchField.ETH_DST, LLDP_MAC_ADDRESS)
                .build();
        List<OFAction> actionList = new ArrayList<OFAction>();
        OFAction action = factory.actions().output(OFPort.CONTROLLER, Short.MAX_VALUE);
        actionList.add(action);
        builder.setMatch(match)
            .setActions(actionList)
            .setIdleTimeout(IDLE_TIMEOUT)
            .setHardTimeout(HARD_TIMEOUT)
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(DEFAULT_RULE_PRIORITY);
        OFMessage ofMessage = builder.build();
        log.debug("Sending 'LLDP permit' OF message to switch {}.", swId);
        flowPusher.add(new Dpid(swId), ofMessage);
    }

    /**
     * The method installs a default FlowMod on the switch to allow ARP traffic.
     * ARP flows have an higher priority than the DROP ones
     *
     * @param swId The switch ID
     */
    private void permitARP(long swId) {
        IOFSwitch sw = floodlightProvider.getMasterSwitch(swId);
        OFFactory factory = sw.getFactory();
        OFFlowMod.Builder builder = factory.buildFlowAdd();
        Match match = factory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.ARP)
                .build();
        List<OFAction> actionList = new ArrayList<OFAction>();
        OFAction action = factory.actions().output(OFPort.CONTROLLER, Short.MAX_VALUE);
        actionList.add(action);
        builder.setMatch(match)
            .setActions(actionList)
            .setIdleTimeout(IDLE_TIMEOUT)
            .setHardTimeout(HARD_TIMEOUT)
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(DEFAULT_RULE_PRIORITY);
        OFMessage ofMessage = builder.build();
        log.debug("Sending 'ARP permit' OF message to the switch {}.", swId);
        flowPusher.add(new Dpid(swId), ofMessage);
    }

    @Override
    public void switchActivatedEqual(long swId) {
    }

    @Override
    public void switchMasterToEqual(long swId) {
    }

    @Override
    public void switchEqualToMaster(long swId) {
    }

    @Override
    public void switchDisconnected(long swId) {
    }

    @Override
    public void switchPortChanged(long swId, OFPortDesc port, PortChangeType changeType) {
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(IFlowPusherService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.flowPusher = context.getServiceImpl(IFlowPusherService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFSwitchListener(this);
    }
}
