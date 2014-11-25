/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.core.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchFilter;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.annotations.LogMessageDocs;
import net.floodlightcontroller.core.internal.OFChannelHandler.RoleRecvStatus;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.util.ListenerDispatcher;
import net.floodlightcontroller.core.web.CoreWebRoutable;
import net.floodlightcontroller.debugcounter.IDebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterException;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterType;
import net.floodlightcontroller.debugevent.IDebugEventService;
import net.floodlightcontroller.debugevent.IDebugEventService.EventColumn;
import net.floodlightcontroller.debugevent.IDebugEventService.EventFieldType;
import net.floodlightcontroller.debugevent.IDebugEventService.EventType;
import net.floodlightcontroller.debugevent.IDebugEventService.MaxEventsRegistered;
import net.floodlightcontroller.debugevent.IEventUpdater;
import net.floodlightcontroller.debugevent.NullDebugEvent;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.LoadMonitor;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.drivermanager.DriverManager;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.registry.IControllerRegistryService.ControlChangeCallback;
import net.onrc.onos.core.registry.RegistryException;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller class. Handles all setup and network listeners -
 * Distributed ownership control of switch through IControllerRegistryService
 */
public class Controller implements IFloodlightProviderService {

    protected final static Logger log = LoggerFactory.getLogger(Controller.class);
    static final String ERROR_DATABASE =
            "The controller could not communicate with the system database.";
    protected static OFFactory factory13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);

    // connectedSwitches cache contains all connected switch's channelHandlers
    // including ones where this controller is a master/equal/slave controller
    // as well as ones that have not been activated yet
    protected ConcurrentHashMap<Long, OFChannelHandler> connectedSwitches;
    // These caches contains only those switches that are active
    protected ConcurrentHashMap<Long, IOFSwitch> activeMasterSwitches;
    protected ConcurrentHashMap<Long, IOFSwitch> activeSlaveSwitches;
    // lock to synchronize on, when manipulating multiple caches above
    private Object multiCacheLock;

    // The controllerNodeIPsCache maps Controller IDs to their IP address.
    // It's only used by handleControllerNodeIPsChanged
    protected HashMap<String, String> controllerNodeIPsCache;
    protected BlockingQueue<IUpdate> updates;

    protected ConcurrentMap<OFType, ListenerDispatcher<OFType, IOFMessageListener>> messageListeners;
    protected Set<IOFSwitchListener> switchListeners;

    // Module dependencies
    protected IRestApiService restApi;
    protected IThreadPoolService threadPool;
    protected IControllerRegistryService registryService;
    protected IDebugCounterService debugCounters;
    protected IDebugEventService debugEvents;

    protected ILinkDiscoveryService linkDiscovery;
    protected INetworkConfigService networkConfig;

    // Configuration options
    protected int openFlowPort = 6633;
    protected int workerThreads = 0;

    // The id for this controller node. Should be unique for each controller
    // node in a controller cluster.
    private OnosInstanceId onosInstanceId = new OnosInstanceId("localhost");

    // defined counters
    private Counters counters;
    // Event IDs for debug events
    protected IEventUpdater<SwitchEvent> evSwitch;

    // Load monitor for overload protection
    protected final boolean overload_drop =
            Boolean.parseBoolean(System.getProperty("overload_drop", "false"));
    protected final LoadMonitor loadmonitor = new LoadMonitor(log);

    // Start time of the controller
    protected long systemStartTime;

    // Flag to always flush flow table on switch reconnect (HA or otherwise)
    protected boolean alwaysClearFlowsOnSwAdd = false;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;
    protected static final int BATCH_MAX_SIZE = 100;
    protected static final boolean ALWAYS_DECODE_ETH = true;

    // ******************************
    // Switch Management and Updates
    // ******************************

    /**
     * Switch updates are sent to all IOFSwitchListeners. A switch that is
     * connected to this controller instance, but not activated, is not
     * available for updates.
     *
     * In ONOS, each controller instance can simultaneously serve in a MASTER
     * role for some connected switches, and in a SLAVE role for other connected
     * switches. It does not support role EQUAL. Activated switches, either with
     * Controller Role MASTER or SLAVE are announced as updates. We also support
     * announcements of controller role transitions from MASTER --> SLAVE, and
     * SLAVE --> MASTER, for an individual switch.
     *
     * Disconnection of only activated switches are announced. Finally, changes
     * to switch ports are announced with a portChangeType (see @IOFSwitch)
     *
     * @author saurav
     */
    public enum SwitchUpdateType {
        /** switch activated with this controller's role as MASTER */
        ACTIVATED_MASTER,
        /**
         * switch activated with this controller's role as SLAVE. listener
         * cannot send packets or commands to the switch
         */
        ACTIVATED_SLAVE,
        /** this controller's role for this switch changed from Master to Slave */
        MASTER_TO_SLAVE,
        /** this controller's role for this switch changed form Slave to Master */
        SLAVE_TO_MASTER,
        /** A previously activated switch disconnected */
        DISCONNECTED,
        /** Port changed on a previously activated switch */
        PORTCHANGED,
    }

    /**
     * Update message indicating a switch was added or removed ONOS: This
     * message extended to indicate Port add or removed event.
     */
    protected class SwitchUpdate implements IUpdate {
        public long getSwId() {
            return swId;
        }

        public SwitchUpdateType getSwitchUpdateType() {
            return switchUpdateType;
        }

        public PortChangeType getPortChangeType() {
            return changeType;
        }

        private final long swId;
        private final SwitchUpdateType switchUpdateType;
        private final OFPortDesc port;
        private final PortChangeType changeType;

        public SwitchUpdate(long swId, SwitchUpdateType switchUpdateType) {
            this(swId, switchUpdateType, null, null);
        }

        public SwitchUpdate(long swId,
                SwitchUpdateType switchUpdateType,
                OFPortDesc port,
                PortChangeType changeType) {
            if (switchUpdateType == SwitchUpdateType.PORTCHANGED) {
                if (port == null) {
                    throw new NullPointerException("Port must not be null " +
                            "for PORTCHANGED updates");
                }
                if (changeType == null) {
                    throw new NullPointerException("ChangeType must not be " +
                            "null for PORTCHANGED updates");
                }
            } else {
                if (port != null || changeType != null) {
                    throw new IllegalArgumentException("port and changeType " +
                            "must be null for " + switchUpdateType +
                            " updates");
                }
            }
            this.swId = swId;
            this.switchUpdateType = switchUpdateType;
            this.port = port;
            this.changeType = changeType;
        }

        @Override
        public void dispatch() {
            if (log.isTraceEnabled()) {
                log.trace("Dispatching switch update {} {}",
                        HexString.toHexString(swId), switchUpdateType);
            }
            if (switchListeners != null) {
                for (IOFSwitchListener listener : switchListeners) {
                    switch (switchUpdateType) {
                    case ACTIVATED_MASTER:
                        // don't count here. We have more specific
                        // counters before the update is created
                        listener.switchActivatedMaster(swId);
                        break;
                    case ACTIVATED_SLAVE:
                        // don't count here. We have more specific
                        // counters before the update is created
                        listener.switchActivatedSlave(swId);
                        break;
                    case MASTER_TO_SLAVE:
                        listener.switchMasterToSlave(swId);
                        break;
                    case SLAVE_TO_MASTER:
                        listener.switchSlaveToMaster(swId);
                        break;
                    case DISCONNECTED:
                        // don't count here. We have more specific
                        // counters before the update is created
                        listener.switchDisconnected(swId);
                        break;
                    case PORTCHANGED:
                        counters.switchPortChanged.updateCounterWithFlush();
                        listener.switchPortChanged(swId, port, changeType);
                        break;
                    }
                }
            }
        }

    }

    protected boolean addConnectedSwitch(long dpid, OFChannelHandler h) {
        if (connectedSwitches.get(dpid) != null) {
            log.error("Trying to add connectedSwitch but found a previous "
                    + "value for dpid: {}", dpid);
            return false;
        } else {
            connectedSwitches.put(dpid, h);
            return true;
        }
    }

    /**
     * Switch Events
     */
    @Override
    public void addSwitchEvent(long dpid, String reason, boolean flushNow) {
        if (flushNow)
            evSwitch.updateEventWithFlush(new SwitchEvent(dpid, reason));
        else
            evSwitch.updateEventNoFlush(new SwitchEvent(dpid, reason));
    }

    private boolean validActivation(long dpid) {
        if (connectedSwitches.get(dpid) == null) {
            log.error("Trying to activate switch but is not in "
                    + "connected switches: dpid {}. Aborting ..",
                    HexString.toHexString(dpid));
            return false;
        }
        if (activeMasterSwitches.get(dpid) != null ||
                activeSlaveSwitches.get(dpid) != null) {
            log.error("Trying to activate switch but it is already "
                    + "activated: dpid {}. Found in activeMaster: {} "
                    + "Found in activeEqual: {}. Aborting ..", new Object[] {
                    HexString.toHexString(dpid),
                    (activeMasterSwitches.get(dpid) == null) ? 'Y' : 'N',
                    (activeSlaveSwitches.get(dpid) == null) ? 'Y' : 'N'});
            counters.switchWithSameDpidActivated.updateCounterWithFlush();
            return false;
        }
        return true;
    }

    /**
     * Called when a switch is activated, with this controller's role as MASTER
     */
    protected boolean addActivatedMasterSwitch(long dpid, IOFSwitch sw) {
        synchronized (multiCacheLock) {
            if (!validActivation(dpid))
                return false;
            activeMasterSwitches.put(dpid, sw);
        }

        // update counters and events
        counters.switchActivated.updateCounterWithFlush();
        evSwitch.updateEventWithFlush(new SwitchEvent(dpid, "activeMaster"));
        addUpdateToQueue(new SwitchUpdate(dpid,
                SwitchUpdateType.ACTIVATED_MASTER));
        return true;
    }

    /**
     * Called when a switch is activated, with this controller's role as SLAVE
     */
    protected boolean addActivatedSlaveSwitch(long dpid, IOFSwitch sw) {
        synchronized (multiCacheLock) {
            if (!validActivation(dpid))
                return false;
            activeSlaveSwitches.put(dpid, sw);
        }
        // update counters and events
        counters.switchActivated.updateCounterWithFlush();
        evSwitch.updateEventWithFlush(new SwitchEvent(dpid, "activeEqual"));
        addUpdateToQueue(new SwitchUpdate(dpid,
                SwitchUpdateType.ACTIVATED_SLAVE));
        return true;
    }

    /**
     * Called when this controller's role for a switch transitions from slave to
     * master.
     */
    protected void transitionToMasterSwitch(long dpid) {
        synchronized (multiCacheLock) {
            IOFSwitch sw = activeSlaveSwitches.remove(dpid);
            if (sw == null) {
                log.error("Transition to master called on sw {}, but switch "
                        + "was not found in controller-cache", dpid);
                return;
            }
            activeMasterSwitches.put(dpid, sw);
        }
        addUpdateToQueue(new SwitchUpdate(dpid,
                SwitchUpdateType.SLAVE_TO_MASTER));
    }

    /**
     * Called when this controller's role for a switch transitions to slave.
     */
    protected void transitionToSlaveSwitch(long dpid) {
        synchronized (multiCacheLock) {
            IOFSwitch sw = activeMasterSwitches.remove(dpid);
            if (sw == null) {
                log.error("Transition to slave called on sw {}, but switch "
                        + "was not found in controller-cache", dpid);
                return;
            }
            activeSlaveSwitches.put(dpid, sw);
        }
        addUpdateToQueue(new SwitchUpdate(dpid,
                SwitchUpdateType.MASTER_TO_SLAVE));
    }

    /**
     * Clear all state in controller switch maps for a switch that has
     * disconnected from the local controller. Also release control for that
     * switch from the global repository. Notify switch listeners.
     */
    protected void removeConnectedSwitch(long dpid) {
        releaseRegistryControl(dpid);
        OFChannelHandler ch = connectedSwitches.remove(dpid);
        IOFSwitch sw = activeMasterSwitches.remove(dpid);
        if (sw == null) {
            sw = activeSlaveSwitches.remove(dpid);
        }
        if (sw != null) {
            sw.cancelAllStatisticsReplies();
            sw.setConnected(false); // do we need this?
        }
        evSwitch.updateEventWithFlush(new SwitchEvent(dpid, "disconnected"));
        counters.switchDisconnected.updateCounterWithFlush();
        if (ch != null) {
            addUpdateToQueue(new SwitchUpdate(dpid, SwitchUpdateType.DISCONNECTED));
        }
    }

    /**
     * Indicates that ports on the given switch have changed. Enqueue a switch
     * update.
     *
     * @param sw
     */
    protected void notifyPortChanged(long dpid, OFPortDesc port,
            PortChangeType changeType) {
        if (port == null || changeType == null) {
            String msg = String.format("Switch port or changeType must not "
                    + "be null in port change notification");
            throw new NullPointerException(msg);
        }
        if (connectedSwitches.get(dpid) == null || getSwitch(dpid) == null) {
            log.warn("Port change update on switch {} not connected or activated "
                    + "... Aborting.", HexString.toHexString(dpid));
            return;
        }

        SwitchUpdate update = new SwitchUpdate(dpid, SwitchUpdateType.PORTCHANGED,
                port, changeType);
        addUpdateToQueue(update);
    }

    // ***************
    // Getters/Setters
    // ***************

    public void setRestApiService(IRestApiService restApi) {
        this.restApi = restApi;
    }

    public void setThreadPoolService(IThreadPoolService tp) {
        this.threadPool = tp;
    }

    public void setMastershipService(IControllerRegistryService serviceImpl) {
        this.registryService = serviceImpl;
    }

    public void setLinkDiscoveryService(ILinkDiscoveryService linkDiscovery) {
        this.linkDiscovery = linkDiscovery;
    }

    public void setNetworkConfigService(INetworkConfigService networkConfigService) {
        this.networkConfig = networkConfigService;
    }

    public void setDebugCounter(IDebugCounterService debugCounters) {
        this.debugCounters = debugCounters;
    }

    public void setDebugEvent(IDebugEventService debugEvents) {
        this.debugEvents = debugEvents;
    }

    IDebugCounterService getDebugCounter() {
        return this.debugCounters;
    }

    // **********************
    // Role Handling
    // **********************

    /**
     * created by ONOS - works with registry service
     */
    protected class RoleChangeCallback implements ControlChangeCallback {
        @Override
        public void controlChanged(long dpidLong, boolean hasControl) {
            Dpid dpid = new Dpid(dpidLong);
            log.info("Role change callback for switch {}, hasControl {}",
                    dpid, hasControl);

            Role role = null;

            /*
             * issue #229
             * Cannot rely on sw.getRole() as it can be behind due to pending
             * role changes in the queue. Just submit it and late the
             * RoleChanger handle duplicates.
             */

            if (hasControl) {
                role = Role.MASTER;
            } else {
                role = Role.SLAVE;
            }

            OFChannelHandler swCh = connectedSwitches.get(dpid.value());
            if (swCh == null) {
                log.warn("Switch {} not found in connected switches", dpid);
                return;
            }

            swCh.sendRoleRequest(role, RoleRecvStatus.MATCHED_SET_ROLE);
        }
    }

    public synchronized void submitRegistryRequest(long dpid) {
        OFChannelHandler h = connectedSwitches.get(dpid);
        if (h == null) {
            log.error("Trying to request registry control for switch {} "
                    + "not in connected switches. Aborting.. ",
                    HexString.toHexString(dpid));
            // FIXME shouldn't we immediately return here?
        }
        // Request control of the switch from the global registry
        try {
            h.controlRequested = Boolean.TRUE;
            registryService.requestControl(dpid, new RoleChangeCallback());
        } catch (RegistryException e) {
            log.debug("Registry error: {}", e.getMessage());
            h.controlRequested = Boolean.FALSE;
        }
        if (!h.controlRequested) { // XXX what is being attempted here?
            // yield to allow other thread(s) to release control
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore interruptions
            }
            // safer to bounce the switch to reconnect here than proceeding
            // further
            // XXX S why? can't we just try again a little later?
            log.debug("Closing sw:{} because we weren't able to request control " +
                    "successfully" + dpid);
            connectedSwitches.get(dpid).disconnectSwitch();
        }
    }

    public synchronized void releaseRegistryControl(long dpidLong) {
        OFChannelHandler h = connectedSwitches.get(dpidLong);
        if (h == null) {
            log.error("Trying to release registry control for switch {} "
                    + "not in connected switches. Aborting.. ",
                    HexString.toHexString(dpidLong));
            return;
        }
        if (h.controlRequested) {
            registryService.releaseControl(dpidLong);
        }
    }

    // *******************
    // OF Message Handling
    // *******************

    /**
     * Handle and dispatch a message to IOFMessageListeners.
     *
     * We only dispatch messages to listeners if the controller's role is
     * MASTER.
     *
     * @param sw The switch sending the message
     * @param m The message the switch sent
     * @param flContext The floodlight context to use for this message. If null,
     *        a new context will be allocated.
     * @throws IOException
     *
     *         FIXME: this method and the ChannelHandler disagree on which
     *         messages should be dispatched and which shouldn't
     */
    @LogMessageDocs({
            @LogMessageDoc(level = "ERROR",
                    message = "Ignoring PacketIn (Xid = {xid}) because the data" +
                            " field is empty.",
                    explanation = "The switch sent an improperly-formatted PacketIn" +
                            " message",
                    recommendation = LogMessageDoc.CHECK_SWITCH),
            @LogMessageDoc(level = "WARN",
                    message = "Unhandled OF Message: {} from {}",
                    explanation = "The switch sent a message not handled by " +
                            "the controller")
    })
    @SuppressWarnings({"fallthrough", "unchecked"})
    protected void handleMessage(IOFSwitch sw, OFMessage m,
            FloodlightContext bContext)
            throws IOException {
        Ethernet eth = null;
        // FIXME losing port number precision here
        short inport = -1;

        switch (m.getType()) {
        case PACKET_IN:
            OFPacketIn pi = (OFPacketIn) m;
            // log.info("saw packet in from sw {}", sw.getStringId());
            if (pi.getData().length <= 0) {
                log.error("Ignoring PacketIn (Xid = " + pi.getXid() +
                        ") because/*  the data field is empty.");
                return;
            }

            // get incoming port to store in floodlight context
            if (sw.getOFVersion() == OFVersion.OF_10) {
                inport = pi.getInPort().getShortPortNumber();
            } else if (sw.getOFVersion() == OFVersion.OF_13) {
                for (MatchField<?> mf : pi.getMatch().getMatchFields()) {
                    if (mf.id == MatchFields.IN_PORT) {
                        inport = pi.getMatch().get((MatchField<OFPort>) mf)
                                .getShortPortNumber();
                        break;
                    }
                }
                if (inport == -1) {
                    log.error("Match field for incoming port missing in "
                            + "packet-in from sw {}. Ignoring msg",
                            sw.getStringId());
                    return;
                }
            } else {
                // should have been taken care of earlier in handshake
                log.error("OFVersion {} not supported for "
                        + "packet-in from sw {}. Ignoring msg",
                        sw.getOFVersion(), sw.getStringId());
                return;
            }

            // decode enclosed ethernet packet to store in floodlight context
            if (Controller.ALWAYS_DECODE_ETH) {
                eth = new Ethernet();
                eth.deserialize(pi.getData(), 0,
                        pi.getData().length);
            }
            // fall through to default case...

            /*log.debug("Sw:{} packet-in: {}", sw.getStringId(),
            String.format("0x%x", eth.getEtherType()));*/
            if (eth.getEtherType() != (short) EthType.LLDP.getValue())
                log.debug("Sw:{} packet-in: {}", sw.getStringId(), pi);

        default:

            List<IOFMessageListener> listeners = null;

            if (messageListeners.containsKey(m.getType())) {
                listeners = messageListeners.get(m.getType()).
                        getOrderedListeners();
            }
            FloodlightContext bc = null;
            if (listeners != null) {
                // Check if floodlight context is passed from the calling
                // function, if so use that floodlight context, otherwise
                // allocate one
                if (bContext == null) {
                    bc = flcontext_alloc();
                } else {
                    bc = bContext;
                }
                if (eth != null) {
                    IFloodlightProviderService.bcStore.put(bc,
                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                            eth);
                }
                if (inport != -1) {
                    bc.getStorage().put(
                            IFloodlightProviderService.CONTEXT_PI_INPORT,
                            inport);
                }

                // Get the starting time (overall and per-component) of
                // the processing chain for this packet if performance
                // monitoring is turned on

                Command cmd = null;
                for (IOFMessageListener listener : listeners) {
                    if (listener instanceof IOFSwitchFilter) {
                        if (!((IOFSwitchFilter) listener).isInterested(sw)) {
                            continue;
                        }
                    }

                    cmd = listener.receive(sw, m, bc);

                    if (Command.STOP.equals(cmd)) {
                        break;
                    }
                }

            } else {
                log.warn("Unhandled OF Message: {} from {}", m, sw);
            }

            if ((bContext == null) && (bc != null))
                flcontext_free(bc);
        }
    }

    // ***************
    // IFloodlightProviderService
    // ***************

    // FIXME: remove this method
    @Override
    public Map<Long, IOFSwitch> getSwitches() {
        return getMasterSwitches();
    }

    // FIXME: remove this method
    public Map<Long, IOFSwitch> getMasterSwitches() {
        return Collections.unmodifiableMap(activeMasterSwitches);
    }

    @Override
    public Set<Long> getAllSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeMasterSwitches.keySet());
        dpids.addAll(activeSlaveSwitches.keySet());
        return dpids;
    }

    @Override
    public Set<Long> getAllMasterSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeMasterSwitches.keySet());
        return dpids;
    }

    @Override
    public Set<Long> getAllSlaveSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeSlaveSwitches.keySet());
        return dpids;
    }

    @Override
    public IOFSwitch getSwitch(long dpid) {
        IOFSwitch sw = null;
        if ((sw = activeMasterSwitches.get(dpid)) != null)
            return sw;
        if ((sw = activeSlaveSwitches.get(dpid)) != null)
            return sw;
        return sw;
    }

    @Override
    public IOFSwitch getMasterSwitch(long dpid) {
        return activeMasterSwitches.get(dpid);
    }

    @Override
    public IOFSwitch getSlaveSwitch(long dpid) {
        return activeSlaveSwitches.get(dpid);
    }

    @Override
    public synchronized void addOFMessageListener(OFType type,
            IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                messageListeners.get(type);
        if (ldd == null) {
            ldd = new ListenerDispatcher<OFType, IOFMessageListener>();
            messageListeners.put(type, ldd);
        }
        ldd.addListener(type, listener);
    }

    @Override
    public synchronized void removeOFMessageListener(OFType type,
            IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                messageListeners.get(type);
        if (ldd != null) {
            ldd.removeListener(listener);
        }
    }

    public void removeOFMessageListeners(OFType type) {
        messageListeners.remove(type);
    }

    private void logListeners() {
        for (Map.Entry<OFType, ListenerDispatcher<OFType, IOFMessageListener>> entry : messageListeners
                .entrySet()) {

            OFType type = entry.getKey();
            ListenerDispatcher<OFType, IOFMessageListener> ldd =
                    entry.getValue();

            StringBuffer sb = new StringBuffer();
            sb.append("OFMessageListeners for ");
            sb.append(type);
            sb.append(": ");
            for (IOFMessageListener l : ldd.getOrderedListeners()) {
                sb.append(l.getName());
                sb.append(",");
            }
            log.debug(sb.toString());
        }
        StringBuffer sl = new StringBuffer();
        sl.append("SwitchUpdate Listeners: ");
        for (IOFSwitchListener swlistener : switchListeners) {
            sl.append(swlistener.getName());
            sl.append(",");
        }
        log.debug(sl.toString());

    }

    @Override
    public void addOFSwitchListener(IOFSwitchListener listener) {
        this.switchListeners.add(listener);
    }

    @Override
    public void removeOFSwitchListener(IOFSwitchListener listener) {
        this.switchListeners.remove(listener);
    }

    @Override
    public Map<OFType, List<IOFMessageListener>> getListeners() {
        Map<OFType, List<IOFMessageListener>> lers =
                new HashMap<OFType, List<IOFMessageListener>>();
        for (Entry<OFType, ListenerDispatcher<OFType, IOFMessageListener>> e : messageListeners
                .entrySet()) {
            lers.put(e.getKey(), e.getValue().getOrderedListeners());
        }
        return Collections.unmodifiableMap(lers);
    }

    /*@Override
    @LogMessageDocs({
            @LogMessageDoc(message = "Failed to inject OFMessage {message} onto " +
                    "a null switch",
                    explanation = "Failed to process a message because the switch " +
                            " is no longer connected."),
            @LogMessageDoc(level = "ERROR",
                    message = "Error reinjecting OFMessage on switch {switch}",
                    explanation = "An I/O error occured while attempting to " +
                            "process an OpenFlow message",
                    recommendation = LogMessageDoc.CHECK_SWITCH)
    })
    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg,
                                   FloodlightContext bc) {
        if (sw == null) {
            log.info("Failed to inject OFMessage {} onto a null switch", msg);
            return false;
        }

        // FIXME: Do we need to be able to inject messages to switches
        // where we're the slave controller (i.e. they're connected but
        // not active)?
        // FIXME: Don't we need synchronization logic here so we're holding
        // the listener read lock when we call handleMessage? After some
        // discussions it sounds like the right thing to do here would be to
        // inject the message as a netty upstream channel event so it goes
        // through the normal netty event processing, including being
        // handled
        if (!activeSwitches.containsKey(sw.getId())) return false;

        try {
            // Pass Floodlight context to the handleMessages()
            handleMessage(sw, msg, bc);
        } catch (IOException e) {
            log.error("Error reinjecting OFMessage on switch {}",
                    HexString.toHexString(sw.getId()));
            return false;
        }
        return true;
    }*/

    // @Override
    // public boolean injectOfMessage(IOFSwitch sw, OFMessage msg) {
    // // call the overloaded version with floodlight context set to null
    // return injectOfMessage(sw, msg, null);
    // }

    // @Override
    // public void handleOutgoingMessage(IOFSwitch sw, OFMessage m,
    // FloodlightContext bc) {
    //
    // List<IOFMessageListener> listeners = null;
    // if (messageListeners.containsKey(m.getType())) {
    // listeners =
    // messageListeners.get(m.getType()).getOrderedListeners();
    // }
    //
    // if (listeners != null) {
    // for (IOFMessageListener listener : listeners) {
    // if (listener instanceof IOFSwitchFilter) {
    // if (!((IOFSwitchFilter) listener).isInterested(sw)) {
    // continue;
    // }
    // }
    // if (Command.STOP.equals(listener.receive(sw, m, bc))) {
    // break;
    // }
    // }
    // }
    // }

    /**
     * Gets an OpenFlow message factory for version 1.0.
     *
     * @return an OpenFlow 1.0 message factory
     */
    public OFFactory getOFMessageFactory_10() {
        return factory10;
    }

    /**
     * Gets an OpenFlow message factory for version 1.3.
     *
     * @return an OpenFlow 1.3 message factory
     */
    public OFFactory getOFMessageFactory_13() {
        return factory13;
    }

    @Override
    public void publishUpdate(IUpdate update) {
        try {
            this.updates.put(update);
        } catch (InterruptedException e) {
            log.error("Failure adding update to queue", e);
        }
    }

    @Override
    public Map<String, String> getControllerNodeIPs() {
        // We return a copy of the mapping so we can guarantee that
        // the mapping return is the same as one that will be (or was)
        // dispatched to IHAListeners
        HashMap<String, String> retval = new HashMap<String, String>();
        synchronized (controllerNodeIPsCache) {
            retval.putAll(controllerNodeIPsCache);
        }
        return retval;
    }

    @Override
    public long getSystemStartTime() {
        return (this.systemStartTime);
    }

    @Override
    public void setAlwaysClearFlowsOnSwAdd(boolean value) {
        this.alwaysClearFlowsOnSwAdd = value;
    }

    @Override
    public OnosInstanceId getOnosInstanceId() {
        return onosInstanceId;
    }

    /**
     * FOR TESTING ONLY. Dispatch all updates in the update queue until queue is
     * empty
     */
    void processUpdateQueueForTesting() {
        while (!updates.isEmpty()) {
            IUpdate update = updates.poll();
            if (update != null)
                update.dispatch();
        }
    }

    public INetworkConfigService getNetworkConfigService() {
        return networkConfig;
    }

    // **************
    // Initialization
    // **************

    // XXX S This should probably go away OR it should be edited to handle
    // controller roles per switch! Then it could be a way to
    // deterministically configure a switch to a MASTER controller instance
    /**
     * Sets the initial role based on properties in the config params. It looks
     * for two different properties. If the "role" property is specified then
     * the value should be either "EQUAL", "MASTER", or "SLAVE" and the role of
     * the controller is set to the specified value. If the "role" property is
     * not specified then it looks next for the "role.path" property. In this
     * case the value should be the path to a property file in the file system
     * that contains a property called "floodlight.role" which can be one of the
     * values listed above for the "role" property. The idea behind the
     * "role.path" mechanism is that you have some separate heartbeat and master
     * controller election algorithm that determines the role of the controller.
     * When a role transition happens, it updates the current role in the file
     * specified by the "role.path" file. Then if floodlight restarts for some
     * reason it can get the correct current role of the controller from the
     * file.
     *
     * @param configParams The config params for the FloodlightProvider service
     * @return A valid role if role information is specified in the config
     *         params, otherwise null
     */
    @LogMessageDocs({
            @LogMessageDoc(message = "Controller role set to {role}",
                    explanation = "Setting the initial HA role to "),
            @LogMessageDoc(level = "ERROR",
                    message = "Invalid current role value: {role}",
                    explanation = "An invalid HA role value was read from the " +
                            "properties file",
                    recommendation = LogMessageDoc.CHECK_CONTROLLER)
    })
    protected Role getInitialRole(Map<String, String> configParams) {
        Role role = null;
        String roleString = configParams.get("role");
        FileInputStream fs = null;
        if (roleString == null) {
            String rolePath = configParams.get("rolepath");
            if (rolePath != null) {
                Properties properties = new Properties();
                try {
                    fs = new FileInputStream(rolePath);
                    properties.load(fs);
                    roleString = properties.getProperty("floodlight.role");
                } catch (IOException exc) {
                    // Don't treat it as an error if the file specified by the
                    // rolepath property doesn't exist. This lets us enable the
                    // HA mechanism by just creating/setting the floodlight.role
                    // property in that file without having to modify the
                    // floodlight properties.
                } finally {
                    if (fs != null) {
                        try {
                            fs.close();
                        } catch (IOException e) {
                            log.error("Exception while closing resource ", e);
                        }
                    }
                }
            }
        }

        if (roleString != null) {
            // Canonicalize the string to the form used for the enum constants
            roleString = roleString.trim().toUpperCase();
            try {
                role = Role.valueOf(roleString);
            } catch (IllegalArgumentException exc) {
                log.error("Invalid current role value: {}", roleString);
            }
        }

        log.info("Controller role set to {}", role);

        return role;
    }

    /**
     * Tell controller that we're ready to accept switches loop
     *
     * @throws IOException
     */
    @Override
    @LogMessageDocs({
            @LogMessageDoc(message = "Listening for switch connections on {address}",
                    explanation = "The controller is ready and listening for new" +
                            " switch connections"),
            @LogMessageDoc(message = "Storage exception in controller " +
                    "updates loop; terminating process",
                    explanation = ERROR_DATABASE,
                    recommendation = LogMessageDoc.CHECK_CONTROLLER),
            @LogMessageDoc(level = "ERROR",
                    message = "Exception in controller updates loop",
                    explanation = "Failed to dispatch controller event",
                    recommendation = LogMessageDoc.GENERIC_ACTION)
    })
    public void run() {
        if (log.isDebugEnabled()) {
            logListeners();
        }

        try {
            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.setOption("reuseAddr", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            ChannelPipelineFactory pfact =
                    new OpenflowPipelineFactory(this, null);
            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(openFlowPort);
            final ChannelGroup cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));

            log.info("Listening for switch connections on {}", sa);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // main loop
        while (true) {
            try {
                IUpdate update = updates.take();
                update.dispatch();
            } catch (InterruptedException e) {
                log.error("Received interrupted exception in updates loop;" +
                        "terminating process");
                terminate();
            } catch (Exception e) {
                log.error("Exception in controller updates loop", e);
            }
        }
    }

    private ServerBootstrap createServerBootStrap() {
        if (workerThreads == 0) {
            return new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool()));
        } else {
            return new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool(), workerThreads));
        }
    }

    public void setConfigParams(Map<String, String> configParams) {
        String ofPort = configParams.get("openflowport");
        if (ofPort != null) {
            this.openFlowPort = Integer.parseInt(ofPort);
        }
        log.debug("OpenFlow port set to {}", this.openFlowPort);
        String threads = configParams.get("workerthreads");
        if (threads != null) {
            this.workerThreads = Integer.parseInt(threads);
        }
        log.debug("Number of worker threads set to {}", this.workerThreads);
        String controllerId = configParams.get("controllerid");
        if (controllerId != null) {
            this.onosInstanceId = new OnosInstanceId(controllerId);
        } else {
            // Try to get the hostname of the machine and use that for
            // controller ID
            try {
                String hostname = java.net.InetAddress.getLocalHost().getHostName();
                this.onosInstanceId = new OnosInstanceId(hostname);
            } catch (UnknownHostException e) {
                // Can't get hostname, we'll just use the default
            }
        }

        String useOnly10 = configParams.get("useOnly10");
        if (useOnly10 != null && useOnly10.equalsIgnoreCase("true")) {
            OFChannelHandler.useOnly10 = true;
            log.info("Setting controller to only use OpenFlow 1.0");
        }

        log.debug("ControllerId set to {}", this.onosInstanceId);
    }

    /**
     * Initialize internal data structures
     */
    public void init(Map<String, String> configParams) {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.messageListeners =
                new ConcurrentHashMap<OFType, ListenerDispatcher<OFType,
                IOFMessageListener>>();
        this.switchListeners = new CopyOnWriteArraySet<IOFSwitchListener>();
        this.activeMasterSwitches = new ConcurrentHashMap<Long, IOFSwitch>();
        this.activeSlaveSwitches = new ConcurrentHashMap<Long, IOFSwitch>();
        this.connectedSwitches = new ConcurrentHashMap<Long, OFChannelHandler>();
        this.controllerNodeIPsCache = new HashMap<String, String>();
        this.updates = new LinkedBlockingQueue<IUpdate>();

        setConfigParams(configParams);
        this.systemStartTime = System.currentTimeMillis();
        this.counters = new Counters();
        this.multiCacheLock = new Object();

        String option = configParams.get("flushSwitchesOnReconnect");
        if (option != null && option.equalsIgnoreCase("true")) {
            this.setAlwaysClearFlowsOnSwActivate(true);
            log.info("Flush switches on reconnect -- Enabled.");
        } else {
            this.setAlwaysClearFlowsOnSwActivate(false);
            log.info("Flush switches on reconnect -- Disabled");
        }

        option = configParams.get("cpqdUsePipeline13");
        if (option != null && option.equalsIgnoreCase("true")) {
            DriverManager.setConfigForCpqd(true);
            log.info("Using OF1.3 pipeline for the CPqD software switch");
        } else {
            log.info("Using OF1.0 pipeline for the CPqD software switch");
        }

        String disableOvsClassification =
                configParams.get("disableOvsClassification");
        if (disableOvsClassification != null &&
                disableOvsClassification.equalsIgnoreCase("true")) {
            DriverManager.setDisableOvsClassification(true);
            log.info("OVS switches will be classified as default switches");
        }
    }

    /**
     * Startup all of the controller's components
     *
     * @throws FloodlightModuleException
     */
    @LogMessageDoc(message = "Waiting for storage source",
            explanation = "The system database is not yet ready",
            recommendation = "If this message persists, this indicates " +
                    "that the system database has failed to start. " +
                    LogMessageDoc.CHECK_CONTROLLER)
    public void startupComponents() throws FloodlightModuleException {
        try {
            registryService.registerController(onosInstanceId.toString());
        } catch (RegistryException e) {
            log.warn("Registry service error: {}", e.getMessage());
        }

        // Add our REST API
        restApi.addRestletRoutable(new CoreWebRoutable());

        // Startup load monitoring
        if (overload_drop) {
            this.loadmonitor.startMonitoring(
                    this.threadPool.getScheduledExecutor());
        }

        // register counters and events
        try {
            this.counters.createCounters(debugCounters);
        } catch (CounterException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
        registerControllerDebugEvents();
    }

    // **************
    // debugCounter registrations
    // **************

    public static class Counters {
        public static final String prefix = "controller";
        public IDebugCounter setRoleEqual;
        public IDebugCounter setSameRole;
        public IDebugCounter setRoleMaster;
        public IDebugCounter remoteStoreNotification;
        public IDebugCounter invalidPortsChanged;
        public IDebugCounter invalidSwitchActivatedWhileSlave;
        public IDebugCounter invalidStoreEventWhileMaster;
        public IDebugCounter switchDisconnectedWhileSlave;
        public IDebugCounter switchActivated;
        public IDebugCounter errorSameSwitchReactivated; // err
        public IDebugCounter switchWithSameDpidActivated; // warn
        public IDebugCounter newSwitchActivated; // new switch
        public IDebugCounter syncedSwitchActivated;
        public IDebugCounter readyForReconcile;
        public IDebugCounter newSwitchFromStore;
        public IDebugCounter updatedSwitchFromStore;
        public IDebugCounter switchDisconnected;
        public IDebugCounter syncedSwitchRemoved;
        public IDebugCounter unknownSwitchRemovedFromStore;
        public IDebugCounter consolidateStoreRunCount;
        public IDebugCounter consolidateStoreInconsistencies;
        public IDebugCounter storeSyncError;
        public IDebugCounter switchesNotReconnectingToNewMaster;
        public IDebugCounter switchPortChanged;
        public IDebugCounter switchOtherChange;
        public IDebugCounter dispatchMessageWhileSlave;
        public IDebugCounter dispatchMessage; // does this cnt make sense? more
                                              // specific?? per type? count
                                              // stops?
        public IDebugCounter controllerNodeIpsChanged;
        public IDebugCounter messageReceived;
        public IDebugCounter messageInputThrottled;
        public IDebugCounter switchDisconnectReadTimeout;
        public IDebugCounter switchDisconnectHandshakeTimeout;
        public IDebugCounter switchDisconnectIOError;
        public IDebugCounter switchDisconnectParseError;
        public IDebugCounter switchDisconnectSwitchStateException;
        public IDebugCounter rejectedExecutionException;
        public IDebugCounter switchDisconnectOtherException;
        public IDebugCounter switchConnected;
        public IDebugCounter unhandledMessage;
        public IDebugCounter packetInWhileSwitchIsSlave;
        public IDebugCounter epermErrorWhileSwitchIsMaster;
        public IDebugCounter roleNotResentBecauseRolePending;
        public IDebugCounter roleRequestSent;
        public IDebugCounter roleReplyTimeout;
        public IDebugCounter roleReplyReceived; // expected RoleReply received
        public IDebugCounter roleReplyErrorUnsupported;
        public IDebugCounter switchCounterRegistrationFailed;
        public IDebugCounter packetParsingError;

        void createCounters(IDebugCounterService debugCounters) throws CounterException {
            setRoleEqual =
                    debugCounters.registerCounter(
                            prefix, "set-role-equal",
                            "Controller received a role request with role of " +
                                    "EQUAL which is unusual",
                            CounterType.ALWAYS_COUNT);
            setSameRole =
                    debugCounters.registerCounter(
                            prefix, "set-same-role",
                            "Controller received a role request for the same " +
                                    "role the controller already had",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            setRoleMaster =
                    debugCounters.registerCounter(
                            prefix, "set-role-master",
                            "Controller received a role request with role of " +
                                    "MASTER. This counter can be at most 1.",
                            CounterType.ALWAYS_COUNT);

            remoteStoreNotification =
                    debugCounters.registerCounter(
                            prefix, "remote-store-notification",
                            "Received a notification from the sync service " +
                                    "indicating that switch information has changed",
                            CounterType.ALWAYS_COUNT);

            invalidPortsChanged =
                    debugCounters.registerCounter(
                            prefix, "invalid-ports-changed",
                            "Received an unexpected ports changed " +
                                    "notification while the controller was in " +
                                    "SLAVE role.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            invalidSwitchActivatedWhileSlave =
                    debugCounters.registerCounter(
                            prefix, "invalid-switch-activated-while-slave",
                            "Received an unexpected switchActivated " +
                                    "notification while the controller was in " +
                                    "SLAVE role.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            invalidStoreEventWhileMaster =
                    debugCounters.registerCounter(
                            prefix, "invalid-store-event-while-master",
                            "Received an unexpected notification from " +
                                    "the sync store while the controller was in " +
                                    "MASTER role.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            switchDisconnectedWhileSlave =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnected-while-slave",
                            "A switch disconnected and the controller was " +
                                    "in SLAVE role.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            switchActivated =
                    debugCounters.registerCounter(
                            prefix, "switch-activated",
                            "A switch connected to this controller is now " +
                                    "in MASTER role",
                            CounterType.ALWAYS_COUNT);

            errorSameSwitchReactivated = // err
            debugCounters.registerCounter(
                    prefix, "error-same-switch-reactivated",
                    "A switch that was already in active state " +
                            "was activated again. This indicates a " +
                            "controller defect",
                    CounterType.ALWAYS_COUNT,
                    IDebugCounterService.CTR_MDATA_ERROR);

            switchWithSameDpidActivated = // warn
            debugCounters.registerCounter(
                    prefix, "switch-with-same-dpid-activated",
                    "A switch with the same DPID as another switch " +
                            "connected to the controller. This can be " +
                            "caused by multiple switches configured with " +
                            "the same DPID or by a switch reconnecting very " +
                            "quickly.",
                    CounterType.COUNT_ON_DEMAND,
                    IDebugCounterService.CTR_MDATA_WARN);

            newSwitchActivated = // new switch
            debugCounters.registerCounter(
                    prefix, "new-switch-activated",
                    "A new switch has completed the handshake as " +
                            "MASTER. The switch was not known to any other " +
                            "controller in the cluster",
                    CounterType.ALWAYS_COUNT);
            syncedSwitchActivated =
                    debugCounters.registerCounter(
                            prefix, "synced-switch-activated",
                            "A switch has completed the handshake as " +
                                    "MASTER. The switch was known to another " +
                                    "controller in the cluster",
                            CounterType.ALWAYS_COUNT);

            readyForReconcile =
                    debugCounters.registerCounter(
                            prefix, "ready-for-reconcile",
                            "Controller is ready for flow reconciliation " +
                                    "after Slave to Master transition. Either all " +
                                    "previously known switches are now active " +
                                    "or they have timed out and have been removed." +
                                    "This counter will be 0 or 1.",
                            CounterType.ALWAYS_COUNT);

            newSwitchFromStore =
                    debugCounters.registerCounter(
                            prefix, "new-switch-from-store",
                            "A new switch has connected to another " +
                                    "another controller in the cluster. This " +
                                    "controller instance has received a sync store " +
                                    "notification for it.",
                            CounterType.ALWAYS_COUNT);

            updatedSwitchFromStore =
                    debugCounters.registerCounter(
                            prefix, "updated-switch-from-store",
                            "Information about a switch connected to " +
                                    "another controller instance was updated in " +
                                    "the sync store. This controller instance has " +
                                    "received a notification for it",
                            CounterType.ALWAYS_COUNT);

            switchDisconnected =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnected",
                            "FIXME: switch has disconnected",
                            CounterType.ALWAYS_COUNT);

            syncedSwitchRemoved =
                    debugCounters.registerCounter(
                            prefix, "synced-switch-removed",
                            "A switch connected to another controller " +
                                    "instance has disconnected from the controller " +
                                    "cluster. This controller instance has " +
                                    "received a notification for it",
                            CounterType.ALWAYS_COUNT);

            unknownSwitchRemovedFromStore =
                    debugCounters.registerCounter(
                            prefix, "unknown-switch-removed-from-store",
                            "This controller instances has received a sync " +
                                    "store notification that a switch has " +
                                    "disconnected but this controller instance " +
                                    "did not have the any information about the " +
                                    "switch", // might be less than warning
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            consolidateStoreRunCount =
                    debugCounters.registerCounter(
                            prefix, "consolidate-store-run-count",
                            "This controller has transitioned from SLAVE " +
                                    "to MASTER and waited for switches to reconnect. " +
                                    "The controller has finished waiting and has " +
                                    "reconciled switch entries in the sync store " +
                                    "with live state",
                            CounterType.ALWAYS_COUNT);

            consolidateStoreInconsistencies =
                    debugCounters.registerCounter(
                            prefix, "consolidate-store-inconsistencies",
                            "During switch sync store consolidation: " +
                                    "Number of switches that were in the store " +
                                    "but not otherwise known plus number of " +
                                    "switches that were in the store previously " +
                                    "but are now missing plus number of " +
                                    "connected switches that were absent from " +
                                    "the store although this controller has " +
                                    "written them. A non-zero count " +
                                    "indicates a brief split-brain dual MASTER " +
                                    "situation during fail-over",
                            CounterType.ALWAYS_COUNT);

            storeSyncError =
                    debugCounters.registerCounter(
                            prefix, "store-sync-error",
                            "Number of times a sync store operation failed " +
                                    "due to a store sync exception or an entry in " +
                                    "in the store had invalid data.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchesNotReconnectingToNewMaster =
                    debugCounters.registerCounter(
                            prefix, "switches-not-reconnecting-to-new-master",
                            "Switches that were connected to another " +
                                    "controller instance in the cluster but that " +
                                    "did not reconnect to this controller after it " +
                                    "transitioned to MASTER", // might be less
                                                              // than warning
                            CounterType.ALWAYS_COUNT);

            switchPortChanged =
                    debugCounters.registerCounter(
                            prefix, "switch-port-changed",
                            "Number of times switch ports have changed",
                            CounterType.ALWAYS_COUNT);
            switchOtherChange =
                    debugCounters.registerCounter(
                            prefix, "switch-other-change",
                            "Number of times other information of a switch " +
                                    "has changed.",
                            CounterType.ALWAYS_COUNT);

            dispatchMessageWhileSlave =
                    debugCounters.registerCounter(
                            prefix, "dispatch-message-while-slave",
                            "Number of times an OF message was received " +
                                    "and supposed to be dispatched but the " +
                                    "controller was in SLAVE role and the message " +
                                    "was not dispatched",
                            CounterType.ALWAYS_COUNT);

            dispatchMessage = // does this cnt make sense? more specific?? per
                              // type? count stops?
            debugCounters.registerCounter(
                    prefix, "dispatch-message",
                    "Number of times an OF message was dispatched " +
                            "to registered modules",
                    CounterType.ALWAYS_COUNT);

            controllerNodeIpsChanged =
                    debugCounters.registerCounter(
                            prefix, "controller-nodes-ips-changed",
                            "IP addresses of controller nodes have changed",
                            CounterType.ALWAYS_COUNT);

            // ------------------------
            // channel handler counters. Factor them out ??
            messageReceived =
                    debugCounters.registerCounter(
                            prefix, "message-received",
                            "Number of OpenFlow messages received. Some of " +
                                    "these might be throttled",
                            CounterType.ALWAYS_COUNT);
            messageInputThrottled =
                    debugCounters.registerCounter(
                            prefix, "message-input-throttled",
                            "Number of OpenFlow messages that were " +
                                    "throttled due to high load from the sender",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);
            // TODO: more counters in messageReceived ??

            switchDisconnectReadTimeout =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-read-timeout",
                            "Number of times a switch was disconnected due " +
                                    "due the switch failing to send OpenFlow " +
                                    "messages or responding to OpenFlow ECHOs",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectHandshakeTimeout =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-handshake-timeout",
                            "Number of times a switch was disconnected " +
                                    "because it failed to complete the handshake " +
                                    "in time.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectIOError =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-io-error",
                            "Number of times a switch was disconnected " +
                                    "due to IO errors on the switch connection.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectParseError =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-parse-error",
                            "Number of times a switch was disconnected " +
                                    "because it sent an invalid packet that could " +
                                    "not be parsed",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchDisconnectSwitchStateException =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-switch-state-exception",
                            "Number of times a switch was disconnected " +
                                    "because it sent messages that were invalid " +
                                    "given the switch connection's state.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            rejectedExecutionException =
                    debugCounters.registerCounter(
                            prefix, "rejected-execution-exception",
                            "TODO",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchDisconnectOtherException =
                    debugCounters.registerCounter(
                            prefix, "switch-disconnect-other-exception",
                            "Number of times a switch was disconnected " +
                                    "due to an exceptional situation not covered " +
                                    "by other counters",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchConnected =
                    debugCounters.registerCounter(
                            prefix, "switch-connected",
                            "Number of times a new switch connection was " +
                                    "established",
                            CounterType.ALWAYS_COUNT);

            unhandledMessage =
                    debugCounters.registerCounter(
                            prefix, "unhandled-message",
                            "Number of times an OpenFlow message was " +
                                    "received that the controller ignored because " +
                                    "it was inapproriate given the switch " +
                                    "connection's state.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);
            // might be less than warning

            packetInWhileSwitchIsSlave =
                    debugCounters.registerCounter(
                            prefix, "packet-in-while-switch-is-slave",
                            "Number of times a packet in was received " +
                                    "from a switch that was in SLAVE role. " +
                                    "Possibly inidicates inconsistent roles.",
                            CounterType.ALWAYS_COUNT);
            epermErrorWhileSwitchIsMaster =
                    debugCounters.registerCounter(
                            prefix, "eperm-error-while-switch-is-master",
                            "Number of times a permission error was " +
                                    "received while the switch was in MASTER role. " +
                                    "Possibly inidicates inconsistent roles.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            roleNotResentBecauseRolePending =
                    debugCounters.registerCounter(
                            prefix, "role-not-resent-because-role-pending",
                            "The controller tried to reestablish a role " +
                                    "with a switch but did not do so because a " +
                                    "previous role request was still pending",
                            CounterType.ALWAYS_COUNT);
            roleRequestSent =
                    debugCounters.registerCounter(
                            prefix, "role-request-sent",
                            "Number of times the controller sent a role " +
                                    "request to a switch.",
                            CounterType.ALWAYS_COUNT);
            roleReplyTimeout =
                    debugCounters.registerCounter(
                            prefix, "role-reply-timeout",
                            "Number of times a role request message did not " +
                                    "receive the expected reply from a switch",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            roleReplyReceived = // expected RoleReply received
            debugCounters.registerCounter(
                    prefix, "role-reply-received",
                    "Number of times the controller received the " +
                            "expected role reply message from a switch",
                    CounterType.ALWAYS_COUNT);

            roleReplyErrorUnsupported =
                    debugCounters.registerCounter(
                            prefix, "role-reply-error-unsupported",
                            "Number of times the controller received an " +
                                    "error from a switch in response to a role " +
                                    "request indicating that the switch does not " +
                                    "support roles.",
                            CounterType.ALWAYS_COUNT);

            switchCounterRegistrationFailed =
                    debugCounters.registerCounter(prefix,
                            "switch-counter-registration-failed",
                            "Number of times the controller failed to " +
                                    "register per-switch debug counters",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            packetParsingError =
                    debugCounters.registerCounter(prefix,
                            "packet-parsing-error",
                            "Number of times the packet parsing " +
                                    "encountered an error",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
        }
    }

    @Override
    public Counters getCounters() {
        return this.counters;
    }

    // **************
    // debugEvent registrations
    // **************

    private void registerControllerDebugEvents() throws FloodlightModuleException {
        if (debugEvents == null) {
            debugEvents = new NullDebugEvent();
        }
        try {
            evSwitch = debugEvents.registerEvent(
                    Counters.prefix, "switchevent",
                    "Switch connected, disconnected or port changed",
                    EventType.ALWAYS_LOG, SwitchEvent.class, 100);
        } catch (MaxEventsRegistered e) {
            throw new FloodlightModuleException("Max events registered", e);
        }
    }

    public class SwitchEvent {
        @EventColumn(name = "dpid", description = EventFieldType.DPID)
        long dpid;

        @EventColumn(name = "reason", description = EventFieldType.STRING)
        String reason;

        public SwitchEvent(long dpid, String reason) {
            this.dpid = dpid;
            this.reason = reason;
        }
    }

    // **************
    // Utility methods
    // **************

    @Override
    public void setAlwaysClearFlowsOnSwActivate(boolean value) {
        // this.alwaysClearFlowsOnSwActivate = value;
        // XXX S need to be a little more careful about this
    }

    @Override
    public Map<String, Long> getMemory() {
        Map<String, Long> m = new HashMap<String, Long>();
        Runtime runtime = Runtime.getRuntime();
        m.put("total", runtime.totalMemory());
        m.put("free", runtime.freeMemory());
        return m;
    }

    @Override
    public Long getUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    /**
     * Forward to the driver-manager to get an IOFSwitch instance.
     *
     * @param desc
     * @return
     */
    protected IOFSwitch getOFSwitchInstance(OFDescStatsReply desc, OFVersion ofv) {
        return DriverManager.getOFSwitchImpl(desc, ofv);
    }

    protected IThreadPoolService getThreadPoolService() {
        return this.threadPool;
    }

    /**
     * Part of the controller updates framework (see 'run()' method) Use this
     * method to add an IUpdate. A thread-pool will serve the update by
     * dispatching it to all listeners for that update.
     *
     * @param update
     */
    @LogMessageDoc(level = "WARN",
            message = "Failure adding update {} to queue",
            explanation = "The controller tried to add an internal notification" +
                    " to its message queue but the add failed.",
            recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG)
    private void addUpdateToQueue(IUpdate update) {
        try {
            this.updates.put(update);
        } catch (InterruptedException e) {
            // This should never happen
            log.error("Failure adding update {} to queue.", update);
        }
    }

    void flushAll() {
        // Flush all flow-mods/packet-out/stats generated from this "train"
        OFSwitchImplBase.flush_all();
        debugCounters.flushCounters();
        debugEvents.flushEvents();
    }

    /**
     * flcontext_free - Free the context to the current thread
     *
     * @param flcontext
     */
    protected void flcontext_free(FloodlightContext flcontext) {
        flcontext.getStorage().clear();
        flcontext_cache.get().push(flcontext);
    }

    @LogMessageDoc(message = "Calling System.exit",
            explanation = "The controller is terminating")
    private synchronized void terminate() {
        log.info("Calling System.exit");
        System.exit(1);
    }

    // ***************
    // Floodlight context related
    // ***************

    /**
     * flcontext_cache - Keep a thread local stack of contexts
     */
    protected static final ThreadLocal<Stack<FloodlightContext>> flcontext_cache =
            new ThreadLocal<Stack<FloodlightContext>>() {
                @Override
                protected Stack<FloodlightContext> initialValue() {
                    return new Stack<FloodlightContext>();
                }
            };

    /**
     * flcontext_alloc - pop a context off the stack, if required create a new
     * one
     *
     * @return FloodlightContext
     */
    protected static FloodlightContext flcontext_alloc() {
        FloodlightContext flcontext = null;

        if (flcontext_cache.get().empty()) {
            flcontext = new FloodlightContext();
        } else {
            flcontext = flcontext_cache.get().pop();
        }

        return flcontext;
    }

}
