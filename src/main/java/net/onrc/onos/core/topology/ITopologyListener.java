package net.onrc.onos.core.topology;

/**
 * Interface that needs to be implemented to receive Topology events from
 * the Topology.
 */
public interface ITopologyListener {
    /**
     * Topology events that have been generated.
     *
     * @param topologyEvents the generated Topology Events
     * @see TopologyEvents
     */
    public void topologyEvents(TopologyEvents topologyEvents);
}
