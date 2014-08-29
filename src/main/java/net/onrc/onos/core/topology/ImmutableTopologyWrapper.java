package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Wrapper to access {@link ImmutableTopology} as {@link MutableTopology}.
 */
public final class ImmutableTopologyWrapper implements MutableTopology {

    private final ImmutableTopology wrapped;

    /**
     * Constructor.
     *
     * @param toWrap {@link ImmutableTopology} to wrap
     */
    public ImmutableTopologyWrapper(ImmutableTopology toWrap) {
        this.wrapped = checkNotNull(toWrap);
    }

    @Override
    public void acquireReadLock() {
        // no-op
    }

    @Override
    public void releaseReadLock() {
        // no-op
    }

    @Override
    public Switch getSwitch(Dpid dpid) {
        return wrapped.getSwitch(dpid);
    }

    @Override
    public Iterable<Switch> getSwitches() {
        return wrapped.getSwitches();
    }

    @Override
    public Port getPort(Dpid dpid, PortNumber portNumber) {
        return wrapped.getPort(dpid, portNumber);
    }

    @Override
    public Port getPort(SwitchPort port) {
        return wrapped.getPort(port);
    }

    @Override
    public Collection<Port> getPorts(Dpid dpid) {
        return wrapped.getPorts(dpid);
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber portNumber) {
        return wrapped.getOutgoingLink(dpid, portNumber);
    }

    @Override
    public Link getOutgoingLink(Dpid dpid, PortNumber portNumber, String type) {
        return wrapped.getOutgoingLink(dpid, portNumber, type);
    }

    @Override
    public Link getOutgoingLink(SwitchPort port) {
        return wrapped.getOutgoingLink(port);
    }

    @Override
    public Link getOutgoingLink(SwitchPort port, String type) {
        return wrapped.getOutgoingLink(port, type);
    }

    @Override
    public Collection<Link> getOutgoingLinks(SwitchPort port) {
        return wrapped.getOutgoingLinks(port);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber portNumber) {
        return wrapped.getIncomingLink(dpid, portNumber);
    }

    @Override
    public Link getIncomingLink(Dpid dpid, PortNumber portNumber, String type) {
        return wrapped.getIncomingLink(dpid, portNumber, type);
    }

    @Override
    public Link getIncomingLink(SwitchPort port) {
        return wrapped.getIncomingLink(port);
    }

    @Override
    public Link getIncomingLink(SwitchPort port, String type) {
        return wrapped.getIncomingLink(port, type);
    }

    @Override
    public Collection<Link> getIncomingLinks(SwitchPort port) {
        return wrapped.getIncomingLinks(port);
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcPortNumber, Dpid dstDpid,
            PortNumber dstPortNumber) {
        return wrapped.getLink(srcDpid, srcPortNumber, dstDpid, dstPortNumber);
    }

    @Override
    public Link getLink(Dpid srcDpid, PortNumber srcPortNumber, Dpid dstDpid,
            PortNumber dstPortNumber, String type) {
        return wrapped.getLink(srcDpid, srcPortNumber, dstDpid, dstPortNumber, type);
    }

    @Override
    public Iterable<Link> getLinks() {
        return wrapped.getLinks();
    }

    @Override
    public Host getHostByMac(MACAddress address) {
        return wrapped.getHostByMac(address);
    }

    @Override
    public Iterable<Host> getHosts() {
        return wrapped.getHosts();
    }

    @Override
    public Collection<Host> getHosts(SwitchPort port) {
        return wrapped.getHosts(port);
    }

    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        return wrapped.getSwitchMaster(dpid);
    }
}
