package net.onrc.onos.core.topology;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class TopologyImpl implements Topology {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(TopologyImpl.class);

    // DPID -> Switch
    private final ConcurrentMap<Dpid, Switch> switches;
    // XXX may need to be careful when shallow copying.
    private final ConcurrentMap<Dpid, ConcurrentMap<PortNumber, Port>> ports;

    // Index from Port to Device
    private final Multimap<SwitchPort, Device> devices;
    private final ConcurrentMap<MACAddress, Device> mac2Device;

    private final ConcurrentMap<SwitchPort, Link> outgoingLinks;
    private final ConcurrentMap<SwitchPort, Link> incomingLinks;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private Lock writeLock = readWriteLock.writeLock();

    public TopologyImpl() {
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        ports = new ConcurrentHashMap<>();
        devices = Multimaps.synchronizedMultimap(
                HashMultimap.<SwitchPort, Device>create());
        mac2Device = new ConcurrentHashMap<>();
        outgoingLinks = new ConcurrentHashMap<>();
        incomingLinks = new ConcurrentHashMap<>();
    }

    @Override
    public Switch getSwitch(Dpid dpid) {
        // TODO Check if it is safe to directly return this Object.
        return switches.get(dpid);
    }

    // Only add switch.
    protected void putSwitch(Switch sw) {
        switches.put(sw.getDpid(), sw);
        ports.putIfAbsent(sw.getDpid(), new ConcurrentHashMap<PortNumber, Port>());
    }

    // TODO remove me when ready
    protected void removeSwitch(Long dpid) {
        removeSwitch(new Dpid(dpid));
    }

    // XXX Will remove ports in snapshot as side-effect.
    protected void removeSwitch(Dpid dpid) {
        switches.remove(dpid);
        ports.remove(dpid);
    }

    // This method is expected to be serialized by writeLock.
    protected void putPort(Port port) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(port.getDpid());
        if (portMap == null) {
            portMap = new ConcurrentHashMap<>();
            ConcurrentMap<PortNumber, Port> existing =
                    ports.putIfAbsent(port.getDpid(), portMap);
            if (existing != null) {
                // port map was added concurrently, using theirs
                portMap = existing;
            }
        }
        portMap.put(port.getNumber(), port);
    }

    protected void removePort(Port port) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(port.getDpid());
        if (portMap != null) {
            portMap.remove(port.getNumber());
        }
    }

    @Override
    public Iterable<Switch> getSwitches() {
        // TODO Check if it is safe to directly return this Object.
        return Collections.unmodifiableCollection(switches.values());
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber number) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(dpid);
        if (portMap != null) {
            return portMap.get(number);
        }
        return null;
    }

    @Override
    public Port getPort(SwitchPort port) {
        return getPort(port.dpid(), port.port());
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        ConcurrentMap<PortNumber, Port> portMap = ports.get(dpid);
        if (portMap == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(portMap.values());
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber number) {
        return outgoingLinks.get(new SwitchPort(dpid, number));
    }

    @Override
    public Link getOutgoingLink(SwitchPort port) {
        return outgoingLinks.get(port);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber number) {
        return incomingLinks.get(new SwitchPort(dpid, number));
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        return incomingLinks.get(port);
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcNumber,
                        Dpid dstDpid, PortNumber dstNumber) {

        Link link = getOutgoingLink(srcDpid, srcNumber);
        if (link == null) {
            return null;
        }
        if (!link.getDstSwitch().getDpid().equals(dstDpid)) {
            return null;
        }
        if (!link.getDstPort().getNumber().equals(dstNumber)) {
            return null;
        }
        return link;
    }

    @Override
    public Iterable<Link> getLinks() {
        return Collections.unmodifiableCollection(outgoingLinks.values());
    }

    protected void putLink(Link link) {
        outgoingLinks.put(link.getSrcPort().asSwitchPort(), link);
        incomingLinks.put(link.getDstPort().asSwitchPort(), link);
    }

    protected void removeLink(Link link) {
        outgoingLinks.remove(link.getSrcPort().asSwitchPort(), link);
        incomingLinks.remove(link.getDstPort().asSwitchPort(), link);
    }

    @Override
    public Device getDeviceByMac(MACAddress address) {
        return mac2Device.get(address);
    }

    @Override
    public Iterable<Device> getDevices() {
        return Collections.unmodifiableCollection(mac2Device.values());
    }

    @Override
    public Collection<Device> getDevices(SwitchPort port) {
        return Collections.unmodifiableCollection(devices.get(port));
    }

    // This method is expected to be serialized by writeLock.
    // XXX new or updated device
    protected void putDevice(Device device) {
        // assuming Device is immutable
        Device oldDevice = mac2Device.get(device.getMacAddress());
        if (oldDevice != null) {
            // remove old attachment point
            removeDevice(oldDevice);
        }
        // add new attachment points
        for (Port port : device.getAttachmentPoints()) {
            // TODO Won't need remove() if we define Device equality to reflect
            //      all of it's fields.
            devices.remove(port.asSwitchPort(), device);
            devices.put(port.asSwitchPort(), device);
        }
        mac2Device.put(device.getMacAddress(), device);
    }

    protected void removeDevice(Device device) {
        for (Port port : device.getAttachmentPoints()) {
            devices.remove(port.asSwitchPort(), device);
        }
        mac2Device.remove(device.getMacAddress());
    }

    @Override
    public void acquireReadLock() {
        readLock.lock();
    }

    @Override
    public void releaseReadLock() {
        readLock.unlock();
    }

    protected void acquireWriteLock() {
        writeLock.lock();
    }

    protected void releaseWriteLock() {
        writeLock.unlock();
    }
}
