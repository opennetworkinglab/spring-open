package net.floodlightcontroller.core.internal;

/**
 *    Copyright 2012, Big Switch Networks, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.web.serializers.DPIDSerializer;
import net.floodlightcontroller.debugcounter.IDebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterException;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterType;
import net.floodlightcontroller.debugcounter.NullDebugCounter;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.LinkedHashSetWrapper;
import net.floodlightcontroller.util.OrderedCollection;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowDelete.Builder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFAuxId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the internal representation of an openflow switch.
 */
public class OFSwitchImplBase implements IOFSwitch {
    // TODO: should we really do logging in the class or should we throw
    // exception that can then be handled by callers?
    protected final static Logger log = LoggerFactory.getLogger(OFSwitchImplBase.class);

    private static final String HA_CHECK_SWITCH =
            "Check the health of the indicated switch.  If the problem " +
                    "persists or occurs repeatedly, it likely indicates a defect " +
                    "in the switch HA implementation.";

    protected ConcurrentMap<Object, Object> attributes;
    protected IFloodlightProviderService floodlightProvider;
    protected IThreadPoolService threadPool;
    protected Date connectedSince;
    protected String stringId;
    protected Channel channel;
    // transaction id used for messages sent out to this switch from
    // this controller instance. This xid has significance only between this
    // controller<->switch pair.
    protected AtomicInteger transactionIdSource;

    // generation id used for roleRequest messages sent to switches (see section
    // 6.3.5 of the OF1.3.4 spec). This generationId has significance between
    // all the controllers that this switch is connected to; and only for role
    // request messages with role MASTER or SLAVE. The set of Controllers that
    // this switch is connected to should coordinate the next generation id,
    // via transactional semantics.
    protected long generationIdSource;

    // Lock to protect modification of the port maps. We only need to
    // synchronize on modifications. For read operations we are fine since
    // we rely on ConcurrentMaps which works for our use case.
    // private Object portLock; XXX S remove this

    // Map port numbers to the appropriate OFPortDesc
    protected ConcurrentHashMap<Integer, OFPortDesc> portsByNumber;
    // Map port names to the appropriate OFPhyiscalPort
    // XXX: The OF spec doesn't specify if port names need to be unique but
    // according it's always the case in practice.
    protected ConcurrentHashMap<String, OFPortDesc> portsByName;
    protected Map<Integer, OFStatisticsFuture> statsFutureMap;
    // XXX Consider removing the following 2 maps - not used anymore
    protected Map<Integer, IOFMessageListener> iofMsgListenersMap;
    protected Map<Integer, OFFeaturesReplyFuture> featuresFutureMap;
    protected boolean connected;
    protected Role role;
    protected ReentrantReadWriteLock listenerLock;
    protected ConcurrentMap<Short, Long> portBroadcastCacheHitMap;
    /**
     * When sending a role request message, the role request is added to this
     * queue. If a role reply is received this queue is checked to verify that
     * the reply matches the expected reply. We require in order delivery of
     * replies. That's why we use a Queue. The RoleChanger uses a timeout to
     * ensure we receive a timely reply.
     * <p/>
     * Need to synchronize on this instance if a request is sent, received,
     * checked.
     */
    protected LinkedList<PendingRoleRequestEntry> pendingRoleRequests;

    /** OpenFlow version for this switch */
    protected OFVersion ofversion;
    // Description stats reply describing this switch
    private OFDescStatsReply switchDescription;
    // Switch features from initial featuresReply
    protected Set<OFCapabilities> capabilities;
    protected int buffers;
    protected Set<OFActionType> actions;
    protected byte tables;
    protected DatapathId datapathId;
    private OFAuxId auxId;

    private IDebugCounterService debugCounters;
    private boolean debugCountersRegistered;
    @SuppressWarnings("unused")
    private IDebugCounter ctrSwitch, ctrSwitchPktin, ctrSwitchWrite,
            ctrSwitchPktinDrops, ctrSwitchWriteDrops;

    protected boolean startDriverHandshakeCalled = false;
    private boolean flowTableFull = false;

    private final PortManager portManager;

    protected static final ThreadLocal<Map<OFSwitchImplBase, List<OFMessage>>> local_msg_buffer =
            new ThreadLocal<Map<OFSwitchImplBase, List<OFMessage>>>() {
                @Override
                protected Map<OFSwitchImplBase, List<OFMessage>> initialValue() {
                    return new WeakHashMap<OFSwitchImplBase, List<OFMessage>>();
                }
            };

    private static final String BASE = "switchbase";

    protected static class PendingRoleRequestEntry {
        protected int xid;
        protected Role role;
        // cookie is used to identify the role "generation". roleChanger uses
        protected long cookie;

        public PendingRoleRequestEntry(int xid, Role role, long cookie) {
            this.xid = xid;
            this.role = role;
            this.cookie = cookie;
        }
    }

    public OFSwitchImplBase() {
        this.stringId = null;
        this.attributes = new ConcurrentHashMap<Object, Object>();
        this.connectedSince = new Date();
        this.transactionIdSource = new AtomicInteger();
        this.generationIdSource = 0; // XXX S this is wrong; should be
                                     // negotiated
        // XXX S no need this.portLock = new Object();
        this.portsByNumber = new ConcurrentHashMap<Integer, OFPortDesc>();
        this.portsByName = new ConcurrentHashMap<String, OFPortDesc>();
        this.connected = true;
        this.statsFutureMap = new ConcurrentHashMap<Integer, OFStatisticsFuture>();
        this.featuresFutureMap = new ConcurrentHashMap<Integer, OFFeaturesReplyFuture>();
        this.iofMsgListenersMap = new ConcurrentHashMap<Integer, IOFMessageListener>();
        this.role = null;
        this.listenerLock = new ReentrantReadWriteLock();
        this.pendingRoleRequests = new LinkedList<OFSwitchImplBase.PendingRoleRequestEntry>();
        this.portManager = new PortManager();
        // by default the base impl declares no support for Nx_role_requests.
        // OF1.0 switches like OVS that do support these messages should set the
        // attribute in the associated switch driver.
        setAttribute(SWITCH_SUPPORTS_NX_ROLE, false);

    }

    // *******************************************
    // Setters and Getters
    // *******************************************

    @Override
    public Object getAttribute(String name) {
        if (this.attributes.containsKey(name)) {
            return this.attributes.get(name);
        }
        return null;
    }

    @Override
    public ConcurrentMap<Object, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
        return;
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
        return this.attributes.containsKey(name);
    }

    @Override
    @JsonSerialize(using = DPIDSerializer.class)
    @JsonProperty("dpid")
    public long getId() {
        if (this.stringId == null)
            throw new RuntimeException("Features reply has not yet been set");
        return this.datapathId.getLong();
    }

    @JsonIgnore
    @Override
    public String getStringId() {
        return stringId;
    }

    @Override
    public OFFactory getFactory() {
        return OFFactories.getFactory(ofversion);
    }

    @Override
    public OFVersion getOFVersion() {
        return ofversion;
    }

    @Override
    public void setOFVersion(OFVersion ofv) {
        ofversion = ofv;
    }

    /**
     * @param floodlightProvider the floodlightProvider to set
     */
    @JsonIgnore
    @Override
    public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
        this.floodlightProvider = floodlightProvider;
    }

    @JsonIgnore
    @Override
    public void setThreadPoolService(IThreadPoolService tp) {
        this.threadPool = tp;
    }

    @Override
    @JsonIgnore
    public void setDebugCounterService(IDebugCounterService debugCounters)
            throws CounterException {
        this.debugCounters = debugCounters;
        registerOverloadCounters();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImpl [" + ((channel != null) ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((stringId != null) ? stringId : "?") + "]]";
    }

    // *******************************************
    // Channel related methods
    // *******************************************

    @JsonIgnore
    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void write(OFMessage m, FloodlightContext bc) throws IOException {
        Map<OFSwitchImplBase, List<OFMessage>> msg_buffer_map = local_msg_buffer.get();
        List<OFMessage> msg_buffer = msg_buffer_map.get(this);
        if (msg_buffer == null) {
            msg_buffer = new ArrayList<OFMessage>();
            msg_buffer_map.put(this, msg_buffer);
        }
        // XXX S will change when iFloodlight provider changes
        // this.floodlightProvider.handleOutgoingMessage(this, m, bc);
        msg_buffer.add(m);

        if ((msg_buffer.size() >= Controller.BATCH_MAX_SIZE) ||
                ((m.getType() != OFType.PACKET_OUT) && (m.getType() != OFType.FLOW_MOD))) {
            this.write(msg_buffer);
            msg_buffer.clear();
        }
    }

    @Override
    @LogMessageDoc(level = "WARN",
            message = "Sending OF message that modifies switch " +
                    "state while in the slave role: {switch}",
            explanation = "An application has sent a message to a switch " +
                    "that is not valid when the switch is in a slave role",
            recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG)
    public void write(List<OFMessage> msglist,
            FloodlightContext bc) throws IOException {
        for (OFMessage m : msglist) {
            if (role == Role.SLAVE) {
                switch (m.getType()) {
                case PACKET_OUT:
                case FLOW_MOD:
                case PORT_MOD:
                    log.warn("Sending OF message that modifies switch " +
                            "state while in the slave role: {}",
                            m.getType().name());
                    break;
                default:
                    break;
                }
            }
            // XXX S again
            // this.floodlightProvider.handleOutgoingMessage(this, m, bc);
        }
        this.write(msglist);
    }

    public void write(List<OFMessage> msglist) throws IOException {
        this.channel.write(msglist);
    }

    @Override
    public void disconnectSwitch() {
        channel.close();
    }

    @Override
    public Date getConnectedSince() {
        return connectedSince;
    }

    @JsonIgnore
    @Override
    public int getNextTransactionId() {
        return this.transactionIdSource.incrementAndGet();
    }

    @JsonIgnore
    @Override
    public synchronized boolean isConnected() {
        return connected;
    }

    @Override
    @JsonIgnore
    public synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    // *******************************************
    // Switch features related methods
    // *******************************************

    /**
     * Set the features reply for this switch from the handshake
     */
    protected void setFeaturesReply(OFFeaturesReply featuresReply) {
        if (featuresReply == null) {
            log.error("Error setting featuresReply for switch: {}", getStringId());
            return;
        }
        this.datapathId = featuresReply.getDatapathId();
        this.capabilities = featuresReply.getCapabilities();
        this.buffers = (int) featuresReply.getNBuffers();
        this.tables = (byte) featuresReply.getNTables();
        this.stringId = this.datapathId.toString();
        if (ofversion == OFVersion.OF_13) {
            auxId = featuresReply.getAuxiliaryId();
            if (!auxId.equals(OFAuxId.MAIN)) {
                log.warn("This controller does not handle auxiliary connections. "
                        + "Aux connection id {} received from switch {}",
                        auxId, getStringId());
            }
        }

        if (ofversion == OFVersion.OF_10) {
            this.actions = featuresReply.getActions();
            portManager.compareAndUpdatePorts(featuresReply.getPorts(), true);
        }
    }

    /**
     * Set the port descriptions for this switch from the handshake for an OF1.3
     * switch.
     */
    protected void setPortDescReply(OFPortDescStatsReply pdrep) {
        if (ofversion != OFVersion.OF_13)
            return;
        if (pdrep == null) {
            log.error("Error setting ports description for switch: {}", getStringId());
            return;
        }
        portManager.updatePorts(pdrep.getEntries());
    }

    @Override
    public int getNumBuffers() {
        return buffers;
    }

    @Override
    public Set<OFActionType> getActions() {
        return actions;
    }

    @Override
    public Set<OFCapabilities> getCapabilities() {
        return capabilities;
    }

    @Override
    public byte getNumTables() {
        return tables;
    }

    // public Future<OFFeaturesReply> getFeaturesReplyFromSwitch()
    // throws IOException {
    // // XXX S fix this later
    // OFMessage request = floodlightProvider.getOFMessageFactory_13()
    // .buildFeaturesRequest()
    // .setXid(getNextTransactionId())
    // .build();
    // OFFeaturesReplyFuture future =
    // new OFFeaturesReplyFuture(threadPool, this, (int) request.getXid());
    // this.featuresFutureMap.put((int) request.getXid(), future);
    // this.channel.write(Collections.singletonList(request));
    // return future;
    //
    // }
    //
    // public void deliverOFFeaturesReply(OFMessage reply) {
    // OFFeaturesReplyFuture future =
    // this.featuresFutureMap.get(reply.getXid());
    // if (future != null) {
    // future.deliverFuture(this, reply);
    // // The future will ultimately unregister itself and call
    // // cancelFeaturesReply
    // return;
    // }
    // log.error("Switch {}: received unexpected featureReply", this);
    // }

    @Override
    public void cancelFeaturesReply(int transactionId) {
        this.featuresFutureMap.remove(transactionId);
    }

    @JsonIgnore
    public void setSwitchDescription(OFDescStatsReply desc) {
        switchDescription = desc;
    }

    @Override
    @JsonIgnore
    public OFDescStatsReply getSwitchDescription() {
        return switchDescription;
    }

    // *******************************************
    // Switch port handling
    // *******************************************

    @Override
    @JsonIgnore
    public Collection<OFPortDesc> getEnabledPorts() {
        return portManager.getEnabledPorts();
    }

    @Override
    @JsonIgnore
    public Collection<Integer> getEnabledPortNumbers() {
        return portManager.getEnabledPortNumbers();
    }

    @Override
    public OFPortDesc getPort(int portNumber) {
        return portManager.getPort(portNumber);
    }

    @Override
    public OFPortDesc getPort(String portName) {
        return portManager.getPort(portName);
    }

    @Override
    @JsonIgnore
    public OrderedCollection<PortChangeEvent> processOFPortStatus(OFPortStatus ps) {
        return portManager.handlePortStatusMessage(ps);
    }

    @Override
    @JsonProperty("ports")
    public Collection<OFPortDesc> getPorts() {
        return portManager.getPorts();
    }

    @Override
    public boolean portEnabled(int portNumber) {
        return isEnabled(portManager.getPort(portNumber));
    }

    @Override
    public boolean portEnabled(String portName) {
        return isEnabled(portManager.getPort(portName));
    }

    private boolean isEnabled(OFPortDesc p) {
        return (p != null &&
                !p.getState().contains(OFPortState.LINK_DOWN) &&
                !p.getState().contains(OFPortState.BLOCKED) && !p.getConfig().contains(
                OFPortConfig.PORT_DOWN));
    }

    @Override
    public OrderedCollection<PortChangeEvent> comparePorts(Collection<OFPortDesc> ports) {
        return portManager.comparePorts(ports);
    }

    @Override
    @JsonIgnore
    public OrderedCollection<PortChangeEvent> setPorts(Collection<OFPortDesc> ports) {
        return portManager.updatePorts(ports);
    }

    /**
     * Manages the ports of this switch.
     *
     * Provides methods to query and update the stored ports. The class ensures
     * that every port name and port number is unique. When updating ports the
     * class checks if port number <-> port name mappings have change due to the
     * update. If a new port P has number and port that are inconsistent with
     * the previous mapping(s) the class will delete all previous ports with
     * name or number of the new port and then add the new port.
     *
     * Port names are stored as-is but they are compared case-insensitive
     *
     * The methods that change the stored ports return a list of
     * PortChangeEvents that represent the changes that have been applied to the
     * port list so that IOFSwitchListeners can be notified about the changes.
     *
     * Implementation notes: - We keep several different representations of the
     * ports to allow for fast lookups - Ports are stored in unchangeable lists.
     * When a port is modified new data structures are allocated. - We use a
     * read-write-lock for synchronization, so multiple readers are allowed. -
     * All port numbers have int representation (no more shorts)
     */
    protected class PortManager {
        private final ReentrantReadWriteLock lock;
        private List<OFPortDesc> portList;
        private List<OFPortDesc> enabledPortList;
        private List<Integer> enabledPortNumbers;
        private Map<Integer, OFPortDesc> portsByNumber;
        private Map<String, OFPortDesc> portsByName;

        public PortManager() {
            this.lock = new ReentrantReadWriteLock();
            this.portList = Collections.emptyList();
            this.enabledPortList = Collections.emptyList();
            this.enabledPortNumbers = Collections.emptyList();
            this.portsByName = Collections.emptyMap();
            this.portsByNumber = Collections.emptyMap();
        }

        /**
         * Set the internal data structure storing this switch's port to the
         * ports specified by newPortsByNumber
         *
         * CALLER MUST HOLD WRITELOCK
         *
         * @param newPortsByNumber
         * @throws IllegaalStateException if called without holding the
         *         writelock
         */
        private void updatePortsWithNewPortsByNumber(
                Map<Integer, OFPortDesc> newPortsByNumber) {
            if (!lock.writeLock().isHeldByCurrentThread()) {
                throw new IllegalStateException("Method called without " +
                        "holding writeLock");
            }
            Map<String, OFPortDesc> newPortsByName =
                    new HashMap<String, OFPortDesc>();
            List<OFPortDesc> newPortList =
                    new ArrayList<OFPortDesc>();
            List<OFPortDesc> newEnabledPortList =
                    new ArrayList<OFPortDesc>();
            List<Integer> newEnabledPortNumbers = new ArrayList<Integer>();

            for (OFPortDesc p : newPortsByNumber.values()) {
                newPortList.add(p);
                newPortsByName.put(p.getName().toLowerCase(), p);
                if (isEnabled(p)) {
                    newEnabledPortList.add(p);
                    newEnabledPortNumbers.add(p.getPortNo().getPortNumber());
                }
            }
            portsByName = Collections.unmodifiableMap(newPortsByName);
            portsByNumber =
                    Collections.unmodifiableMap(newPortsByNumber);
            enabledPortList =
                    Collections.unmodifiableList(newEnabledPortList);
            enabledPortNumbers =
                    Collections.unmodifiableList(newEnabledPortNumbers);
            portList = Collections.unmodifiableList(newPortList);
        }

        /**
         * Handle a OFPortStatus delete message for the given port. Updates the
         * internal port maps/lists of this switch and returns the
         * PortChangeEvents caused by the delete. If the given port exists as
         * it, it will be deleted. If the name<->number for the given port is
         * inconsistent with the ports stored by this switch the method will
         * delete all ports with the number or name of the given port.
         *
         * This method will increment error/warn counters and log
         *
         * @param delPort the port from the port status message that should be
         *        deleted.
         * @return ordered collection of port changes applied to this switch
         */
        private OrderedCollection<PortChangeEvent> handlePortStatusDelete(
                OFPortDesc delPort) {
            lock.writeLock().lock();
            OrderedCollection<PortChangeEvent> events =
                    new LinkedHashSetWrapper<PortChangeEvent>();
            try {
                Map<Integer, OFPortDesc> newPortByNumber =
                        new HashMap<Integer, OFPortDesc>(portsByNumber);
                OFPortDesc prevPort =
                        portsByNumber.get(delPort.getPortNo().getPortNumber());
                if (prevPort == null) {
                    // so such port. Do we have a port with the name?
                    prevPort = portsByName.get(delPort.getName());
                    if (prevPort != null) {
                        newPortByNumber.remove(prevPort.getPortNo().getPortNumber());
                        events.add(new PortChangeEvent(prevPort,
                                PortChangeType.DELETE));
                    }
                } else if (prevPort.getName().equals(delPort.getName())) {
                    // port exists with consistent name-number mapping
                    newPortByNumber.remove(delPort.getPortNo().getPortNumber());
                    events.add(new PortChangeEvent(delPort,
                            PortChangeType.DELETE));
                } else {
                    // port with same number exists but its name differs. This
                    // is weird. The best we can do is to delete the existing
                    // port(s) that have delPort's name and number.
                    newPortByNumber.remove(delPort.getPortNo().getPortNumber());
                    events.add(new PortChangeEvent(prevPort,
                            PortChangeType.DELETE));
                    // is there another port that has delPort's name?
                    prevPort = portsByName.get(delPort.getName().toLowerCase());
                    if (prevPort != null) {
                        newPortByNumber.remove(prevPort.getPortNo().getPortNumber());
                        events.add(new PortChangeEvent(prevPort,
                                PortChangeType.DELETE));
                    }
                }
                updatePortsWithNewPortsByNumber(newPortByNumber);
                return events;
            } finally {
                lock.writeLock().unlock();
            }
        }

        /**
         * Handle a OFPortStatus message, update the internal data structures
         * that store ports and return the list of OFChangeEvents.
         *
         * This method will increment error/warn counters and log
         *
         * @param ps
         * @return
         */
        public OrderedCollection<PortChangeEvent> handlePortStatusMessage(OFPortStatus ps) {
            if (ps == null) {
                throw new NullPointerException("OFPortStatus message must " +
                        "not be null");
            }
            lock.writeLock().lock();
            try {
                OFPortReason reason = ps.getReason();
                if (reason == null) {
                    throw new IllegalArgumentException("Unknown PortStatus " +
                            "reason code " + ps.getReason());
                }

                if (log.isDebugEnabled()) {
                    log.debug("Handling OFPortStatus: {} for {}",
                            reason, ps);
                }

                if (reason == OFPortReason.DELETE)
                    return handlePortStatusDelete(ps.getDesc());

                // We handle ADD and MODIFY the same way. Since OpenFlow
                // doesn't specify what uniquely identifies a port the
                // notion of ADD vs. MODIFY can also be hazy. So we just
                // compare the new port to the existing ones.
                Map<Integer, OFPortDesc> newPortByNumber =
                        new HashMap<Integer, OFPortDesc>(portsByNumber);
                OrderedCollection<PortChangeEvent> events =
                        getSinglePortChanges(ps.getDesc());
                for (PortChangeEvent e : events) {
                    switch (e.type) {
                    case DELETE:
                        newPortByNumber.remove(e.port.getPortNo().getPortNumber());
                        break;
                    case ADD:
                        if (reason != OFPortReason.ADD) {
                            // weird case
                        }
                        newPortByNumber.put(e.port.getPortNo().getPortNumber(),
                                e.port);
                        break;
                    case DOWN:
                        newPortByNumber.put(e.port.getPortNo().getPortNumber(),
                                e.port);
                        break;
                    case OTHER_UPDATE:
                        newPortByNumber.put(e.port.getPortNo().getPortNumber(),
                                e.port);
                        break;
                    case UP:
                        newPortByNumber.put(e.port.getPortNo().getPortNumber(),
                                e.port);
                        break;
                    }
                }
                updatePortsWithNewPortsByNumber(newPortByNumber);
                return events;
            } finally {
                lock.writeLock().unlock();
            }

        }

        /**
         * Given a new or modified port newPort, returns the list of
         * PortChangeEvents to "transform" the current ports stored by this
         * switch to include / represent the new port. The ports stored by this
         * switch are <b>NOT</b> updated.
         *
         * This method acquires the readlock and is thread-safe by itself. Most
         * callers will need to acquire the write lock before calling this
         * method though (if the caller wants to update the ports stored by this
         * switch)
         *
         * @param newPort the new or modified port.
         * @return the list of changes
         */
        public OrderedCollection<PortChangeEvent> getSinglePortChanges(
                OFPortDesc newPort) {
            lock.readLock().lock();
            try {
                OrderedCollection<PortChangeEvent> events =
                        new LinkedHashSetWrapper<PortChangeEvent>();
                // Check if we have a port by the same number in our
                // old map.
                OFPortDesc prevPort =
                        portsByNumber.get(newPort.getPortNo().getPortNumber());
                if (newPort.equals(prevPort)) {
                    // nothing has changed
                    return events;
                }

                if (prevPort != null &&
                        prevPort.getName().equals(newPort.getName())) {
                    // A simple modify of a existing port
                    // A previous port with this number exists and it's name
                    // also matches the new port. Find the differences
                    if (isEnabled(prevPort) && !isEnabled(newPort)) {
                        events.add(new PortChangeEvent(newPort,
                                PortChangeType.DOWN));
                    } else if (!isEnabled(prevPort) && isEnabled(newPort)) {
                        events.add(new PortChangeEvent(newPort,
                                PortChangeType.UP));
                    } else {
                        events.add(new PortChangeEvent(newPort,
                                PortChangeType.OTHER_UPDATE));
                    }
                    return events;
                }

                if (prevPort != null) {
                    // There exists a previous port with the same port
                    // number but the port name is different (otherwise we would
                    // never have gotten here)
                    // Remove the port. Name-number mapping(s) have changed
                    events.add(new PortChangeEvent(prevPort,
                            PortChangeType.DELETE));
                }

                // We now need to check if there exists a previous port sharing
                // the same name as the new/updated port.
                prevPort = portsByName.get(newPort.getName().toLowerCase());
                if (prevPort != null) {
                    // There exists a previous port with the same port
                    // name but the port number is different (otherwise we
                    // never have gotten here).
                    // Remove the port. Name-number mapping(s) have changed
                    events.add(new PortChangeEvent(prevPort,
                            PortChangeType.DELETE));
                }

                // We always need to add the new port. Either no previous port
                // existed or we just deleted previous ports with inconsistent
                // name-number mappings
                events.add(new PortChangeEvent(newPort, PortChangeType.ADD));
                return events;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Compare the current ports of this switch to the newPorts list and
         * return the changes that would be applied to transfort the current
         * ports to the new ports. No internal data structures are updated see
         * {@link #compareAndUpdatePorts(List, boolean)}
         *
         * @param newPorts the list of new ports
         * @return The list of differences between the current ports and
         *         newPortList
         */
        public OrderedCollection<PortChangeEvent> comparePorts(
                Collection<OFPortDesc> newPorts) {
            return compareAndUpdatePorts(newPorts, false);
        }

        /**
         * Compare the current ports of this switch to the newPorts list and
         * return the changes that would be applied to transform the current
         * ports to the new ports. No internal data structures are updated see
         * {@link #compareAndUpdatePorts(List, boolean)}
         *
         * @param newPorts the list of new ports
         * @return The list of differences between the current ports and
         *         newPortList
         */
        public OrderedCollection<PortChangeEvent> updatePorts(
                Collection<OFPortDesc> newPorts) {
            return compareAndUpdatePorts(newPorts, true);
        }

        /**
         * Compare the current ports stored in this switch instance with the new
         * port list given and return the differences in the form of
         * PortChangeEvents. If the doUpdate flag is true, newPortList will
         * replace the current list of this switch (and update the port maps)
         *
         * Implementation note: Since this method can optionally modify the
         * current ports and since it's not possible to upgrade a read-lock to a
         * write-lock we need to hold the write-lock for the entire operation.
         * If this becomes a problem and if compares() are common we can
         * consider splitting in two methods but this requires lots of code
         * duplication
         *
         * @param newPorts the list of new ports.
         * @param doUpdate If true the newPortList will replace the current port
         *        list for this switch. If false this switch will not be
         *        changed.
         * @return The list of differences between the current ports and
         *         newPorts
         * @throws NullPointerException if newPortsList is null
         * @throws IllegalArgumentException if either port names or port numbers
         *         are duplicated in the newPortsList.
         */
        private OrderedCollection<PortChangeEvent> compareAndUpdatePorts(
                Collection<OFPortDesc> newPorts, boolean doUpdate) {
            if (newPorts == null) {
                throw new NullPointerException("newPortsList must not be null");
            }
            lock.writeLock().lock();
            try {
                OrderedCollection<PortChangeEvent> events =
                        new LinkedHashSetWrapper<PortChangeEvent>();

                Map<Integer, OFPortDesc> newPortsByNumber =
                        new HashMap<Integer, OFPortDesc>();
                Map<String, OFPortDesc> newPortsByName =
                        new HashMap<String, OFPortDesc>();
                List<OFPortDesc> newEnabledPortList =
                        new ArrayList<OFPortDesc>();
                List<Integer> newEnabledPortNumbers =
                        new ArrayList<Integer>();
                List<OFPortDesc> newPortsList =
                        new ArrayList<OFPortDesc>(newPorts);

                for (OFPortDesc p : newPortsList) {
                    if (p == null) {
                        throw new NullPointerException("portList must not " +
                                "contain null values");
                    }

                    // Add the port to the new maps and lists and check
                    // that every port is unique
                    OFPortDesc duplicatePort;
                    duplicatePort = newPortsByNumber.put(
                            p.getPortNo().getPortNumber(), p);
                    if (duplicatePort != null) {
                        String msg = String.format("Cannot have two ports " +
                                "with the same number: %s <-> %s",
                                p, duplicatePort);
                        throw new IllegalArgumentException(msg);
                    }
                    duplicatePort =
                            newPortsByName.put(p.getName().toLowerCase(), p);
                    if (duplicatePort != null) {
                        String msg = String.format("Cannot have two ports " +
                                "with the same name: %s <-> %s",
                                p.toString().substring(0, 80),
                                duplicatePort.toString().substring(0, 80));
                        throw new IllegalArgumentException(msg);
                    }
                    if (isEnabled(p)) {
                        newEnabledPortList.add(p);
                        newEnabledPortNumbers.add(p.getPortNo().getPortNumber());
                    }

                    // get changes
                    events.addAll(getSinglePortChanges(p));
                }
                // find deleted ports
                // We need to do this after looping through all the new ports
                // to we can handle changed name<->number mappings correctly
                // We could pull it into the loop of we address this but
                // it's probably not worth it
                for (OFPortDesc oldPort : this.portList) {
                    if (!newPortsByNumber
                            .containsKey(oldPort.getPortNo().getPortNumber())) {
                        PortChangeEvent ev =
                                new PortChangeEvent(oldPort,
                                        PortChangeType.DELETE);
                        events.add(ev);
                    }
                }

                if (doUpdate) {
                    portsByName = Collections.unmodifiableMap(newPortsByName);
                    portsByNumber =
                            Collections.unmodifiableMap(newPortsByNumber);
                    enabledPortList =
                            Collections.unmodifiableList(newEnabledPortList);
                    enabledPortNumbers =
                            Collections.unmodifiableList(newEnabledPortNumbers);
                    portList = Collections.unmodifiableList(newPortsList);
                }
                return events;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public OFPortDesc getPort(String name) {
            if (name == null) {
                throw new NullPointerException("Port name must not be null");
            }
            lock.readLock().lock();
            try {
                return portsByName.get(name.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
        }

        public OFPortDesc getPort(int portNumber) {
            lock.readLock().lock();
            try {
                return portsByNumber.get(portNumber);
            } finally {
                lock.readLock().unlock();
            }
        }

        public List<OFPortDesc> getPorts() {
            lock.readLock().lock();
            try {
                return portList;
            } finally {
                lock.readLock().unlock();
            }
        }

        public List<OFPortDesc> getEnabledPorts() {
            lock.readLock().lock();
            try {
                return enabledPortList;
            } finally {
                lock.readLock().unlock();
            }
        }

        public List<Integer> getEnabledPortNumbers() {
            lock.readLock().lock();
            try {
                return enabledPortNumbers;
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    // *******************************************
    // Switch stats handling
    // *******************************************

    @Override
    public Future<List<OFStatsReply>> getStatistics(OFStatsRequest<?> request)
            throws IOException {
        OFStatisticsFuture future = new OFStatisticsFuture(threadPool, this,
                (int) request.getXid());
        log.info("description STAT REQUEST XID {}", request.getXid());
        this.statsFutureMap.put((int) request.getXid(), future);

        this.channel.write(Collections.singletonList(request));
        return future;
    }

    @Override
    public void deliverStatisticsReply(OFMessage reply) {
        OFStatisticsFuture future = this.statsFutureMap.get((int) reply.getXid());
        if (future != null) {
            future.deliverFuture(this, reply);
            // The future will ultimately unregister itself and call
            // cancelStatisticsReply
            return;
        }
        // Transaction id was not found in statsFutureMap.check the other map
        IOFMessageListener caller = this.iofMsgListenersMap.get(reply.getXid());
        if (caller != null) {
            caller.receive(this, reply, null);
        }
    }

    @Override
    public void cancelStatisticsReply(int transactionId) {
        if (null == this.statsFutureMap.remove(transactionId)) {
            this.iofMsgListenersMap.remove(transactionId);
        }
    }

    @Override
    public void cancelAllStatisticsReplies() {
        /* we don't need to be synchronized here. Even if another thread
         * modifies the map while we're cleaning up the future will eventuall
         * timeout */
        for (OFStatisticsFuture f : statsFutureMap.values()) {
            f.cancel(true);
        }
        statsFutureMap.clear();
        iofMsgListenersMap.clear();
    }

    // *******************************************
    // Switch role handling
    // *******************************************

    // XXX S this is completely wrong. The generation id should be obtained
    // after coordinating with all controllers connected to this switch.
    // ie. should be part of registry service (account for 1.3 vs 1.0)
    // For now we are just generating this locally and keeping it constant.
    public U64 getNextGenerationId() {
        // TODO: Pankaj, fix nextGenerationId as part of Registry interface
        return U64.of(generationIdSource);
    }

    @Override
    public Role getRole() {
        return role;
    }

    @JsonIgnore
    @Override
    public void setRole(Role role) {
        this.role = role;
    }

    // *******************************************
    // Switch utility methods
    // *******************************************

    private void registerOverloadCounters() throws CounterException {
        if (debugCountersRegistered) {
            return;
        }
        if (debugCounters == null) {
            log.error("Debug Counter Service not found");
            debugCounters = new NullDebugCounter();
            debugCountersRegistered = true;
        }
        // every level of the hierarchical counter has to be registered
        // even if they are not used
        ctrSwitch = debugCounters.registerCounter(
                BASE, stringId,
                "Counter for this switch",
                CounterType.ALWAYS_COUNT);
        ctrSwitchPktin = debugCounters.registerCounter(
                BASE, stringId + "/pktin",
                "Packet in counter for this switch",
                CounterType.ALWAYS_COUNT);
        ctrSwitchWrite = debugCounters.registerCounter(
                BASE, stringId + "/write",
                "Write counter for this switch",
                CounterType.ALWAYS_COUNT);
        ctrSwitchPktinDrops = debugCounters.registerCounter(
                BASE, stringId + "/pktin/drops",
                "Packet in throttle drop count",
                CounterType.ALWAYS_COUNT,
                IDebugCounterService.CTR_MDATA_WARN);
        ctrSwitchWriteDrops = debugCounters.registerCounter(
                BASE, stringId + "/write/drops",
                "Switch write throttle drop count",
                CounterType.ALWAYS_COUNT,
                IDebugCounterService.CTR_MDATA_WARN);
    }

    @Override
    public void startDriverHandshake() throws IOException {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled)
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        startDriverHandshakeCalled = true;
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled)
            throw new SwitchDriverSubHandshakeNotStarted();
        return true;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (startDriverHandshakeCalled)
            throw new SwitchDriverSubHandshakeCompleted(m);
        else
            throw new SwitchDriverSubHandshakeNotStarted();
    }

    @Override
    @JsonIgnore
    @LogMessageDoc(level = "WARN",
            message = "Switch {switch} flow table is full",
            explanation = "The controller received flow table full " +
                    "message from the switch, could be caused by increased " +
                    "traffic pattern",
            recommendation = LogMessageDoc.REPORT_CONTROLLER_BUG)
    public void setTableFull(boolean isFull) {
        if (isFull && !flowTableFull) {
            floodlightProvider.addSwitchEvent(this.datapathId.getLong(),
                    "SWITCH_FLOW_TABLE_FULL " +
                            "Table full error from switch", false);
            log.warn("Switch {} flow table is full", stringId);
        }
        flowTableFull = isFull;
    }

    @Override
    @LogMessageDoc(level = "ERROR",
            message = "Failed to clear all flows on switch {switch}",
            explanation = "An I/O error occured while trying to clear " +
                    "flows on the switch.",
            recommendation = LogMessageDoc.CHECK_SWITCH)
    public void clearAllFlowMods() {
        // Delete all pre-existing flows

        // by default if match is not specified, then an empty list of matches
        // is sent, resulting in a wildcard-all flows
        Builder builder = getFactory()
                .buildFlowDelete()
                .setOutPort(OFPort.ANY);

        if (ofversion.wireVersion >= OFVersion.OF_13.wireVersion) {
            builder.setOutGroup(OFGroup.ANY)
                   .setTableId(TableId.ALL);
        }

        OFMessage fm = builder.build();

        try {
            channel.write(Collections.singletonList(fm));
        } catch (Exception e) {
            log.error("Failed to clear all flows on switch " + this, e);
        }
    }

    @Override
    public void flush() {
        Map<OFSwitchImplBase, List<OFMessage>> msg_buffer_map = local_msg_buffer.get();
        List<OFMessage> msglist = msg_buffer_map.get(this);
        if ((msglist != null) && (msglist.size() > 0)) {
            try {
                this.write(msglist);
            } catch (IOException e) {
                log.error("Failed flushing messages", e);
            }
            msglist.clear();
        }
    }

    public static void flush_all() {
        Map<OFSwitchImplBase, List<OFMessage>> msg_buffer_map = local_msg_buffer.get();
        for (OFSwitchImplBase sw : msg_buffer_map.keySet()) {
            sw.flush();
        }
    }

    /**
     * Return a read lock that must be held while calling the listeners for
     * messages from the switch. Holding the read lock prevents the active
     * switch list from being modified out from under the listeners.
     *
     * @return listener read lock
     */
    @JsonIgnore
    public Lock getListenerReadLock() {
        return listenerLock.readLock();
    }

    /**
     * Return a write lock that must be held when the controllers modifies the
     * list of active switches. This is to ensure that the active switch list
     * doesn't change out from under the listeners as they are handling a
     * message from the switch.
     *
     * @return listener write lock
     */
    @JsonIgnore
    public Lock getListenerWriteLock() {
        return listenerLock.writeLock();
    }

    public String getSwitchDriverState() {
        return "";
    }

}
