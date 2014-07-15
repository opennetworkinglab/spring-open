package net.onrc.onos.core.topology;

import net.onrc.onos.core.topology.web.serializers.LinkSerializer;
import net.onrc.onos.core.util.LinkTuple;
import org.codehaus.jackson.map.annotate.JsonSerialize;

// TODO Everything returned by these interfaces must be either Unmodifiable view,
// immutable object, or a copy of the original "SB" In-memory Topology.
/**
 * Interface of Link object in the topology.
 */
@JsonSerialize(using = LinkSerializer.class)
public interface Link extends ITopologyElement, StringAttributes {

    /**
     * Gets a {@link LinkTuple} that identifies this link.
     *
     * @return a LinkTuple representing the Port
     */
    public LinkTuple getLinkTuple();

    /**
     * Gets the source switch for the link.
     *
     * @return the source switch for the link.
     */
    public Switch getSrcSwitch();

    /**
     * Gets the source port for the link.
     *
     * @return the source port for the link.
     */
    public Port getSrcPort();

    /**
     * Gets the destination switch for the link.
     *
     * @return the destination switch for the link.
     */
    public Switch getDstSwitch();

    /**
     * Gets the destination port for the link.
     *
     * @return the destination port for the link.
     */
    public Port getDstPort();

    /**
     * Gets the last seen time for the link.
     * <p/>
     * TODO: Not implemented yet.
     * TODO: what is the time definition?
     *
     * @return the last seen time for the link.
     */
    public long getLastSeenTime();

    /**
     * Gets the link capacity.
     * <p/>
     * TODO: What is the unit?
     *
     * @return the link capacity.
     */
    public Double getCapacity();

    /**
     * Returns the link type of the link.
     *
     * @return {@link LinkType} for this link
     */
    public LinkType getLinkType();
}
