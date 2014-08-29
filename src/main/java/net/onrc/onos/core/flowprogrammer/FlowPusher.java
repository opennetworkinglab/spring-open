package net.onrc.onos.core.flowprogrammer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFMessageFuture;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.FlowEntry;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations.Operator;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.ModifySrcMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * FlowPusher is a implementation of FlowPusherService. FlowPusher assigns one
 * message queue instance for each one switch. Number of message processing
 * threads is configurable by constructor, and one thread can handle multiple
 * message queues. Each queue will be assigned to a thread according to hash
 * function defined by getHash(). Each processing thread reads messages from
 * queues and sends it to switches in round-robin. Processing thread also
 * calculates rate of sending to suppress excessive message sending.
 */
public final class FlowPusher implements IFlowPusherService, IOFMessageListener {
    private static final Logger log = LoggerFactory.getLogger(FlowPusher.class);
    private static final int DEFAULT_NUMBER_THREAD = 1;

    // Number of messages sent to switch at once
    protected static final int MAX_MESSAGE_SEND = 100;

    private FloodlightModuleContext context = null;
    private IThreadPoolService threadPool = null;
    private IFloodlightProviderService floodlightProvider = null;

    // Map of threads versus dpid
    private Map<Long, FlowPusherThread> threadMap = null;
    // Map from (DPID and transaction ID) to Future objects.
    private Map<BarrierInfo, OFBarrierReplyFuture> barrierFutures =
            new ConcurrentHashMap<BarrierInfo, OFBarrierReplyFuture>();

    private int numberThread;

    private static class SwitchQueueEntry {
        OFMessage msg;

        public SwitchQueueEntry(OFMessage msg) {
            this.msg = msg;
        }

        public OFMessage getOFMessage() {
            return msg;
        }
    }

    /**
     * SwitchQueue represents message queue attached to a switch. This consists
     * of queue itself and variables used for limiting sending rate.
     */
    private static class SwitchQueue {
        List<Queue<SwitchQueueEntry>> rawQueues;
        QueueState state;

        // Max rate of sending message (bytes/ms). 0 implies no limitation.
        long maxRate = 0; // 0 indicates no limitation
        long lastSentTime = 0;
        long lastSentSize = 0;

        // "To be deleted" flag
        boolean toBeDeleted = false;

        SwitchQueue() {
            rawQueues = new ArrayList<>(MsgPriority.values().length);
            for (int i = 0; i < MsgPriority.values().length; ++i) {
                rawQueues.add(i, new ArrayDeque<SwitchQueueEntry>());
            }

            state = QueueState.READY;
        }

        /**
         * Check if sending rate is within the rate.
         * <p>
         * @param current Current time
         * @return true if within the rate
         */
        boolean isSendable(long current) {
            if (maxRate == 0) {
                // no limitation
                return true;
            }

            if (current == lastSentTime) {
                return false;
            }

            // Check if sufficient time (from aspect of rate) elapsed or not.
            long rate = lastSentSize / (current - lastSentTime);
            return (rate < maxRate);
        }

        /**
         * Log time and size of last sent data.
         * <p>
         * @param current Time to be sent.
         * @param size Size of sent data (in bytes).
         */
        void logSentData(long current, long size) {
            lastSentTime = current;
            lastSentSize = size;
        }

        boolean add(SwitchQueueEntry entry, MsgPriority priority) {
            Queue<SwitchQueueEntry> queue = getQueue(priority);
            if (queue == null) {
                log.error("Unexpected priority: {}", priority);
                return false;
            }
            return queue.add(entry);
        }

        /**
         * Poll single appropriate entry object according to QueueState.
         * <p>
         * @return Entry object.
         */
        SwitchQueueEntry poll() {
            switch (state) {
            case READY: {
                for (int i = 0; i < rawQueues.size(); ++i) {
                    SwitchQueueEntry entry = rawQueues.get(i).poll();
                    if (entry != null) {
                        return entry;
                    }
                }

                return null;
            }
            case SUSPENDED: {
                // Only polling from high priority queue
                SwitchQueueEntry entry = getQueue(MsgPriority.HIGH).poll();
                return entry;
            }
            default:
                log.error("Unexpected QueueState: {}", state);
                return null;
            }
        }

        /**
         * Check if this object has any messages in the queues to be sent.
         * <p>
         * @return True if there are some messages to be sent.
         */
        boolean hasMessageToSend() {
            switch (state) {
            case READY:
                for (Queue<SwitchQueueEntry> queue : rawQueues) {
                    if (!queue.isEmpty()) {
                        return true;
                    }
                }
                break;
            case SUSPENDED:
                // Only checking high priority queue
                return (!getQueue(MsgPriority.HIGH).isEmpty());
            default:
                log.error("Unexpected QueueState: {}", state);
                return false;
            }

            return false;
        }

        Queue<SwitchQueueEntry> getQueue(MsgPriority priority) {
            return rawQueues.get(priority.ordinal());
        }
    }

    /**
     * BarrierInfo holds information to specify barrier message sent to switch.
     */
    private static final class BarrierInfo {
        final long dpid;
        final long xid;

        static BarrierInfo create(long dpid, OFBarrierRequest req) {
            return new BarrierInfo(dpid, req.getXid());
        }

        static BarrierInfo create(long dpid, OFBarrierReply rpy) {
            return new BarrierInfo(dpid, rpy.getXid());
        }

        private BarrierInfo(long dpid, long xid) {
            this.dpid = dpid;
            this.xid = xid;
        }

        // Auto generated code by Eclipse
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (dpid ^ (dpid >>> 32));
            result = prime * result + (int) (xid ^ (xid >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            BarrierInfo other = (BarrierInfo) obj;
            return (this.dpid == other.dpid) && (this.xid == other.xid);
        }

    }

    /**
     * Main thread that reads messages from queues and sends them to switches.
     */
    private class FlowPusherThread extends Thread {
        private Map<Dpid, SwitchQueue> assignedQueues = new ConcurrentHashMap<>();

        final Lock queuingLock = new ReentrantLock();
        final Condition messagePushed = queuingLock.newCondition();

        @Override
        public void run() {
            this.setName("FlowPusherThread " + this.getId());
            while (true) {
                while (!queuesHasMessageToSend()) {
                    queuingLock.lock();

                    try {
                        // wait for message pushed to queue
                        messagePushed.await();
                    } catch (InterruptedException e) {
                        // Interrupted to be shut down (not an error)
                        log.debug("FlowPusherThread is interrupted");
                        return;
                    } finally {
                        queuingLock.unlock();
                    }
                }

                for (Iterator<Entry<Dpid, SwitchQueue>> it = assignedQueues
                        .entrySet().iterator(); it.hasNext();) {
                    Entry<Dpid, SwitchQueue> entry = it.next();
                    SwitchQueue queue = entry.getValue();

                    if (queue == null) {
                        continue;
                    }

                    synchronized (queue) {
                        processQueue(entry.getKey(), queue, MAX_MESSAGE_SEND);
                        if (queue.toBeDeleted && !queue.hasMessageToSend()) {
                            // remove queue if flagged to be.
                            it.remove();
                        }
                    }
                }
            }
        }

        /**
         * Read messages from queue and send them to the switch. If number of
         * messages excess the limit, stop sending messages.
         * <p>
         * @param dpid DPID of the switch to which messages will be sent.
         * @param queue Queue of messages.
         * @param maxMsg Limitation of number of messages to be sent. If set to
         *        0, all messages in queue will be sent.
         */
        private void processQueue(Dpid dpid, SwitchQueue queue, int maxMsg) {
            // check sending rate and determine it to be sent or not
            long currentTime = System.currentTimeMillis();
            long size = 0;

            IOFSwitch sw = floodlightProvider.getMasterSwitch(dpid.value());
            if (sw == null) {
                // FlowPusher state for this switch will get cleaned up soon
                // due to the switchDisconnected event
                log.debug("Switch {} not found when processing queue", dpid);
                return;
            }

            if (sw.isConnected() && queue.isSendable(currentTime)) {
                int i = 0;
                while (queue.hasMessageToSend()) {
                    // Number of messages excess the limit
                    if (0 < maxMsg && maxMsg <= i) {
                        break;
                    }
                    ++i;

                    SwitchQueueEntry queueEntry;
                    synchronized (queue) {
                        queueEntry = queue.poll();
                    }

                    OFMessage msg = queueEntry.getOFMessage();
                    try {
                        sw.write(msg, null);
                        if (log.isTraceEnabled()) {
                            log.trace("Pusher sends message to switch {}: {}", sw.getStringId(), msg);
                        }
                        // TODO BOC how do we get the size?
                        // size += msg.getLength();
                    } catch (IOException e) {
                        log.error("Exception in sending message (" + msg + "):", e);
                    }
                }

                sw.flush();
                queue.logSentData(currentTime, size);
            }
        }

        private boolean queuesHasMessageToSend() {
            for (SwitchQueue queue : assignedQueues.values()) {
                if (queue.hasMessageToSend()) {
                    return true;
                }
            }

            return false;
        }

        private void notifyMessagePushed() {
            queuingLock.lock();
            try {
                messagePushed.signal();
            } finally {
                queuingLock.unlock();
            }
        }
    }

    /**
     * Initialize object with one thread.
     */
    public FlowPusher() {
        numberThread = DEFAULT_NUMBER_THREAD;
    }

    /**
     * Initialize object with threads of given number.
     * <p>
     * @param numberThreadValue Number of threads to handle messages.
     */
    public FlowPusher(int numberThreadValue) {
        if (numberThreadValue > 0) {
            numberThread = numberThreadValue;
        } else {
            numberThread = DEFAULT_NUMBER_THREAD;
        }
    }

    /**
     * Set parameters needed for sending messages.
     * <p>
     * @param floodlightContext FloodlightModuleContext used for acquiring
     *        ThreadPoolService and registering MessageListener.
     */
    public void init(FloodlightModuleContext floodlightContext) {
        this.context = floodlightContext;
        this.floodlightProvider = context
                .getServiceImpl(IFloodlightProviderService.class);
        this.threadPool = context.getServiceImpl(IThreadPoolService.class);

        floodlightProvider.addOFMessageListener(OFType.BARRIER_REPLY, this);
    }

    /**
     * Begin processing queue.
     */
    public void start() {
        threadMap = new HashMap<>();
        for (long i = 0; i < numberThread; ++i) {
            FlowPusherThread thread = new FlowPusherThread();

            threadMap.put(i, thread);
            thread.start();
        }
    }

    @Override
    public boolean suspend(Dpid dpid) {
        SwitchQueue queue = getQueue(dpid);

        if (queue == null) {
            // create queue in case suspend is called before first message
            // addition
            queue = createQueueImpl(dpid);
        }

        synchronized (queue) {
            if (queue.state == QueueState.READY) {
                queue.state = QueueState.SUSPENDED;
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean resume(Dpid dpid) {
        SwitchQueue queue = getQueue(dpid);

        if (queue == null) {
            log.error("No queue is attached to DPID: {}", dpid);
            return false;
        }

        synchronized (queue) {
            if (queue.state == QueueState.SUSPENDED) {
                queue.state = QueueState.READY;

                // Free the latch if queue has any messages
                FlowPusherThread thread = getProcessingThread(dpid);
                if (queue.hasMessageToSend()) {
                    thread.notifyMessagePushed();
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public QueueState getState(Dpid dpid) {
        SwitchQueue queue = getQueue(dpid);

        if (queue == null) {
            return QueueState.UNKNOWN;
        }

        return queue.state;
    }

    /**
     * Stop processing queue and exit thread.
     */
    public void stop() {
        if (threadMap == null) {
            return;
        }

        for (FlowPusherThread t : threadMap.values()) {
            t.interrupt();
        }
    }

    @Override
    public void setRate(Dpid dpid, long rate) {
        SwitchQueue queue = getQueue(dpid);
        if (queue == null) {
            queue = createQueueImpl(dpid);
        }

        if (rate > 0) {
            log.debug("rate for {} is set to {}", dpid, rate);
            synchronized (queue) {
                queue.maxRate = rate;
            }
        }
    }

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "Future versions of createQueueImpl() might return null")
    public boolean createQueue(Dpid dpid) {
        SwitchQueue queue = createQueueImpl(dpid);

        return (queue != null);
    }

    protected SwitchQueue createQueueImpl(Dpid dpid) {
        SwitchQueue queue = getQueue(dpid);
        if (queue != null) {
            return queue;
        }

        FlowPusherThread proc = getProcessingThread(dpid);
        queue = new SwitchQueue();
        queue.state = QueueState.READY;
        proc.assignedQueues.put(dpid, queue);

        return queue;
    }

    @Override
    public boolean deleteQueue(Dpid dpid) {
        return deleteQueue(dpid, false);
    }

    @Override
    public boolean deleteQueue(Dpid dpid, boolean forceStop) {
        FlowPusherThread proc = getProcessingThread(dpid);

        if (forceStop) {
            SwitchQueue queue = proc.assignedQueues.remove(dpid);
            if (queue == null) {
                return false;
            }
            return true;
        } else {
            SwitchQueue queue = getQueue(dpid);
            if (queue == null) {
                return false;
            }
            synchronized (queue) {
                queue.toBeDeleted = true;
            }
            return true;
        }
    }

    @Override
    public boolean add(Dpid dpid, OFMessage msg) {
        return add(dpid, msg, MsgPriority.NORMAL);
    }

    @Override
    public boolean add(Dpid dpid, OFMessage msg, MsgPriority priority) {
        return addMessageImpl(dpid, msg, priority);
    }

    @Override
    public void pushFlowEntries(
            Collection<Pair<Dpid, FlowEntry>> entries) {
        pushFlowEntries(entries, MsgPriority.NORMAL);
    }

    @Override
    public void pushFlowEntries(
            Collection<Pair<Dpid, FlowEntry>> entries, MsgPriority priority) {

        for (Pair<Dpid, FlowEntry> entry : entries) {
            add(entry.getLeft(), entry.getRight(), priority);
        }
    }

    @Override
    public void pushFlowEntry(Dpid dpid, FlowEntry flowEntry) {
        pushFlowEntry(dpid, flowEntry, MsgPriority.NORMAL);
    }

    @Override
    public void pushFlowEntry(Dpid dpid, FlowEntry flowEntry, MsgPriority priority) {
        Collection<Pair<Dpid, FlowEntry>> entries = new LinkedList<>();

        entries.add(Pair.of(dpid, flowEntry));
        pushFlowEntries(entries, priority);
    }

    public static final int PRIORITY_DEFAULT = 32768; // Default Flow Priority

    @Override
    public void pushMatchAction(MatchActionOperationEntry matchActionOp) {
        final MatchAction matchAction = matchActionOp.getTarget();

        // Get the switch and its OFFactory
        final SwitchPort srcPort = matchAction.getSwitchPort();
        final Dpid dpid = srcPort.getDpid();
        IOFSwitch sw = floodlightProvider.getMasterSwitch(dpid.value());
        if (sw == null) {
            log.warn("Couldn't find switch {} when pushing message", dpid);
            return;
        }
        OFFactory factory = sw.getFactory();

        // Build Match
        final Match match = matchAction.getMatch();
        Builder matchBuilder = factory.buildMatch();
        if (match instanceof PacketMatch) {
            final PacketMatch packetMatch = (PacketMatch) match;
            final MACAddress srcMac = packetMatch.getSrcMacAddress();
            final MACAddress dstMac = packetMatch.getDstMacAddress();
            final Short etherType = packetMatch.getEtherType();
            final IPv4Net srcIp = packetMatch.getSrcIpAddress();
            final IPv4Net dstIp = packetMatch.getDstIpAddress();
            final Byte ipProto = packetMatch.getIpProtocolNumber();
            final Short srcTcpPort = packetMatch.getSrcTcpPortNumber();
            final Short dstTcpPort = packetMatch.getDstTcpPortNumber();

            if (srcMac != null) {
                matchBuilder.setExact(MatchField.ETH_SRC, MacAddress.of(srcMac.toLong()));
            }
            if (dstMac != null) {
                matchBuilder.setExact(MatchField.ETH_DST, MacAddress.of(dstMac.toLong()));
            }
            if (etherType != null) {
                matchBuilder.setExact(MatchField.ETH_TYPE, EthType.of(etherType));
            }
            if (srcIp != null) {
                matchBuilder.setMasked(MatchField.IPV4_SRC,
                        IPv4Address.of(srcIp.address().value())
                                .withMaskOfLength(srcIp.prefixLen()));
            }
            if (dstIp != null) {
                matchBuilder.setMasked(MatchField.IPV4_DST,
                        IPv4Address.of(dstIp.address().value())
                                .withMaskOfLength(dstIp.prefixLen()));
            }
            if (ipProto != null) {
                matchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.of(ipProto));
            }
            if (srcTcpPort != null) {
                matchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(srcTcpPort));
            }
            if (dstTcpPort != null) {
                matchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(dstTcpPort));
            }
            matchBuilder.setExact(MatchField.IN_PORT,
                    OFPort.of(srcPort.getPortNumber().shortValue()));
        } else {
            log.warn("Unsupported Match type: {}", match.getClass().getName());
            return;
        }

        // Build Actions
        List<OFAction> actionList = new ArrayList<>(matchAction.getActions().size());
        OFActions ofActionTypes = factory.actions();
        for (Action action : matchAction.getActions()) {
            OFAction ofAction = null;
            if (action instanceof OutputAction) {
                OutputAction outputAction = (OutputAction) action;
                // short or int?
                OFPort port = OFPort.of((int) outputAction.getPortNumber().value());
                ofAction = ofActionTypes.output(port, Short.MAX_VALUE);
            } else if (action instanceof ModifyDstMacAction) {
                ModifyDstMacAction dstMacAction = (ModifyDstMacAction) action;
                ofActionTypes.setDlDst(MacAddress.of(dstMacAction.getDstMac().toLong()));
            } else if (action instanceof ModifySrcMacAction) {
                ModifySrcMacAction srcMacAction = (ModifySrcMacAction) action;
                ofActionTypes.setDlSrc(MacAddress.of(srcMacAction.getSrcMac().toLong()));
            } else {
                log.warn("Unsupported Action type: {}", action.getClass().getName());
                continue;
            }
            actionList.add(ofAction);
        }

        // Construct a FlowMod message builder
        OFFlowMod.Builder fmBuilder = null;
        switch (matchActionOp.getOperator()) {
        case ADD:
            fmBuilder = factory.buildFlowAdd();
            break;
        case REMOVE:
            fmBuilder = factory.buildFlowDeleteStrict();
            break;
        // case MODIFY: // TODO
        // fmBuilder = factory.buildFlowModifyStrict();
        // break;
        default:
            log.warn("Unsupported MatchAction Operator: {}", matchActionOp.getOperator());
            return;
        }

        // Add output port for OF1.0
        OFPort outp = OFPort.of((short) 0xffff); // OF1.0 OFPP.NONE
        if (matchActionOp.getOperator() == Operator.REMOVE) {
            if (actionList.size() == 1) {
                if (actionList.get(0).getType() == OFActionType.OUTPUT) {
                    OFActionOutput oa = (OFActionOutput) actionList.get(0);
                    outp = oa.getPort();
                }
            }
        }


        // Build OFFlowMod
        fmBuilder.setMatch(matchBuilder.build())
                .setActions(actionList)
                .setIdleTimeout(0) // hardcoded to zero for now
                .setHardTimeout(0) // hardcoded to zero for now
                .setCookie(U64.of(matchAction.getId().value()))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(PRIORITY_DEFAULT)
                .setOutPort(outp);

        // Build the message and add it to the queue
        add(dpid, fmBuilder.build());
    }

    @Override
    public void pushMatchActions(Collection<MatchActionOperationEntry> matchActionOps) {
        for (MatchActionOperationEntry matchActionOp : matchActionOps) {
            pushMatchAction(matchActionOp);
        }
    }

    /**
     * Create a message from FlowEntry and add it to the queue of the switch.
     * <p>
     * @param dpid DPID of the switch to which the message is pushed.
     * @param flowEntry FlowEntry object used for creating message.
     * @return true if message is successfully added to a queue.
     */
    private boolean add(Dpid dpid, FlowEntry flowEntry, MsgPriority priority) {
        IOFSwitch sw = floodlightProvider.getMasterSwitch(dpid.value());
        if (sw == null) {
            log.warn("Couldn't find switch {} when pushing message", dpid);
            return false;
        }

        //
        // Create the OpenFlow Flow Modification Entry to push
        //
        OFFlowMod fm = flowEntry.buildFlowMod(sw.getFactory());
        // log.trace("Pushing flow mod {}", fm);
        return addMessageImpl(dpid, fm, priority);
    }

    /**
     * Add message to queue.
     * <p>
     * @param dpid DPID of the switch to which the message is sent
     * @param msg message to send to the switch
     * @param priority priority of the message
     * @return true if the message was added successfully, otherwise false
     */
    protected boolean addMessageImpl(Dpid dpid, OFMessage msg, MsgPriority priority) {
        FlowPusherThread thread = getProcessingThread(dpid);

        SwitchQueue queue = getQueue(dpid);

        // create queue at first addition of message
        if (queue == null) {
            queue = createQueueImpl(dpid);
        }

        SwitchQueueEntry entry = new SwitchQueueEntry(msg);

        synchronized (queue) {
            queue.add(entry, priority);
            if (log.isTraceEnabled()) {
                log.trace("Message is pushed to switch {}: {}",
                        dpid, entry.getOFMessage());
            }
        }

        thread.notifyMessagePushed();

        return true;
    }

    @Override
    public OFBarrierReply barrier(Dpid dpid) {
        OFMessageFuture<OFBarrierReply> future = barrierAsync(dpid);
        if (future == null) {
            return null;
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        } catch (ExecutionException e) {
            log.error("ExecutionException", e);
        }
        return null;
    }

    @Override
    public OFMessageFuture<OFBarrierReply> barrierAsync(Dpid dpid) {
        // TODO creation of message and future should be moved to OFSwitchImpl
        IOFSwitch sw = floodlightProvider.getMasterSwitch(dpid.value());
        if (sw == null) {
            return null;
        }

        OFBarrierRequest msg = createBarrierRequest(sw);
        OFBarrierReplyFuture future = new OFBarrierReplyFuture(threadPool, sw,
                (int) msg.getXid());
        barrierFutures.put(BarrierInfo.create(dpid.value(), msg), future);
        addMessageImpl(dpid, msg, MsgPriority.NORMAL);
        return future;
    }

    protected OFBarrierRequest createBarrierRequest(IOFSwitch sw) {
        OFFactory factory = sw.getFactory();
        if (factory == null) {
            log.error("No OF Message Factory for switch {} with OFVersion {}", sw,
                    sw.getOFVersion());
            return null;
        }
        return factory.buildBarrierRequest()
                .setXid(sw.getNextTransactionId())
                .build();
    }

    /**
     * Get a queue attached to a switch.
     * <p>
     * @param dpid DPID of the switch
     * @return Queue object
     */
    protected SwitchQueue getQueue(Dpid dpid) {
        if (dpid == null) {
            return null;
        }

        FlowPusherThread th = getProcessingThread(dpid);
        if (th == null) {
            return null;
        }

        return th.assignedQueues.get(dpid);
    }

    /**
     * Get a hash value correspondent to a switch.
     * <p>
     * @param dpid DPID of the switch
     * @return Hash value
     */
    protected long getHash(long dpid) {
        // This code assumes DPID is sequentially assigned.
        // TODO consider equalization algorithm
        return dpid % numberThread;
    }

    /**
     * Get a Thread object which processes the queue attached to a switch.
     * <p>
     * @param dpid DPID of the switch
     * @return Thread object
     */
    protected FlowPusherThread getProcessingThread(Dpid dpid) {
        long hash = getHash(dpid.value());

        return threadMap.get(hash);
    }

    @Override
    public String getName() {
        return "flowpusher";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        if (log.isTraceEnabled()) {
            log.trace("Received BARRIER_REPLY from : {}", sw.getStringId());
        }

        if ((msg.getType() != OFType.BARRIER_REPLY) ||
                !(msg instanceof OFBarrierReply)) {
            log.error("Unexpected reply message: {}", msg.getType());
            return Command.CONTINUE;
        }

        OFBarrierReply reply = (OFBarrierReply) msg;
        BarrierInfo info = BarrierInfo.create(sw.getId(), reply);
        // Deliver future if exists
        OFBarrierReplyFuture future = barrierFutures.get(info);
        if (future != null) {
            future.deliverFuture(sw, msg);
            barrierFutures.remove(info);
        }

        return Command.CONTINUE;
    }

}
