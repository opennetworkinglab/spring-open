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
    public SwitchData getSwitchData(Dpid dpid) {
        return wrapped.getSwitchData(dpid);
    }

    @Override
    public Collection<SwitchData> getAllSwitchDataEntries() {
        return wrapped.getAllSwitchDataEntries();
    }

    @Override
    public PortData getPortData(SwitchPort port) {
        return wrapped.getPortData(port);
    }

    @Override
    public PortData getPortData(Dpid dpid, PortNumber portNumber) {
        return wrapped.getPortData(dpid, portNumber);
    }

    @Override
    public Collection<PortData> getPortDataEntries(Dpid dpid) {
        return wrapped.getPortDataEntries(dpid);
    }

    @Override
    public Collection<PortData> getAllPortDataEntries() {
        return wrapped.getAllPortDataEntries();
    }

    @Override
    public LinkData getLinkData(LinkTuple linkId) {
        return wrapped.getLinkData(linkId);
    }

    @Override
    public LinkData getLinkData(LinkTuple linkId, String type) {
        return wrapped.getLinkData(linkId, type);
    }

    @Override
    public Collection<LinkData> getLinkDataEntries(LinkTuple linkId) {
        return wrapped.getLinkDataEntries(linkId);
    }

    @Override
    public Collection<LinkData> getAllLinkDataEntries() {
        return wrapped.getAllLinkDataEntries();
    }

    @Override
    public HostData getHostData(MACAddress mac) {
        return wrapped.getHostData(mac);
    }

    @Override
    public Collection<HostData> getAllHostDataEntries() {
        return wrapped.getAllHostDataEntries();
    }

    @Override
    public Collection<LinkData> getLinkDataEntriesFrom(SwitchPort srcPort) {
        return wrapped.getLinkDataEntriesFrom(srcPort);
    }

    @Override
    public Collection<LinkData> getLinkDataEntriesTo(SwitchPort dstPort) {
        return wrapped.getLinkDataEntriesTo(dstPort);
    }

    @Override
    public Collection<HostData> getHostDataEntries(SwitchPort port) {
        return wrapped.getHostDataEntries(port);
    }

    @Override
    public OnosInstanceId getSwitchMaster(Dpid dpid) {
        return wrapped.getSwitchMaster(dpid);
    }
}
