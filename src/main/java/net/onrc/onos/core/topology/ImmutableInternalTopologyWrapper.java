package net.onrc.onos.core.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

/**
 * Wrapper to access {@link ImmutableInternalTopology} as {@link MutableInternalTopology}.
 */
public final class ImmutableInternalTopologyWrapper implements MutableInternalTopology {

    private final ImmutableInternalTopology wrapped;

    /**
     * Constructor.
     *
     * @param toWrap {@link ImmutableInternalTopology} to wrap
     */
    public ImmutableInternalTopologyWrapper(ImmutableInternalTopology toWrap) {
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
    public SwitchEvent getSwitchEvent(Dpid dpid) {
        return wrapped.getSwitchEvent(dpid);
    }

    @Override
    public Collection<SwitchEvent> getAllSwitchEvents() {
        return wrapped.getAllSwitchEvents();
    }

    @Override
    public PortEvent getPortEvent(SwitchPort port) {
        return wrapped.getPortEvent(port);
    }

    @Override
    public PortEvent getPortEvent(Dpid dpid, PortNumber portNumber) {
        return wrapped.getPortEvent(dpid, portNumber);
    }

    @Override
    public Collection<PortEvent> getPortEvents(Dpid dpid) {
        return wrapped.getPortEvents(dpid);
    }

    @Override
    public Collection<PortEvent> getAllPortEvents() {
        return wrapped.getAllPortEvents();
    }

    @Override
    public LinkEvent getLinkEvent(LinkTuple linkId) {
        return wrapped.getLinkEvent(linkId);
    }

    @Override
    public LinkEvent getLinkEvent(LinkTuple linkId, String type) {
        return wrapped.getLinkEvent(linkId, type);
    }

    @Override
    public Collection<LinkEvent> getLinkEvents(LinkTuple linkId) {
        return wrapped.getLinkEvents(linkId);
    }

    @Override
    public Collection<LinkEvent> getAllLinkEvents() {
        return wrapped.getAllLinkEvents();
    }

    @Override
    public HostEvent getHostEvent(MACAddress mac) {
        return wrapped.getHostEvent(mac);
    }

    @Override
    public Collection<HostEvent> getAllHostEvents() {
        return wrapped.getAllHostEvents();
    }

    @Override
    public Collection<LinkEvent> getLinkEventsFrom(SwitchPort srcPort) {
        return wrapped.getLinkEventsFrom(srcPort);
    }

    @Override
    public Collection<LinkEvent> getLinkEventsTo(SwitchPort dstPort) {
        return wrapped.getLinkEventsTo(dstPort);
    }

    @Override
    public Collection<HostEvent> getHostEvents(SwitchPort port) {
        return wrapped.getHostEvents(port);
    }

    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        return wrapped.getSwitchMaster(dpid);
    }
}
