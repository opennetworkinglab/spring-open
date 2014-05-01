package net.onrc.onos.core.topology;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.SwitchPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkGraphImpl implements NetworkGraph {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NetworkGraphImpl.class);

    // DPID -> Switch
    private final ConcurrentMap<Long, Switch> switches;
    private final ConcurrentMap<MACAddress, Device> mac2Device;

    private final ConcurrentMap<SwitchPort, Link> outgoingLinks;
    private final ConcurrentMap<SwitchPort, Link> incomingLinks;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private Lock writeLock = readWriteLock.writeLock();

    public NetworkGraphImpl() {
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        mac2Device = new ConcurrentHashMap<>();
        outgoingLinks = new ConcurrentHashMap<>();
        incomingLinks = new ConcurrentHashMap<>();
    }

    @Override
    public Switch getSwitch(Long dpid) {
        // TODO Check if it is safe to directly return this Object.
        return switches.get(dpid);
    }

    protected void putSwitch(Switch sw) {
        switches.put(sw.getDpid(), sw);
    }

    protected void removeSwitch(Long dpid) {
        switches.remove(dpid);
    }

    @Override
    public Iterable<Switch> getSwitches() {
        // TODO Check if it is safe to directly return this Object.
        return Collections.unmodifiableCollection(switches.values());
    }

    @Override
    public Port getPort(Long dpid, Long number) {
        Switch sw = getSwitch(dpid);
        if (sw != null) {
            return sw.getPort(number);
        }
        return null;
    }

    @Override
    public Link getOutgoingLink(Long dpid, Long number) {
        return outgoingLinks.get(new SwitchPort(dpid, number.shortValue()));
    }

    @Override
    public Link getIncomingLink(Long dpid, Long number) {
        return incomingLinks.get(new SwitchPort(dpid, number.shortValue()));
    }

    @Override
    public Link getLink(Long srcDpid, Long srcNumber, Long dstDpid,
                        Long dstNumber) {
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

    protected void putDevice(Device device) {
        mac2Device.put(device.getMacAddress(), device);
    }

    protected void removeDevice(Device device) {
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
