package net.onrc.onos.core.topology;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Interface for providing the topology service to other modules.
 */
public interface ITopologyService extends IFloodlightService {
    /**
     * Allows a module to get a reference to the global topology object.
     *
     * @return the global Topology object
     */
    public Topology getTopology();

    /**
     * Registers a listener for topology events.
     *
     * @param listener the listener to register
     */
    public void registerTopologyListener(ITopologyListener listener);

    /**
     * Deregisters a listener for topology events. The listener will no longer
     * receive topology events after this call.
     *
     * @param listener the listener to deregister
     */
    public void deregisterTopologyListener(ITopologyListener listener);

    /**
     * Allows a module to get a reference to the southbound interface to
     * the topology.
     * TODO Figure out how to hide the southbound interface from
     * applications/modules that shouldn't touch it
     *
     * @return the TopologyDiscoveryInterface object
     */
    public TopologyDiscoveryInterface getTopologyDiscoveryInterface();
}
