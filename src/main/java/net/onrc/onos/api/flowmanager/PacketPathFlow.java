package net.onrc.onos.api.flowmanager;

import java.util.List;

import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.PortNumber;

/**
 * Flow object representing a packet path.
 * <p>
 * TODO: Think this: Do we need a bandwidth constraint?
 */
public class PacketPathFlow extends PathFlow {
    private int hardTimeout;
    private int idleTimeout;

    /**
     * Constructor.
     *
     * @param id ID for this new Flow object.
     * @param match Match object at the source node of the path.
     * @param inPort Ingress port number at the ingress edge node.
     * @param path Path between ingress and egress edge node.
     * @param edgeActions The list of Action objects at the egress edge node.
     * @param hardTimeout hard-timeout value.
     * @param idleTimeout idle-timeout value.
     */
    public PacketPathFlow(String id,
            PacketMatch match, PortNumber inPort, Path path, List<Action> edgeActions,
            int hardTimeout, int idleTimeout) {
        super(id, match, inPort, path, edgeActions);
        this.hardTimeout = hardTimeout;
        this.idleTimeout = idleTimeout;
    }

    /**
     * Gets idle-timeout value.
     *
     * @return Idle-timeout value (seconds)
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Gets hard-timeout value.
     *
     * @return Hard-timeout value (seconds)
     */
    public int getHardTimeout() {
        return hardTimeout;
    }

    @Override
    public MatchActionOperations compile() {
        // TODO Auto-generated method stub
        return super.compile();
    }
}
