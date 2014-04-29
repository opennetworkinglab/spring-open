package net.onrc.onos.apps.proxyarp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.packet.IPacketListener;
import net.onrc.onos.api.packet.IPacketService;
import net.onrc.onos.apps.sdnip.Interface;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.devicemanager.IOnosDeviceService;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.packet.ARP;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.packet.IPv4;
import net.onrc.onos.core.topology.Device;
import net.onrc.onos.core.topology.INetworkGraphService;
import net.onrc.onos.core.topology.NetworkGraph;
import net.onrc.onos.core.topology.Port;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.SwitchPort;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public class ProxyArpManager implements IProxyArpService, IFloodlightModule,
                                        IPacketListener {
    private static final Logger log = LoggerFactory
            .getLogger(ProxyArpManager.class);

    private static long arpTimerPeriodConfig = 100; // ms
    private static int arpRequestTimeoutConfig = 2000; // ms
    private long arpCleaningTimerPeriodConfig = 60 * 1000; // ms (1 min)

    private IDatagridService datagrid;
    private IEventChannel<Long, ArpReplyNotification> arpReplyEventChannel;
    private IEventChannel<String, ArpCacheNotification> arpCacheEventChannel;
    private static final String ARP_REPLY_CHANNEL_NAME = "onos.arp_reply";
    private static final String ARP_CACHE_CHANNEL_NAME = "onos.arp_cache";
    private final ArpReplyEventHandler arpReplyEventHandler = new ArpReplyEventHandler();
    private final ArpCacheEventHandler arpCacheEventHandler = new ArpCacheEventHandler();

    private IConfigInfoService configService;
    private IRestApiService restApi;

    private INetworkGraphService networkGraphService;
    private NetworkGraph networkGraph;
    private IOnosDeviceService onosDeviceService;
    private IPacketService packetService;

    private short vlan;
    private static final short NO_VLAN = 0;

    private SetMultimap<InetAddress, ArpRequest> arpRequests;

    private ArpCache arpCache;

    private class ArpReplyEventHandler implements
            IEventChannelListener<Long, ArpReplyNotification> {

        @Override
        public void entryAdded(ArpReplyNotification arpReply) {
            log.debug("Received ARP reply notification for ip {}, mac {}",
                    arpReply.getTargetAddress(), arpReply.getTargetMacAddress());
            //This 4 means ipv4 addr size. Need to change it in the future.
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(arpReply.getTargetAddress());
            InetAddress addr = null;
            try {
                addr = InetAddress.getByAddress(buffer.array());
            } catch (UnknownHostException e) {
                log.error("Exception:", e);
            }

            if (addr != null) {
                sendArpReplyToWaitingRequesters(addr,
                        arpReply.getTargetMacAddress());
            }
        }

        @Override
        public void entryUpdated(ArpReplyNotification arpReply) {
            entryAdded(arpReply);
        }

        @Override
        public void entryRemoved(ArpReplyNotification arpReply) {
            //Not implemented. ArpReplyEventHandler is used only for remote messaging.
        }
    }

    private class ArpCacheEventHandler implements
    IEventChannelListener<String, ArpCacheNotification> {

        /**
         * Startup processing.
         */
        private void startUp() {
            //
            // TODO: Read all state from the database:
            // For now, as a shortcut we read it from the datagrid
            //
            Collection<ArpCacheNotification> arpCacheEvents =
                    arpCacheEventChannel.getAllEntries();

            for (ArpCacheNotification arpCacheEvent : arpCacheEvents) {
                entryAdded(arpCacheEvent);
            }
        }

        @Override
        public void entryAdded(ArpCacheNotification value) {

            try {
                log.debug("Received entryAdded for ARP cache notification for ip {}, mac {}",
                    InetAddress.getByAddress(value.getTargetAddress()), value.getTargetMacAddress());
                arpCache.update(InetAddress.getByAddress(value.getTargetAddress()), MACAddress.valueOf(value.getTargetMacAddress()));
            } catch (UnknownHostException e) {
                log.error("Exception : ", e);
            }
        }

        @Override
        public void entryRemoved(ArpCacheNotification value) {
            log.debug("Received entryRemoved for ARP cache notification for ip {}, mac {}",
                    value.getTargetAddress(), value.getTargetMacAddress());
            try {
                arpCache.remove(InetAddress.getByAddress(value.getTargetAddress()));
            } catch (UnknownHostException e) {
                log.error("Exception : ", e);
            }
        }

        @Override
        public void entryUpdated(ArpCacheNotification value) {
            try {
                log.debug("Received entryUpdated for ARP cache notification for ip {}, mac {}",
                        InetAddress.getByAddress(value.getTargetAddress()), value.getTargetMacAddress());
                arpCache.update(InetAddress.getByAddress(value.getTargetAddress()), MACAddress.valueOf(value.getTargetMacAddress()));
            } catch (UnknownHostException e) {
                log.error("Exception : ", e);
            }
        }
    }

    private static class ArpRequest {
        private final IArpRequester requester;
        private final boolean retry;
        private boolean sent = false;
        private long requestTime;

        public ArpRequest(IArpRequester requester, boolean retry) {
            this.requester = requester;
            this.retry = retry;
        }

        public ArpRequest(ArpRequest old) {
            this.requester = old.requester;
            this.retry = old.retry;
        }

        public boolean isExpired() {
            return sent
                    && ((System.currentTimeMillis() - requestTime) > arpRequestTimeoutConfig);
        }

        public boolean shouldRetry() {
            return retry;
        }

        public void dispatchReply(InetAddress ipAddress,
                                  MACAddress replyMacAddress) {
            requester.arpResponse(ipAddress, replyMacAddress);
        }

        public void setRequestTime() {
            this.requestTime = System.currentTimeMillis();
            this.sent = true;
        }
    }

    private class HostArpRequester implements IArpRequester {
        private final ARP arpRequest;
        private final long dpid;
        private final short port;

        public HostArpRequester(ARP arpRequest, long dpid, short port) {
            this.arpRequest = arpRequest;
            this.dpid = dpid;
            this.port = port;
        }

        @Override
        public void arpResponse(InetAddress ipAddress, MACAddress macAddress) {
            ProxyArpManager.this.sendArpReply(arpRequest, dpid, port,
                    macAddress);
        }

        public ARP getArpRequest() {
            return arpRequest;
        }
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IProxyArpService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        m.put(IProxyArpService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(IRestApiService.class);
        dependencies.add(IDatagridService.class);
        dependencies.add(IConfigInfoService.class);
        dependencies.add(INetworkGraphService.class);
        dependencies.add(IOnosDeviceService.class);
        dependencies.add(IPacketService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context) {
        this.configService = context.getServiceImpl(IConfigInfoService.class);
        this.restApi = context.getServiceImpl(IRestApiService.class);
        this.datagrid = context.getServiceImpl(IDatagridService.class);
        this.networkGraphService = context.getServiceImpl(INetworkGraphService.class);
        this.onosDeviceService = context.getServiceImpl(IOnosDeviceService.class);
        this.packetService = context.getServiceImpl(IPacketService.class);

        arpRequests = Multimaps.synchronizedSetMultimap(HashMultimap
                .<InetAddress, ArpRequest>create());
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        Map<String, String> configOptions = context.getConfigParams(this);

        try {
            arpCleaningTimerPeriodConfig = Long.parseLong(configOptions.get("cleanupmsec"));
        } catch (NumberFormatException e) {
            log.debug("ArpCleaningTimerPeriod related config options were not set. Use default.");
        }

        Long agingmsec = null;
        try {
            agingmsec = Long.parseLong(configOptions.get("agingmsec"));
        } catch (NumberFormatException e) {
            log.debug("ArpEntryTimeout related config options were not set. Use default.");
        }

        arpCache = new ArpCache();
        if (agingmsec != null) {
            arpCache.setArpEntryTimeoutConfig(agingmsec);
        }

        this.vlan = configService.getVlan();
        log.info("vlan set to {}", this.vlan);

        restApi.addRestletRoutable(new ArpWebRoutable());
        packetService.registerPacketListener(this);
        networkGraph = networkGraphService.getNetworkGraph();

        //
        // Event notification setup: channels and event handlers
        //

        arpReplyEventChannel = datagrid.addListener(ARP_REPLY_CHANNEL_NAME,
                arpReplyEventHandler,
                Long.class,
                ArpReplyNotification.class);

        arpCacheEventChannel = datagrid.addListener(ARP_CACHE_CHANNEL_NAME,
                arpCacheEventHandler,
                String.class,
                ArpCacheNotification.class);
        arpCacheEventHandler.startUp();

        Timer arpTimer = new Timer("arp-processing");
        arpTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doPeriodicArpProcessing();
            }
        }, 0, arpTimerPeriodConfig);

        Timer arpCacheTimer = new Timer("arp-clearning");
        arpCacheTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doPeriodicArpCleaning();
            }
        }, 0, arpCleaningTimerPeriodConfig);
    }

    /*
     * Function that runs periodically to manage the asynchronous request mechanism.
     * It basically cleans up old ARP requests if we don't get a response for them.
     * The caller can designate that a request should be retried indefinitely, and
     * this task will handle that as well.
     */
    private void doPeriodicArpProcessing() {
        SetMultimap<InetAddress, ArpRequest> retryList = HashMultimap
                .<InetAddress, ArpRequest>create();

        // Have to synchronize externally on the Multimap while using an
        // iterator,
        // even though it's a synchronizedMultimap
        synchronized (arpRequests) {
            Iterator<Map.Entry<InetAddress, ArpRequest>> it = arpRequests
                    .entries().iterator();

            while (it.hasNext()) {
                Map.Entry<InetAddress, ArpRequest> entry = it.next();
                ArpRequest request = entry.getValue();
                if (request.isExpired()) {
                    log.debug("Cleaning expired ARP request for {}", entry
                            .getKey().getHostAddress());

                    // If the ARP request is expired and then delete the device
                    HostArpRequester requester = (HostArpRequester) request.requester;
                    ARP req = requester.getArpRequest();
                    networkGraph.acquireReadLock();
                    Device targetDev = networkGraph.getDeviceByMac(MACAddress.valueOf(req.getTargetHardwareAddress()));
                    networkGraph.releaseReadLock();
                    if (targetDev != null) {
                        onosDeviceService.deleteOnosDeviceByMac(MACAddress.valueOf(req.getTargetHardwareAddress()));
                        if (log.isDebugEnabled()) {
                            log.debug("RemoveDevice: {} due to no have not recieve the ARP reply", targetDev.getMacAddress());
                        }
                    }

                    it.remove();

                    if (request.shouldRetry()) {
                        retryList.put(entry.getKey(), request);
                    }
                }
            }
        }

        for (Map.Entry<InetAddress, Collection<ArpRequest>> entry : retryList
                .asMap().entrySet()) {

            InetAddress address = entry.getKey();

            log.debug("Resending ARP request for {}", address.getHostAddress());

            // Only ARP requests sent by the controller will have the retry flag
            // set, so for now we can just send a new ARP request for that
            // address.
            sendArpRequestForAddress(address);

            for (ArpRequest request : entry.getValue()) {
                arpRequests.put(address, new ArpRequest(request));
            }
        }
    }

    @Override
    public void receive(Switch sw, Port inPort, Ethernet eth) {

        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            ARP arp = (ARP) eth.getPayload();
            learnArp(arp);
            if (arp.getOpCode() == ARP.OP_REQUEST) {
                handleArpRequest(sw.getDpid(), inPort.getNumber().shortValue(),
                        arp, eth);
            } else if (arp.getOpCode() == ARP.OP_REPLY) {
                // For replies we simply send a notification via Hazelcast
                sendArpReplyNotification(eth);
            }
        }
    }

    private void learnArp(ARP arp) {
        ArpCacheNotification arpCacheNotification = null;

        arpCacheNotification = new ArpCacheNotification(arp.getSenderProtocolAddress(), arp.getSenderHardwareAddress());

        try {
            arpCacheEventChannel.addEntry(InetAddress.getByAddress(arp.getSenderProtocolAddress()).toString(), arpCacheNotification);
        } catch (UnknownHostException e) {
            log.error("Exception : ", e);
        }
    }

    private void handleArpRequest(long dpid, short inPort, ARP arp, Ethernet eth) {
        if (log.isTraceEnabled()) {
            log.trace("ARP request received for {}",
                    inetAddressToString(arp.getTargetProtocolAddress()));
        }

        InetAddress target;
        try {
            target = InetAddress.getByAddress(arp.getTargetProtocolAddress());
        } catch (UnknownHostException e) {
            log.debug("Invalid address in ARP request", e);
            return;
        }

        if (configService.fromExternalNetwork(dpid, inPort)) {
            // If the request came from outside our network, we only care if
            // it was a request for one of our interfaces.
            if (configService.isInterfaceAddress(target)) {
                log.trace(
                        "ARP request for our interface. Sending reply {} => {}",
                        target.getHostAddress(),
                        configService.getRouterMacAddress());

                sendArpReply(arp, dpid, inPort,
                        configService.getRouterMacAddress());
            }

            return;
        }

        //MACAddress mac = arpCache.lookup(target);

        arpRequests.put(target, new ArpRequest(
                new HostArpRequester(arp, dpid, inPort), false));

        networkGraph.acquireReadLock();
        Device targetDevice = networkGraph.getDeviceByMac(
                MACAddress.valueOf(arp.getTargetHardwareAddress()));
        networkGraph.releaseReadLock();

        if (targetDevice == null) {
            if (log.isTraceEnabled()) {
                log.trace("No device info found for {} - broadcasting",
                        target.getHostAddress());
            }

            // We don't know the device so broadcast the request out
            packetService.broadcastPacketOutEdge(eth,
                    new SwitchPort(dpid, inPort));
        } else {
            // Even if the device exists in our database, we do not reply to
            // the request directly, but check whether the device is still valid
            MACAddress macAddress = MACAddress.valueOf(arp.getTargetHardwareAddress());

            if (log.isTraceEnabled()) {
                log.trace("The target Device Record in DB is: {} => {} from ARP request host at {}/{}",
                        new Object[]{
                                inetAddressToString(arp.getTargetProtocolAddress()),
                                macAddress,
                                HexString.toHexString(dpid), inPort});
            }

            // sendArpReply(arp, sw.getId(), pi.getInPort(), macAddress);

            Iterable<net.onrc.onos.core.topology.Port> outPorts = targetDevice.getAttachmentPoints();

            if (!outPorts.iterator().hasNext()) {
                if (log.isTraceEnabled()) {
                    log.trace("Device {} exists but is not connected to any ports" +
                            " - broadcasting", macAddress);
                }

                packetService.broadcastPacketOutEdge(eth,
                        new SwitchPort(dpid, inPort));
            } else {
                for (net.onrc.onos.core.topology.Port portObject : outPorts) {

                    if (portObject.getOutgoingLink() != null || portObject.getIncomingLink() != null) {
                        continue;
                    }

                    short outPort = portObject.getNumber().shortValue();
                    Switch outSwitchObject = portObject.getSwitch();
                    long outSwitch = outSwitchObject.getDpid();

                    if (log.isTraceEnabled()) {
                        log.trace("Probing device {} on port {}/{}",
                                new Object[]{macAddress,
                                        HexString.toHexString(outSwitch), outPort});
                    }

                    packetService.sendPacket(
                            eth, new SwitchPort(outSwitch, outPort));
                }
            }
        }
    }

    // TODO this method has not been tested after recent implementation changes.
    private void sendArpRequestForAddress(InetAddress ipAddress) {
        // TODO what should the sender IP address and MAC address be if no
        // IP addresses are configured? Will there ever be a need to send
        // ARP requests from the controller in that case?
        // All-zero MAC address doesn't seem to work - hosts don't respond to it

        byte[] zeroIpv4 = {0x0, 0x0, 0x0, 0x0};
        byte[] zeroMac = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        byte[] genericNonZeroMac = {0x0, 0x0, 0x0, 0x0, 0x0, 0x01};
        byte[] broadcastMac = {(byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff};

        ARP arpRequest = new ARP();

        arpRequest
                .setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) IPv4.ADDRESS_LENGTH)
                .setOpCode(ARP.OP_REQUEST).setTargetHardwareAddress(zeroMac)
                .setTargetProtocolAddress(ipAddress.getAddress());

        MACAddress routerMacAddress = configService.getRouterMacAddress();
        // As for now, it's unclear what the MAC address should be
        byte[] senderMacAddress = genericNonZeroMac;
        if (routerMacAddress != null) {
            senderMacAddress = routerMacAddress.toBytes();
        }
        arpRequest.setSenderHardwareAddress(senderMacAddress);

        byte[] senderIPAddress = zeroIpv4;
        Interface intf = configService.getOutgoingInterface(ipAddress);
        if (intf == null) {
            // TODO handle the case where the controller needs to send an ARP
            // request but there's not IP configuration. In this case the
            // request should be broadcast out all edge ports in the network.
            log.warn("Sending ARP requests with default configuration "
                    + "not supported");
            return;
        }

        senderIPAddress = intf.getIpAddress().getAddress();

        arpRequest.setSenderProtocolAddress(senderIPAddress);

        Ethernet eth = new Ethernet();
        eth.setSourceMACAddress(senderMacAddress)
                .setDestinationMACAddress(broadcastMac)
                .setEtherType(Ethernet.TYPE_ARP).setPayload(arpRequest);

        if (vlan != NO_VLAN) {
            eth.setVlanID(vlan).setPriorityCode((byte) 0);
        }

        // sendArpRequestToSwitches(ipAddress, eth.serialize());

        packetService.sendPacket(
                eth, new SwitchPort(intf.getDpid(), intf.getPort()));
    }

    //Please leave it for now because this code is needed for SDN-IP. It will be removed soon.
    /*
    private void sendArpRequestToSwitches(InetAddress dstAddress, byte[] arpRequest) {
        sendArpRequestToSwitches(dstAddress, arpRequest,
                0, OFPort.OFPP_NONE.getValue());
    }

    private void sendArpRequestToSwitches(InetAddress dstAddress, byte[] arpRequest,
            long inSwitch, short inPort) {

        if (configService.hasLayer3Configuration()) {
            Interface intf = configService.getOutgoingInterface(dstAddress);
            if (intf != null) {
                sendArpRequestOutPort(arpRequest, intf.getDpid(), intf.getPort());
            }
            else {
                //TODO here it should be broadcast out all non-interface edge ports.
                //I think we can assume that if it's not a request for an external
                //network, it's an ARP for a host in our own network. So we want to
                //send it out all edge ports that don't have an interface configured
                //to ensure it reaches all hosts in our network.
                log.debug("No interface found to send ARP request for {}",
                        dstAddress.getHostAddress());
            }
        }
        else {
            broadcastArpRequestOutEdge(arpRequest, inSwitch, inPort);
        }
    }
    */

    private void sendArpReplyNotification(Ethernet eth) {
        ARP arp = (ARP) eth.getPayload();

        if (log.isTraceEnabled()) {
            log.trace("Sending ARP reply for {} to other ONOS instances",
                    inetAddressToString(arp.getSenderProtocolAddress()));
        }

        InetAddress targetAddress;

        try {
            targetAddress = InetAddress.getByAddress(arp
                    .getSenderProtocolAddress());
        } catch (UnknownHostException e) {
            log.error("Unknown host", e);
            return;
        }

        MACAddress mac = new MACAddress(arp.getSenderHardwareAddress());

        ArpReplyNotification value =
                new ArpReplyNotification(ByteBuffer.wrap(targetAddress.getAddress()).getInt(), mac);
        log.debug("ArpReplyNotification ip {}, mac{}", ByteBuffer.wrap(targetAddress.getAddress()).getInt(), mac);
        arpReplyEventChannel.addTransientEntry(mac.toLong(), value);
    }

    private void sendArpReply(ARP arpRequest, long dpid, short port,
                              MACAddress targetMac) {
        if (log.isTraceEnabled()) {
            log.trace("Sending reply {} => {} to {}",
                    new Object[]{
                    inetAddressToString(arpRequest.getTargetProtocolAddress()),
                    targetMac, inetAddressToString(arpRequest
                                    .getSenderProtocolAddress())});
        }

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

        if (vlan != NO_VLAN) {
            eth.setVlanID(vlan).setPriorityCode((byte) 0);
        }

        packetService.sendPacket(eth, new SwitchPort(dpid, port));
    }

    private String inetAddressToString(byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address", e);
            return "";
        }
    }

    /*
     * IProxyArpService methods
     */

    @Override
    public MACAddress getMacAddress(InetAddress ipAddress) {
        return arpCache.lookup(ipAddress);
    }

    @Override
    public void sendArpRequest(InetAddress ipAddress, IArpRequester requester,
                               boolean retry) {
        arpRequests.put(ipAddress, new ArpRequest(requester, retry));

        // Sanity check to make sure we don't send a request for our own address
        if (!configService.isInterfaceAddress(ipAddress)) {
            sendArpRequestForAddress(ipAddress);
        }
    }

    @Override
    public List<String> getMappings() {
        return  arpCache.getMappings();
    }

    private void sendArpReplyToWaitingRequesters(InetAddress address,
                                                 MACAddress mac) {
        log.debug("Sending ARP reply for {} to requesters",
                address.getHostAddress());

        // See if anyone's waiting for this ARP reply
        Set<ArpRequest> requests = arpRequests.get(address);

        // Synchronize on the Multimap while using an iterator for one of the
        // sets
        List<ArpRequest> requestsToSend = new ArrayList<ArpRequest>(
                requests.size());
        synchronized (arpRequests) {
            Iterator<ArpRequest> it = requests.iterator();
            while (it.hasNext()) {
                ArpRequest request = it.next();
                it.remove();
                requestsToSend.add(request);
            }
        }

        // Don't hold an ARP lock while dispatching requests
        for (ArpRequest request : requestsToSend) {
            request.dispatchReply(address, mac);
        }
    }

    private void doPeriodicArpCleaning() {
        List<InetAddress> expiredipslist = arpCache.getExpiredArpCacheIps();
        for (InetAddress expireIp : expiredipslist) {
            log.debug("call arpCacheEventChannel.removeEntry, ip {}", expireIp);
            arpCacheEventChannel.removeEntry(expireIp.toString());
        }
    }

    public long getArpEntryTimeout() {
        return arpCache.getArpEntryTimeout();
    }

    public long getArpCleaningTimerPeriod() {
        return arpCleaningTimerPeriodConfig;
    }
}
