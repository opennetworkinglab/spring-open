package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final PacketMatch match;
    private final int hardTimeout;
    private final int idleTimeout;

    /**
     * Constructor.
     *
     * @param id ID for this new Flow object
     * @param match the Match object at the source node of the path
     * @param ingressPort the Ingress port number at the ingress edge node
     * @param path the Path between ingress and egress edge node
     * @param egressActions the list of Action objects at the egress edge node
     * @param hardTimeout the hard-timeout value in seconds, or 0 for no timeout
     * @param idleTimeout the idle-timeout value in seconds, or 0 for no timeout
     */
    public PacketPathFlow(FlowId id,
            PacketMatch match, PortNumber ingressPort, Path path,
            List<Action> egressActions,
            int hardTimeout, int idleTimeout) {
        super(id, ingressPort, path, egressActions);
        this.match = checkNotNull(match);
        this.hardTimeout = hardTimeout;
        this.idleTimeout = idleTimeout;
    }

    @Override
    public PacketMatch getMatch() {
        return match;
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
        return null;
    }
}
