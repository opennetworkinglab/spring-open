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
import net.onrc.onos.core.intent.FlowEntry;

import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;

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
    protected static final int DEFAULT_NUMBER_THREAD = 1;

    // Number of messages sent to switch at once
    protected static final int MAX_MESSAGE_SEND = 100;

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

        static BarrierInfo create(IOFSwitch sw, OFBarrierRequest req) {
            return new BarrierInfo(sw.getId(), req.getXid());
        }

        static BarrierInfo create(IOFSwitch sw, OFBarrierReply rpy) {
            return new BarrierInfo(sw.getId(), rpy.getXid());
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

    private FloodlightModuleContext context = null;
    private IThreadPoolService threadPool = null;
    private IFloodlightProviderService floodlightProvider = null;

    // Map of threads versus dpid
    private Map<Long, FlowPusherThread> threadMap = null;
    // Map from (DPID and transaction ID) to Future objects.
    private Map<BarrierInfo, OFBarrierReplyFuture> barrierFutures =
            new ConcurrentHashMap<BarrierInfo, OFBarrierReplyFuture>();

    private int numberThread;

    /**
     * Main thread that reads messages from queues and sends them to switches.
     */
    private static class FlowPusherThread extends Thread {
        // Weak ConcurrentHashMap
        private Map<IOFSwitch, SwitchQueue> assignedQueues = CacheBuilder.newBuilder()
                .weakKeys()
                .<IOFSwitch, SwitchQueue>build().asMap();

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

                for (Iterator<Entry<IOFSwitch, SwitchQueue>> it = assignedQueues
                        .entrySet().iterator(); it.hasNext();) {
                    Entry<IOFSwitch, SwitchQueue> entry = it.next();
                    IOFSwitch sw = entry.getKey();
                    SwitchQueue queue = entry.getValue();

                    if (queue == null) {
                        continue;
                    }

                    synchronized (queue) {
                        processQueue(sw, queue, MAX_MESSAGE_SEND);
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
         * @param sw Switch to which messages will be sent.
         * @param queue Queue of messages.
         * @param maxMsg Limitation of number of messages to be sent. If set to
         *        0, all messages in queue will be sent.
         */
        private void processQueue(IOFSwitch sw, SwitchQueue queue, int maxMsg) {
            // check sending rate and determine it to be sent or not
            long currentTime = System.currentTimeMillis();
            long size = 0;

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
    public boolean suspend(IOFSwitch sw) {
        SwitchQueue queue = getQueue(sw);

        if (queue == null) {
            // create queue in case suspend is called before first message
            // addition
            queue = createQueueImpl(sw);
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
    public boolean resume(IOFSwitch sw) {
        SwitchQueue queue = getQueue(sw);

        if (queue == null) {
            log.error("No queue is attached to DPID: {}", sw.getStringId());
            return false;
        }

        synchronized (queue) {
            if (queue.state == QueueState.SUSPENDED) {
                queue.state = QueueState.READY;

                // Free the latch if queue has any messages
                FlowPusherThread thread = getProcessingThread(sw);
                if (queue.hasMessageToSend()) {
                    thread.notifyMessagePushed();
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public QueueState getState(IOFSwitch sw) {
        SwitchQueue queue = getQueue(sw);

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
    public void setRate(IOFSwitch sw, long rate) {
        SwitchQueue queue = getQueue(sw);
        if (queue == null) {
            queue = createQueueImpl(sw);
        }

        if (rate > 0) {
            log.debug("rate for {} is set to {}", sw.getStringId(), rate);
            synchronized (queue) {
                queue.maxRate = rate;
            }
        }
    }

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "Future versions of createQueueImpl() might return null")
    public boolean createQueue(IOFSwitch sw) {
        SwitchQueue queue = createQueueImpl(sw);

        return (queue != null);
    }

    protected SwitchQueue createQueueImpl(IOFSwitch sw) {
        SwitchQueue queue = getQueue(sw);
        if (queue != null) {
            return queue;
        }

        FlowPusherThread proc = getProcessingThread(sw);
        queue = new SwitchQueue();
        queue.state = QueueState.READY;
        proc.assignedQueues.put(sw, queue);

        return queue;
    }

    @Override
    public boolean deleteQueue(IOFSwitch sw) {
        return deleteQueue(sw, false);
    }

    @Override
    public boolean deleteQueue(IOFSwitch sw, boolean forceStop) {
        FlowPusherThread proc = getProcessingThread(sw);

        if (forceStop) {
            SwitchQueue queue = proc.assignedQueues.remove(sw);
            if (queue == null) {
                return false;
            }
            return true;
        } else {
            SwitchQueue queue = getQueue(sw);
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
    public boolean add(IOFSwitch sw, OFMessage msg) {
        return add(sw, msg, MsgPriority.NORMAL);
    }

    @Override
    public boolean add(IOFSwitch sw, OFMessage msg, MsgPriority priority) {
        return addMessageImpl(sw, msg, priority);
    }

    @Override
    public void pushFlowEntries(
            Collection<Pair<IOFSwitch, FlowEntry>> entries) {
        pushFlowEntries(entries, MsgPriority.NORMAL);
    }

    @Override
    public void pushFlowEntries(
            Collection<Pair<IOFSwitch, FlowEntry>> entries, MsgPriority priority) {

        for (Pair<IOFSwitch, FlowEntry> entry : entries) {
            add(entry.getLeft(), entry.getRight(), priority);
        }
    }

    @Override
    public void pushFlowEntry(IOFSwitch sw, FlowEntry flowEntry) {
        pushFlowEntry(sw, flowEntry, MsgPriority.NORMAL);
    }

    @Override
    public void pushFlowEntry(IOFSwitch sw, FlowEntry flowEntry, MsgPriority priority) {
        Collection<Pair<IOFSwitch, FlowEntry>> entries = new LinkedList<>();

        entries.add(Pair.of(sw, flowEntry));
        pushFlowEntries(entries, priority);
    }

    /**
     * Create a message from FlowEntry and add it to the queue of the switch.
     * <p>
     * @param sw Switch to which message is pushed.
     * @param flowEntry FlowEntry object used for creating message.
     * @return true if message is successfully added to a queue.
     */
    private boolean add(IOFSwitch sw, FlowEntry flowEntry, MsgPriority priority) {
        //
        // Create the OpenFlow Flow Modification Entry to push
        //
        OFFlowMod fm = flowEntry.buildFlowMod(sw.getFactory());
        // log.trace("Pushing flow mod {}", fm);
        return addMessageImpl(sw, fm, priority);
    }

    /**
     * Add message to queue.
     * <p>
     * @param sw
     * @param msg
     * @param priority
     * @return true if the message was added successfully, otherwise false
     */
    protected boolean addMessageImpl(IOFSwitch sw, OFMessage msg, MsgPriority priority) {
        FlowPusherThread thread = getProcessingThread(sw);

        SwitchQueue queue = getQueue(sw);

        // create queue at first addition of message
        if (queue == null) {
            queue = createQueueImpl(sw);
        }

        SwitchQueueEntry entry = new SwitchQueueEntry(msg);

        synchronized (queue) {
            queue.add(entry, priority);
            if (log.isTraceEnabled()) {
                log.trace("Message is pushed to switch {}: {}", sw.getStringId(), entry.getOFMessage());
            }
        }

        thread.notifyMessagePushed();

        return true;
    }

    @Override
    public OFBarrierReply barrier(IOFSwitch sw) {
        OFMessageFuture<OFBarrierReply> future = barrierAsync(sw);
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
    public OFMessageFuture<OFBarrierReply> barrierAsync(IOFSwitch sw) {
        // TODO creation of message and future should be moved to OFSwitchImpl

        if (sw == null) {
            return null;
        }

        OFBarrierRequest msg = createBarrierRequest(sw);
        OFBarrierReplyFuture future = new OFBarrierReplyFuture(threadPool, sw,
                (int) msg.getXid());
        barrierFutures.put(BarrierInfo.create(sw, msg), future);
        addMessageImpl(sw, msg, MsgPriority.NORMAL);
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
     * @param sw Switch object
     * @return Queue object
     */
    protected SwitchQueue getQueue(IOFSwitch sw) {
        if (sw == null) {
            return null;
        }

        FlowPusherThread th = getProcessingThread(sw);
        if (th == null) {
            return null;
        }

        return th.assignedQueues.get(sw);
    }

    /**
     * Get a hash value correspondent to a switch.
     * <p>
     * @param sw Switch object
     * @return Hash value
     */
    protected long getHash(IOFSwitch sw) {
        // This code assumes DPID is sequentially assigned.
        // TODO consider equalization algorithm
        return sw.getId() % numberThread;
    }

    /**
     * Get a Thread object which processes the queue attached to a switch.
     * <p>
     * @param sw Switch object
     * @return Thread object
     */
    protected FlowPusherThread getProcessingThread(IOFSwitch sw) {
        long hash = getHash(sw);

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
        BarrierInfo info = BarrierInfo.create(sw, reply);
        // Deliver future if exists
        OFBarrierReplyFuture future = barrierFutures.get(info);
        if (future != null) {
            future.deliverFuture(sw, msg);
            barrierFutures.remove(info);
        }

        return Command.CONTINUE;
    }

}
