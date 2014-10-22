package net.floodlightcontroller.core;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.projectfloodlight.openflow.types.TableId;

import com.google.common.primitives.Longs;


public interface IOF13Switch extends IOFSwitch {

    // **************************
    // Flow related
    // **************************

    /**
     * Pushes a single flow to the switch as described by the match-action
     * operation and match-action definition, and subject to the TTP supported
     * by a switch implementing this interface. It is up to the implementation
     * to translate the 'matchActionOp' into a match-instruction with actions,
     * as expected by OF 1.3 switches. For better performance, use
     * {@link pushFlows}
     *
     * @param matchActionOp information required to create a flow-mod and push
     *        it to the switch
     * @throws IOException
     */
    public void pushFlow(MatchActionOperationEntry matchActionOp) throws IOException;

    /**
     * Pushes a collection of flows to the switch, at the same time. Can result
     * in better performance, when compared to sending flows one at a time using
     * {@link pushFlow}, especially if the number of flows is large.
     *
     * @param matchActionOps a collection of information required to create a
     *        flowmod
     * @throws IOException
     */
    public void pushFlows(Collection<MatchActionOperationEntry> matchActionOps)
            throws IOException;


    // ****************************
    // Group related
    // ****************************

    /**
     * Representation of a set of neighbor switch dpids along with edge node
     * label. Meant to be used as a lookup-key in a hash-map to retrieve an
     * ECMP-group that hashes packets to a set of ports connecting to the
     * neighbors in this set.
     */
    public class NeighborSet {
        Set<Dpid> dpids;
        int edgeLabel;

        /**
         * Constructor
         *
         * @param dpids A variable number of Dpids represention neighbor
         *        switches
         */
        public NeighborSet(Dpid... dpids) {
            this.edgeLabel = -1;
            this.dpids = new HashSet<Dpid>();
            for (Dpid d : dpids) {
                this.dpids.add(d);
            }
        }

        public void addDpid(Dpid d) {
            dpids.add(d);
        }

        public void addDpids(Set<Dpid> d) {
            dpids.addAll(d);
        }

        public void setEdgeLabel(int edgeLabel) {
            this.edgeLabel = edgeLabel;
        }

        public Set<Dpid> getDpids() {
            return dpids;
        }

        public int getEdgeLabel() {
            return edgeLabel;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NeighborSet)) {
                return false;
            }
            NeighborSet that = (NeighborSet) o;
            return (this.dpids.equals(that.dpids) && (this.edgeLabel == that.edgeLabel));
        }

        @Override
        public int hashCode() {
            int result = 17;
            for (Dpid d : dpids) {
                result = 31 * result + Longs.hashCode(d.value());
            }
            result = 31 * result + Longs.hashCode(edgeLabel);
            return result;
        }

        @Override
        public String toString() {
            return " Neighborset Sw: " + dpids + " and Label: " + edgeLabel;
        }
    }

    /**
     * Get the ECMP group-id for the ECMP group in this switch that includes
     * ports that connect to the neighbor-switches included in the NeighborSet
     * 'ns'
     *
     * @param ns the set of Neighbor Dpids
     * @return the ecmp group id, or -1 if no such group exists
     */
    public int getEcmpGroupId(NeighborSet ns);

    public void removePortFromGroups(PortNumber port);

    public void addPortToGroups(PortNumber port);

    /**
     * give string tableType (ip, mpls, acl)
     * @param tableType  String equal to only one of (ip, mpls, acl)
     * @return TableId
     */
    public TableId getTableId(String tableType);

    /**
     * Create a tunnel for policy routing
     *
     * @param tunnelId tunnel ID for the tunnel
     * @param route list of router DPIDs for the tunnel
     * @param ns NeighborSet to get to the first router of the tunnel
     */
    public void createTunnel(String tunnelId, List<String> route, NeighborSet ns);

    /**
     * Remove all groups for the tunnel
     *
     * @param tunnelId tunnel ID to remove
     */
    public void removeTunnel(String tunnelId);

    /**
     * Return the first group ID for the tunnel.
     * If the router is not the source of the tunnel, it returns -1
     *
     * @param tunnelID tunnel ID for the tunnel
     * @param srcDpid source router DPID
     * @return first Group ID for the tunnel or -1 if not found
     */
    public int getTunnelGroupId(String tunnelID);

    public Map<String, String> getPublishAttributes();
}
