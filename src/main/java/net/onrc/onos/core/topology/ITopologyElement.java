package net.onrc.onos.core.topology;

// TODO give me a better name.
/**
 * Interface common to Topology element interfaces.
 */
public interface ITopologyElement {

    // TODO The term Type is a bit confusing, may rename to something like layer
    /**
     * Returns the type of topology element.
     *
     * @return the type of the topology element
     */
    public String getType();
}
