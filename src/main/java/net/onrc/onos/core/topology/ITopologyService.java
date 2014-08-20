package net.onrc.onos.core.topology;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Interface for providing the topology service to other modules.
 */
public interface ITopologyService extends IFloodlightService,
                                        ITopologyReplicationWriter {
    /**
     * Allows a module to get a reference to the global topology object.
     *
     * @return the global Topology object
     */
    public MutableTopology getTopology();

    /**
     * Adds a listener for topology events.
     *
     * @param listener the listener to add.
     * @param startFromSnapshot if true, and if the topology is not
     * empty, the first event should be a snapshot of the current topology.
     */
    public void addListener(ITopologyListener listener,
                            boolean startFromSnapshot);

    /**
     * Removes a listener for topology events. The listener will no longer
     * receive topology events after this call.
     *
     * @param listener the listener to remove.
     */
    public void removeListener(ITopologyListener listener);
}
