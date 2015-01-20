package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOF13Switch.GroupChainParams;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.onrc.onos.core.drivermanager.OFSwitchImplDellOSR;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingTunnel {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingTunnel.class);

    private String tunnelId;
    private List<Integer> labelIds;
    private List<TunnelRouteInfo> routes;
    private SegmentRoutingManager srManager;
    private String tunnelsetId;

    private final int MAX_NUM_LABELS = 3;

    /**
     * Constructor
     *
     * @param srm SegmentRoutingManager object
     * @param tid  Tunnel ID
     * @param labelIds Label stack of the tunnel
     */
    public SegmentRoutingTunnel(SegmentRoutingManager srm, String tid,
            List<Integer> labelIds) {
        this.srManager = srm;
        this.tunnelId = tid;
        this.labelIds = labelIds;
        this.setTunnelsetId(null);
        this.routes = new ArrayList<TunnelRouteInfo>();
    }

    public SegmentRoutingTunnel(SegmentRoutingManager srm, String tid,
            List<Integer> labelIds, String tunnelsetId) {
        this.srManager = srm;
        this.tunnelId = tid;
        this.labelIds = labelIds;
        this.setTunnelsetId(tunnelsetId);
        this.routes = new ArrayList<TunnelRouteInfo>();
    }

    /**
     * Constructor
     *
     * @param srm SegmentRoutingManager objeect
     * @param tunnelNotification tunnel information published by other controllers
     */
    public SegmentRoutingTunnel(SegmentRoutingManager srm,
            TunnelNotification tunnelNotification) {
        this.srManager = srm;
        this.tunnelId = tunnelNotification.getTunnelId();
        this.labelIds = tunnelNotification.getLabelIds();
        this.routes = tunnelNotification.getRouteInfo();
    }

    /**
     * Check if there is any sub tunnel that starts with the router managed by
     * the controller. If so, create groups for the sub tunnel and set the
     * group Id list. Then, set modified flag to true so that the tunnel
     * information is published again with the new group ID list.
     *
     * @return true if any new group is created, otherwise false.
     */
    public boolean checkAndCreateTunnel() {

        boolean modified = false;
        for (TunnelRouteInfo route: routes) {
            if (srManager.getIOF13Switch(route.getSrcSwDpid()) != null) {
                if (route.getGroupIdList() == null) {
                    NeighborSet ns = new NeighborSet();
                    for (Dpid dpid: route.getFwdSwDpid())
                        ns.addDpid(dpid);

                    printTunnelInfo(route.srcSwDpid, tunnelId, route.getRoute(), ns);
                    List<Integer> groupIdList = createGroupsForTunnel(tunnelId, route, ns);
                    if (groupIdList == null) {
                        log.debug("Failed to create a tunnel at driver.");
                        return false;
                    }
                    route.setGroupIdList(groupIdList);
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Get tunnel ID
     *
     * @return tunnel ID
     */
    public String getTunnelId(){
        return this.tunnelId;
    }

    /**
     * Get Nodes IDs for the tunnel including source and destination router
     *
     * @return List of Node ID
     */
    public List<Integer> getLabelids() {
        return this.labelIds;
    }

    /**
     * Get tunnel information after stitching if necessary
     *
     * @return List of TunnelRouteInfo object
     */
    public List<TunnelRouteInfo> getRoutes(){
        return this.routes;
    }
    
    public HashMap<String, GroupChainParams> getGroupChainParams() {
    	HashMap<String, GroupChainParams> groupChainParamList = 
    			new HashMap<String, GroupChainParams>();
        for (TunnelRouteInfo route: routes) {
            List<Integer> Ids = new ArrayList<Integer>();
            for (String IdStr: route.route)
                Ids.add(Integer.parseInt(IdStr));
            NeighborSet ns = new NeighborSet();
            for (Dpid dpid: route.getFwdSwDpid())
                ns.addDpid(dpid);
            List<PortNumber> ports = getPortsFromNeighborSet(route.srcSwDpid, ns);
        	GroupChainParams groupChainParams = new GroupChainParams(tunnelId, Ids, ports);
        	groupChainParamList.put(route.srcSwDpid, groupChainParams);
        }
        
        return groupChainParamList;
    }
    /**
     * Create a tunnel
     * It requests the driver to create a group chaining for the tunnel.
     *
     * @return true if succeeds, false otherwise
     */
    public boolean createTunnel() {

        if (labelIds.isEmpty() || labelIds.size() < 2) {
            log.debug("Wrong tunnel information");
            return false;
        }

        List<String> Ids = new ArrayList<String>();
        for (Integer label : labelIds) {
            Ids.add(label.toString());
        }

        List<TunnelRouteInfo> stitchingRule = getStitchingRule(Ids);
        if (stitchingRule == null) {
            log.debug("Failed to get a tunnel rule.");
            return false;
        }

        // Rearrange the tunnels if the last subtunnel does not have any label
        // NOTE: this is only for DELL switches because all ACL rule needs PUSH
        // Label Action.
        checkAndSplitLabels(stitchingRule);

        for (TunnelRouteInfo route: stitchingRule) {

            if (srManager.getIOF13Switch(route.getSrcSwDpid()) != null) {
                NeighborSet ns = new NeighborSet();
                for (Dpid dpid: route.getFwdSwDpid())
                    ns.addDpid(dpid);

                printTunnelInfo(route.srcSwDpid, tunnelId, route.getRoute(), ns);
                List<Integer> groupIdList = null;
                if ((groupIdList =createGroupsForTunnel(tunnelId, route, ns)) == null) {
                    log.debug("Failed to create a tunnel at driver.");
                    return false;
                }
                route.setGroupIdList(groupIdList);
            }
            else {
                route.setGroupIdList(null);
            }
        }

        this.routes = stitchingRule;

        return true;
    }

    public boolean computeTunnelLabelStack() {

        if (labelIds.isEmpty() || labelIds.size() < 2) {
            log.debug("Wrong tunnel information");
            return false;
        }

        List<String> Ids = new ArrayList<String>();
        for (Integer label : labelIds) {
            Ids.add(label.toString());
        }

        List<TunnelRouteInfo> stitchingRule = getStitchingRule(Ids);
        if (stitchingRule == null) {
            log.debug("Failed to get a tunnel rule.");
            return false;
        }

        // Rearrange the tunnels if the last subtunnel does not have any label
        // NOTE: this is only for DELL switches because all ACL rule needs PUSH
        // Label Action.
        checkAndSplitLabels(stitchingRule);

        for (TunnelRouteInfo route: stitchingRule) {
        	route.setGroupIdList(null);
        }

        this.routes = stitchingRule;

        return true;
    }

    /**
     * Check if last sub tunnel rule label stack is empty.
     * If so, move a label of previous tunnel to the last sub-tunnel.
     * It directly updates the tunnel information (stitchingRule) given.
     * NOTE: This workaroud is required only for Dell Switch restriction.
     *
     * @param stitchingRule tunnel information
     */
    private void checkAndSplitLabels(List<TunnelRouteInfo> stitchingRule) {

        if (stitchingRule.size() < 2) {
            return;
        }
        TunnelRouteInfo lastSubTunnel = stitchingRule.get(stitchingRule.size()-1);
        if (!lastSubTunnel.getRoute().isEmpty()) {
            return;
        }
        TunnelRouteInfo lastToSecond = stitchingRule.get(stitchingRule.size()-2);
        if (lastToSecond == null) {
            return; // Something wrong here
        }
        String lastLabelId =
                lastToSecond.getRoute().get(lastToSecond.getRoute().size()-1);
        String newStitchingRouterId =
                lastToSecond.getRoute().get(lastToSecond.getRoute().size()-2);

        // Needs to convert any adjacency Sid to node Sid
        if (srManager.isAdjacencySid(newStitchingRouterId)) {
            String orgNodeSid =
                    lastToSecond.getRoute().get(lastToSecond.getRoute().size()-3);
            if (srManager.isAdjacencySid(orgNodeSid)) {
                String firstLabelOrgNodeSid =
                        srManager.getMplsLabel(lastToSecond.getFwdSwDpid().get(0).toString());
                List<Switch> destNodes = getAdjacencyDestinationNode(firstLabelOrgNodeSid, orgNodeSid);
                orgNodeSid = srManager.getMplsLabel(destNodes.get(0).getDpid().toString());
            }
            List<Switch> destNodes = getAdjacencyDestinationNode(orgNodeSid, newStitchingRouterId);
            newStitchingRouterId = srManager.getMplsLabel(destNodes.get(0).getDpid().toString());
        }
        Switch newSitchingSwitch = srManager.getSwitchFromNodeId(newStitchingRouterId);
        // In this case, # of fwd Sws must be only one
        String newLabelId =
                srManager.getMplsLabel(lastSubTunnel.getFwdSwDpid().get(0).toString());
        List<Dpid> newFwdSws = null;
        if (srManager.isAdjacencySid(lastLabelId)) {
            List<Switch> destSwitches = getAdjacencyDestinationNode(newStitchingRouterId, lastLabelId);
            String orgLastLabelId = srManager.getMplsLabel(destSwitches.get(0).toString());
            newFwdSws =
                    srManager.getForwardingSwitchForNodeId(newSitchingSwitch, orgLastLabelId);
        }
        else {
            newFwdSws =
                srManager.getForwardingSwitchForNodeId(newSitchingSwitch, lastLabelId);
        }

        // Remove the last ID from the last-to-second sub-tunnel
        lastToSecond.getRoute().remove(lastLabelId);

        // Reset the src switch
        lastSubTunnel.setSrcDpid(srManager.getSwitchFromNodeId(
                newStitchingRouterId).getDpid().toString());
        // Reset the fwd nodes
        lastSubTunnel.setFwdSwDpid(newFwdSws);
        // Add the new Label Id
        lastSubTunnel.addRoute(newLabelId);

    }

    /**
     * Remove the tunnel.
     * It requests driver to remove all groups for the tunnel
     *
     * @return true if succeeds, false otherwise.
     */
    public boolean removeTunnel() {

        for (TunnelRouteInfo route: routes) {
            IOF13Switch sw13 = srManager.getIOF13Switch(route.srcSwDpid);
            if (sw13 != null) {
                // Group needs to be removed in reverse order because
                // the group being pointed by any other group cannot be removed
                for (int i = route.getGroupIdList().size()-1; i >= 0; i--) {
                    int groupId = route.getGroupIdList().get(i);
                    if (!sw13.removeGroup(groupId)) {
                        log.warn("Faied to remove the tunnel {} at driver",
                                tunnelId);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Create groups for the tunnel
     *
     * @param tunnelId tunnel ID
     * @param routeInfo label stacks for the tunnel
     * @param ns NeighborSet to forward packets
     * @return group ID, return -1 if it fails
     */
    private List<Integer> createGroupsForTunnel(String tunnelId, TunnelRouteInfo routeInfo,
            NeighborSet ns) {

        IOF13Switch targetSw = srManager.getIOF13Switch(routeInfo.srcSwDpid);

        if (targetSw == null) {
            log.debug("Switch {} is gone.", routeInfo.srcSwDpid);
            return null;
        }

        List<Integer> Ids = new ArrayList<Integer>();
        for (String IdStr: routeInfo.route)
            Ids.add(Integer.parseInt(IdStr));

        List<PortNumber> ports = getPortsFromNeighborSet(routeInfo.srcSwDpid, ns);
        List<Integer> groupIdList = targetSw.createGroup(Ids, ports);

        return groupIdList;
    }

    /**
     * Split the nodes IDs into multiple tunnel if Segment Stitching is required.
     * We assume that the first node ID is the one of source router, and the last
     * node ID is that of the destination router.
     *
     * @param route list of node IDs
     * @return List of the TunnelRoutInfo
     */
    private List<TunnelRouteInfo> getStitchingRule(List<String> route) {

        if (route.isEmpty() || route.size() < 3)
            return null;

        List<TunnelRouteInfo> rules = new ArrayList<TunnelRouteInfo>();

        Switch srcSw = srManager.getSwitchFromNodeId(route.get(0));
        if (srcSw == null) {
            log.warn("Switch is not found for Node SID {}", route.get(0));
            return null;
        }
        String srcDpid = srcSw.getDpid().toString();

        int i = 0;
        TunnelRouteInfo routeInfo = new TunnelRouteInfo();
        boolean checkNeighbor = false;
        String prevAdjacencySid = null;
        String prevNodeId = null;

        for (String nodeId: route) {
            // The first node ID is always the source router.
            // We assume that the first ID cannot be an Adjacency SID.
            if (i == 0) {
                srcSw = srManager.getSwitchFromNodeId(nodeId);
                if (srcDpid == null)
                    srcDpid = srcSw.getDpid().toString();
                routeInfo.setSrcDpid(srcDpid);
                checkNeighbor = true;
                i++;
            }
            // if this is the first node ID to put the label stack..
            else if (i == 1) {
                // If the adjacency SID is pushed and the next SID is the destination
                // of the adjacency SID, then do not add the SID.
                if (prevAdjacencySid != null) {
                    if (isAdjacencySidNeighborOf(prevNodeId, prevAdjacencySid, nodeId)) {
                        prevAdjacencySid = null;
                        prevNodeId = nodeId;
                        continue;
                    }
                    prevAdjacencySid = null;
                }

                if (checkNeighbor) {
                    List<Dpid> fwdSws = getDpidIfNeighborOf(nodeId, srcSw);
                    // if nodeId is NOT the neighbor of srcSw..
                    if (fwdSws.isEmpty()) {
                        fwdSws = srManager.getForwardingSwitchForNodeId(srcSw,nodeId);
                        if (fwdSws == null || fwdSws.isEmpty()) {
                            log.warn("There is no route from node {} to node {}",
                                    srcSw.getDpid(), nodeId);
                            return null;
                        }
                        routeInfo.addRoute(nodeId);
                        i++;
                    }
                    routeInfo.setFwdSwDpid(fwdSws);
                    // we check only the next node ID of the source router
                    checkNeighbor = false;
                }
                // if neighbor check is already done, then just add it
                else  {
                    routeInfo.addRoute(nodeId);
                    i++;
                }
            }
            // if i > 1
            else {
                // If the adjacency SID is pushed and the next SID is the destination
                // of the adjacency SID, then do not add the SID.
                if (prevAdjacencySid != null) {
                    if (isAdjacencySidNeighborOf(prevNodeId, prevAdjacencySid, nodeId)) {
                        prevAdjacencySid = null;
                        prevNodeId = nodeId;
                        continue;
                    }
                    prevAdjacencySid = null;
                }
                routeInfo.addRoute(nodeId);
                i++;
            }

            // If the adjacency ID is added the label stack,
            // then we need to check if the next node is the destination of the adjacency SID
            if (srManager.isAdjacencySid(nodeId))
                prevAdjacencySid = nodeId;

            // If the number of labels reaches the limit, start over the procedure
            if (i == MAX_NUM_LABELS +1) {

                rules.add(routeInfo);
                routeInfo = new TunnelRouteInfo();

                if (srManager.isAdjacencySid(nodeId)) {
                    // If the previous sub tunnel finishes with adjacency SID,
                    // then we need to start the procedure from the adjacency
                    // destination ID.
                    List<Switch> destNodeList =
                            getAdjacencyDestinationNode(prevNodeId, nodeId);
                    if (destNodeList == null || destNodeList.isEmpty()) {
                        log.warn("Cannot find destination node for adjacencySID {}",
                                nodeId);
                        return null;
                    }
                    // If the previous sub tunnel finishes with adjacency SID with
                    // multiple ports, then we need to remove the adjacency Sid
                    // from the previous sub tunnel and start the new sub tunnel
                    // with the adjacency Sid. Technically, the new subtunnel
                    // forward packets to the port assigned to the adjacency Sid
                    // and the label stack starts with the next ID.
                    // This is to avoid to install new policy rule to multiple nodes for stitching when the
                    // adjacency Sid that has more than one port.
                    if (destNodeList.size() > 1) {
                        rules.get(rules.size()-1).route.remove(nodeId);
                        srcSw = srManager.getSwitchFromNodeId(prevNodeId);
                        List<Dpid> fwdSws = getDpidIfNeighborOf(nodeId, srcSw);
                        routeInfo.setFwdSwDpid(fwdSws);
                        routeInfo.setSrcDpid(srcSw.getDpid().toString());
                        i = 1;
                        checkNeighbor = false;
                        continue;
                    }
                    else {
                        srcSw = destNodeList.get(0);
                    }
                }
                else {
                    srcSw = srManager.getSwitchFromNodeId(nodeId);
                }
                srcDpid = srcSw.getDpid().toString();
                routeInfo.setSrcDpid(srcDpid);
                i = 1;
                checkNeighbor = true;
            }

            if (prevAdjacencySid == null)
                prevNodeId = nodeId;
        }


        if (i < MAX_NUM_LABELS+1 && (routeInfo.getFwdSwDpid() != null &&
                !routeInfo.getFwdSwDpid().isEmpty())) {
            rules.add(routeInfo);
            // NOTE: empty label stack can happen, but forwarding destination should be set
        }

        return rules;
    }

    /**
     * Get port numbers of the neighbor set.
     * If ECMP in transit router is not supported, then only one port should be returned
     * regardless of number of nodes in neighbor set.
     *
     * @param srcSwDpid source switch
     * @param ns Neighbor set of the switch
     * @return List of PortNumber, null if not found
     */
    private List<PortNumber> getPortsFromNeighborSet(String srcSwDpid, NeighborSet ns) {

        List<PortNumber> portList = new ArrayList<PortNumber>();
        Switch srcSwitch = srManager.getSwitch(srcSwDpid);
        if (srcSwitch == null)
            return null;
        IOF13Switch srcSwitch13 =
                srManager.getIOF13Switch(srcSwitch.getDpid().toString());

        for (Dpid neighborDpid: ns.getDpids()) {
            if (srcSwitch13 instanceof OFSwitchImplDellOSR &&
                    ns.getDpids().size() == 1) {
                Switch dstSwitch = srManager.getSwitch(neighborDpid.toString());
                if (srManager.isTransitRouter(srcSwitch) &&
                        srManager.isTransitRouter(dstSwitch)) {
                    Link link = srcSwitch.getLinkToNeighbor(neighborDpid);
                    portList.add(link.getSrcPort().getNumber());
                    break;
                }
            }
            else {
                for (Link link: srcSwitch.getOutgoingLinks()) {
                    if (link.getDstSwitch().getDpid().equals(neighborDpid)) {
                        portList.add(link.getSrcPort().getNumber());
                    }
                }
            }
        }

        return portList;
    }

    /**
     * Get the DPID of the router with node ID IF the node ID is the neighbor of the
     * Switch srcSW.
     * If the nodeId is the adjacency Sid, then it returns the destination router DPIDs.
     *
     * @param nodeId Node ID to check
     * @param srcSw target Switch
     * @return List of DPID of nodeId, empty list if the nodeId is not the neighbor of srcSW
     */
    private List<Dpid> getDpidIfNeighborOf(String nodeId, Switch srcSw) {
        List<Dpid> fwdSws = new ArrayList<Dpid>();
        // if the nodeID is the adjacency ID, then we need to regard it as the
        // neighbor node ID and need to return the destination router DPID(s)
        if (srManager.isAdjacencySid(nodeId)) {
            String srcNodeId = srManager.getMplsLabel(srcSw.getDpid().toString());
            List<Integer> ports =
                    srManager.getAdacencyPorts(Integer.parseInt(srcNodeId),
                            Integer.parseInt(nodeId));

            for (Integer port: ports) {
                for (Link link: srcSw.getOutgoingLinks()) {
                    if (link.getSrcPort().getPortNumber().value() == port) {
                        fwdSws.add(link.getDstSwitch().getDpid());
                    }
                }
            }
        }
        else {
            List<Dpid> fwdSwDpids =
                    srManager.getForwardingSwitchForNodeId(srcSw,nodeId);
            if (fwdSwDpids == null || fwdSwDpids.isEmpty()) {
                log.warn("There is no route from node {} to node {}",
                        srcSw.getDpid(), nodeId);
                return fwdSws;
            }

            for (Dpid dpid: fwdSwDpids) {
                String id = srManager.getMplsLabel(dpid.toString()).toString();
                if (id.equals(nodeId)) {
                    fwdSws.add(dpid);
                    break;
                }
            }
        }

        return fwdSws;
    }

    /**
     * Check whether the router with preNodeid is connected to the router
     * with nodeId via adjacencySid or not
     *
     * @param prevNodeId the router node ID of the adjacencySid
     * @param adjacencySid adjacency SID
     * @param nodeId the router node ID to check
     * @return
     */
    private boolean isAdjacencySidNeighborOf(String prevNodeId, String adjacencySid, String nodeId) {

        List<Integer> ports =
                srManager.getAdacencyPorts(Integer.valueOf(prevNodeId),
                        Integer.valueOf(adjacencySid));

        if (ports == null) {
            log.warn("Cannot find ports for node ID {} and adjacencySID ",
                    prevNodeId, adjacencySid);
            return false;
        }

        for (Integer port: ports) {
            Switch sw = srManager.getSwitchFromNodeId(prevNodeId);
            for (Link link: sw.getOutgoingLinks()) {
                if (link.getSrcPort().getPortNumber().value() == port) {
                    String linkDstDpid = link.getDstPort().getDpid().toString();
                    String linkDstId = srManager.getMplsLabel(linkDstDpid);
                    if (linkDstId.equals(nodeId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get the destination Nodes of the adjacency Sid
     *
     * @param nodeId  node ID of the adjacency Sid
     * @param adjacencySid  adjacency Sid
     * @return List of Switch, empty list if not found
     */
    private List<Switch> getAdjacencyDestinationNode(String nodeId, String adjacencySid) {
        List<Switch> dstSwList = new ArrayList<Switch>();

        List<Integer> ports = srManager.getAdacencyPorts(Integer.valueOf(nodeId),
                Integer.valueOf(adjacencySid));

        Switch srcSw = srManager.getSwitchFromNodeId(nodeId);
        for (Integer port: ports) {
            for (Link link: srcSw.getOutgoingLinks()) {
                if (link.getSrcPort().getPortNumber().value() == port) {
                    dstSwList.add(link.getDstSwitch());
                }
            }
        }

        return dstSwList;

    }

    /**
     * print tunnel info - used only for debugging.
     * @param targetSw
     *
     * @param fwdSwDpids
     * @param ids
     * @param tunnelId
     */
    private void printTunnelInfo(String targetSw, String tunnelId,
            List<String> ids, NeighborSet ns) {
        StringBuilder logStr = new StringBuilder("In switch " +
                targetSw + ", create a tunnel " + tunnelId + " " + " of push ");
        for (String id: ids)
            logStr.append(id + "-");
        logStr.append(" output to ");
        for (Dpid dpid: ns.getDpids())
            logStr.append(dpid + " - ");

        log.debug(logStr.toString());

    }

	public String getTunnelsetId() {
		return tunnelsetId;
	}

	public void setTunnelsetId(String tunnelsetId) {
		this.tunnelsetId = tunnelsetId;
	}


}
