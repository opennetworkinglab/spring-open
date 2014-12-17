package net.floodlightcontroller.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IOF13Switch.GroupChain;
import net.floodlightcontroller.core.IOF13Switch.GroupChainParams;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.projectfloodlight.openflow.types.MacAddress;
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
        public enum groupPktType {
            IP_OUTGOING,
            MPLS_OUTGOING;

            /**
             * Default constructor for Kryo deserialization.
             */
            @Deprecated
            groupPktType(){}
        };

        Set<Dpid> dpids;
        int edgeLabel;
        groupPktType outPktType;

        /**
         * Default constructor for Kryo deserialization.
         */
        public NeighborSet() {
            this.edgeLabel = -1;
            this.outPktType = groupPktType.IP_OUTGOING;
            this.dpids = new HashSet<Dpid>();
        }

        /**
         * Constructor
         *
         * @param dpids A variable number of Dpids represention neighbor
         *        switches
         */
        public NeighborSet(Dpid... dpids) {
            this.edgeLabel = -1;
            this.outPktType = groupPktType.IP_OUTGOING;
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
            if (edgeLabel > 0)
                this.outPktType = groupPktType.MPLS_OUTGOING;
        }

        public Set<Dpid> getDpids() {
            return dpids;
        }

        public int getEdgeLabel() {
            return edgeLabel;
        }

        public groupPktType getOutPktType() {
            return outPktType;
        }

        public void setOutPktType(groupPktType outPktType) {
            this.outPktType = outPktType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NeighborSet)) {
                return false;
            }
            NeighborSet that = (NeighborSet) o;
            return (this.dpids.equals(that.dpids) &&
                    (this.edgeLabel == that.edgeLabel) && (this.outPktType == that.outPktType));
        }

        @Override
        public int hashCode() {
            int result = 17;
            for (Dpid d : dpids) {
                result = 31 * result + Longs.hashCode(d.value());
            }
            result = 31 * result + Longs.hashCode(edgeLabel);
            result = 31 * result + outPktType.hashCode();
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

    /**
     * Remove the OFBucket that contains the specified port from all the OF
     * groups. This API can be used by applications, when a port goes down, to
     * remove that port from all the group that it is part of
     *
     * @param port Port Number to be removed from groups
     * @return None
     */
    public void removePortFromGroups(PortNumber port);

    /**
     * Add the OFBucket to groups that have new reachability through the given
     * port. This API can be used by applications, when a port is operational
     * again, to add that port to all the relevant groups NOTE1: This API will
     * add the specified port to any existing groups only if it is in Active
     * state. NOTE2: If there were never any groups existing with the neighbor
     * dpid that is reachable through this port, then this method performs
     * no-operation
     *
     * @param port Port Number to be added to groups
     * @return None
     */
    public void addPortToGroups(PortNumber port);

    /**
     * give string tableType (ip, mpls, acl)
     * @param tableType  String equal to only one of (ip, mpls, acl)
     * @return TableId
     */
    public TableId getTableId(String tableType);

    /**
     * Create a group chain with the specified label stack for a given set of
     * ports. This API can be used by user to create groups for a tunnel based
     * policy routing scenario. NOTE: This API can not be used if a group to be
     * created with different label stacks for each port in the given set of
     * ports. Use createGroupChain API for this purpose
     *
     * @param labelStack list of router segment Ids to be pushed. Can be empty.
     *        labelStack is processed from left to right with leftmost
     *        representing the outermost label and rightmost representing
     *        innermost label to be pushed
     * @param ports List of ports on this switch to get to the first router in
     *        the labelStack
     * @return group identifier
     */
    public List<Integer> createGroup(List<Integer> labelStack, List<PortNumber> ports);
    
    public class GroupChainParams {
    	private String id;
    	private List<PortNumber> ports;
    	private List<Integer> labelStack;
    	
    	public GroupChainParams(String id, List<Integer> labelStack, 
    			List<PortNumber> ports) {
    		this.setId(id);
    		this.setPorts(ports);
    		this.setLabelStack(labelStack);
    	}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<PortNumber> getPorts() {
			return ports;
		}

		public void setPorts(List<PortNumber> ports) {
			this.ports = ports;
		}

		public List<Integer> getLabelStack() {
			return labelStack;
		}

		public void setLabelStack(List<Integer> labelStack) {
			this.labelStack = labelStack;
		}
    }
    
    public class GroupChain {
    	private String id;
    	private HashMap<PortNumber, List<Integer>> groupChain;
    	private int innermostGroupId;
    	
    	public GroupChain(String id) {
    		this.setId(id);
    		groupChain = new HashMap<PortNumber,List<Integer>>();
    	}
    	
    	public void addGroupChain(PortNumber port, List<Integer> chain) {
    		this.groupChain.put(port, chain);
    	}
    	
    	public void addGroupToChain(PortNumber port, int groupId) {
    		List<Integer> chain = groupChain.get(port);
    		if (chain == null) {
    			chain = new ArrayList<Integer>();
    			groupChain.put(port, chain);
    		}
    		chain.add(groupId);
    	}
    	
    	public HashMap<PortNumber, List<Integer>> getGroupChain() {
    		return groupChain;
    	}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public int getInnermostGroupId() {
			return innermostGroupId;
		}

		public void setInnermostGroupId(int innermostGroupId) {
			this.innermostGroupId = innermostGroupId;
		}
    }
    
    /**
     * Create a group chain with the specified label stack for each given set of
     * ports. This API can be used by user to create groups for a tunnel based
     * policy routing scenario. 
     *
     * @param groupChainParams list of ports along with label stacks to be 
     *        applied. label stack can be empty.
     *        labelStack is processed from left to right with leftmost
     *        representing the outermost label and rightmost representing
     *        innermost label to be pushed
     *        List of ports on this switch to get to the first router in
     *        the labelStack
     * @return List of GroupChain objects
     */
    public List<GroupChain> createGroupChain(
    		List<GroupChainParams> groupChainParams);
    /**
     * Add a new entry to an existing group chain
     *
     * @param currentGroupChainList Existing group chain to which this 
     *        new entry to be added
     * @param groupChainParams list of ports along with label stacks to be 
     *        applied. label stack can be empty.
     *        labelStack is processed from left to right with leftmost
     *        representing the outermost label and rightmost representing
     *        innermost label to be pushed
     *        List of ports on this switch to get to the first router in
     *        the labelStack
     * @return Modified list of GroupChain objects
     */
    public List<GroupChain> addNewEntryToGroupChain(
    		List<GroupChain> currentGroupChainList, 
    		List<GroupChainParams> groupChainParams);
    /**
     * Remove one or more buckes from the given innermost group those 
     * point to the list of groups specified in chainedGroups list
     *
     * @param innermostGroupId The innermost group from which buckets, 
     *        pointing to the groupIds in the chainedGroups list param, 
     *        to be deleted
     * @param chainedGroups list of groupIds
     * @return True or False depending on the result of the removal
     */
    public boolean removeOutGroupBucketsFromGroup(
    		int innermostGroupId, List<Integer> chainedGroups);

    /**
     * Remove the specified group
     *
     * @param groupId group identifier
     * @return success/fail
     */
    public boolean removeGroup(int groupId);

    public Map<String, String> getPublishAttributes();

    /**
     * Get the specified Router's MAC address to be used for IP flows
     *
     * @param dpid Dpid of the router
     * @return MacAddress of specified router
     */
    public MacAddress getRouterIPMac(Dpid dpid);

    /**
     * Get the specified Router's MAC address to be used for MPLS flows
     *
     * @param dpid Dpid of the router
     * @return MacAddress of specified router
     */
    public MacAddress getRouterMPLSMac(Dpid dpid);
}
