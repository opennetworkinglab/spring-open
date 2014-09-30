package net.floodlightcontroller.core;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.util.Dpid;

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
     * Representation of a set of neighbor switch dpids. Meant to be used as a
     * lookup-key in a hash-map to retrieve an ECMP-group that hashes packets to
     * a set of ports connecting to the neighbors in this set.
     */
    public class NeighborSet {
        Set<Dpid> dpids;

        /**
         * Constructor
         *
         * @param dpids A variable number of Dpids represention neighbor
         *        switches
         */
        public NeighborSet(Dpid... dpids) {
            this.dpids = new HashSet<Dpid>();
            for (Dpid d : dpids) {
                this.dpids.add(d);
            }
        }

        public void addDpid(Dpid d) {
            dpids.add(d);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NeighborSet)) {
                return false;
            }
            NeighborSet that = (NeighborSet) o;
            return this.dpids.equals(that.dpids);
        }

        @Override
        public int hashCode() {
            int result = 17;
            for (Dpid d : dpids) {
                result = 31 * result + Longs.hashCode(d.value());
            }
            return result;
        }

        @Override
        public String toString() {
            return " Sw: {} Neighbors: " + dpids;
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


}
