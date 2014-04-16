package net.onrc.onos.core.topology;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.util.MACAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkGraphImpl implements NetworkGraph {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NetworkGraphImpl.class);

    // DPID -> Switch
    private ConcurrentMap<Long, Switch> switches;
    private ConcurrentMap<MACAddress, Device> mac2Device;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    // TODO use the write lock after refactor
    private Lock writeLock = readWriteLock.writeLock();

    public NetworkGraphImpl() {
        // TODO: Does these object need to be stored in Concurrent Collection?
        switches = new ConcurrentHashMap<>();
        mac2Device = new ConcurrentHashMap<>();
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
    public Link getLink(Long dpid, Long number) {
        Port srcPort = getPort(dpid, number);
        if (srcPort == null) {
            return null;
        }
        return srcPort.getOutgoingLink();
    }

    @Override
    public Link getLink(Long srcDpid, Long srcNumber, Long dstDpid,
                        Long dstNumber) {
        Link link = getLink(srcDpid, srcNumber);
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
        List<Link> linklist = new LinkedList<>();

        for (Switch sw : switches.values()) {
            Iterable<Link> links = sw.getOutgoingLinks();
            for (Link l : links) {
                linklist.add(l);
            }
        }
        return linklist;
    }

    @Override
    public Device getDeviceByMac(MACAddress address) {
        return mac2Device.get(address);
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
