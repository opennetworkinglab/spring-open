package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingPolicyAvoid extends SegmentRoutingPolicy {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingPolicyTunnel.class);

    private String srcDpid;
    private String dstDpid;
    private List<String> dpidListToAvoid;
    private List<Link> linkListToAvoid;
    private List<SegmentRoutingTunnel> tunnels;

    public SegmentRoutingPolicyAvoid(PolicyNotification policyNotication) {
        super(policyNotication);
        // TODO Auto-generated constructor stub
    }

    public SegmentRoutingPolicyAvoid(SegmentRoutingManager srm, String pid,
            PacketMatch match, int priority, String from, String to,
            List<String> dpidList, List<Link> linksToAvoid) {
        super(srm, pid, PolicyType.AVOID, match, priority);
        this.srcDpid = from;
        this.dstDpid = to;
        this.dpidListToAvoid = dpidList;
        this.linkListToAvoid = linksToAvoid;
        this.tunnels = new ArrayList<SegmentRoutingTunnel>();
    }

    public SegmentRoutingPolicyAvoid(SegmentRoutingManager srm,
            PolicyNotification pn) {
        super(srm, pn.getPolicyId(), PolicyType.AVOID, pn.getPacketMatch(),
                pn.getPriority());
        this.srcDpid = pn.getSource();
        this.srcDpid     = pn.getDestination();
        this.dpidListToAvoid = pn.getDpidListToAvoid();
        this.linkListToAvoid = new ArrayList<Link>();
        for (LinkData linkData: pn.getLinkListToAvoid()) {
            Switch sw =
                    srManager.getSwitch(linkData.getSrc().getDpid().toString());
            Link link = sw.getLinkToNeighbor(linkData.getDst().getDpid());
            linkListToAvoid.add(link);
        }
        this.tunnels = new ArrayList<SegmentRoutingTunnel>();
        for (TunnelNotification tn: pn.getTunnels()) {
            SegmentRoutingTunnel srTunnel = new SegmentRoutingTunnel(srm, tn);
            tunnels.add(srTunnel);
        }
    }

    @Override
    public boolean createPolicy() {

        //Create a tunnel from srcSwitch to dstSwitch avoiding swToAvoid;
        Switch srcSwitch = srManager.getSwitch(srcDpid);
        Switch dstSwitch = srManager.getSwitch(dstDpid);
        ECMPShortestPathGraph graph = new ECMPShortestPathGraph(srcSwitch,
                dpidListToAvoid, linkListToAvoid);
        List<Path> ecmpPaths = graph.getECMPPaths(dstSwitch);

        for (Path path: ecmpPaths) {
            List<Integer> labelStack = new ArrayList<Integer>();
            for (int i=path.size()-1; i >=0; i--) {
                LinkData link = path.get(i);
                String dpid = link.getSrc().getDpid().toString();
                labelStack.add(Integer.valueOf(srManager.getMplsLabel(dpid)));
            }
            String dstDpid = path.get(0).getDst().getDpid().toString();
            labelStack.add(Integer.valueOf(srManager.getMplsLabel(dstDpid)));
            //String nodeToAvoid = srManager.getMplsLabel(switchToAvoid.getDpid().toString());
            OptimizeLabelStack(labelStack);
            SegmentRoutingTunnel tunnel = new SegmentRoutingTunnel(
                    srManager, "avoid-0", labelStack);
            tunnels.add(tunnel);
            if (tunnel.createTunnel()) {
                populateAclRule(tunnel.getRoutes());
            }
            else {
                log.warn("Failed to create a tunnel for Policy Avoid");
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean removePolicy() {
        for (SegmentRoutingTunnel tunnel: tunnels) {
            if (tunnel.removeTunnel()) {
                removeAclRules(tunnel.getRoutes());
            }
            else {
                log.warn("Error in removing an avoid policy");
                return false;
            }
        }

        tunnels.removeAll(tunnels);
        return true;
    }

    @Override
	public boolean updatePolicy() {
        Switch srcSwitch = srManager.getSwitch(srcDpid);
        Switch dstSwitch = srManager.getSwitch(dstDpid);
        ECMPShortestPathGraph graph = new ECMPShortestPathGraph(srcSwitch,
                dpidListToAvoid, linkListToAvoid);
        List<Path> ecmpPaths = graph.getECMPPaths(dstSwitch);

        // Check if it needs update or not
        boolean needUpdate = false;
        for (Path path: ecmpPaths) {
            List<Integer> labelStack = new ArrayList<Integer>();
            for (int i=path.size()-1; i >=0; i--) {
                LinkData link = path.get(i);
                String dpid = link.getSrc().getDpid().toString();
                labelStack.add(Integer.valueOf(srManager.getMplsLabel(dpid)));
            }
            String dstDpid = path.get(0).getDst().getDpid().toString();
            labelStack.add(Integer.valueOf(srManager.getMplsLabel(dstDpid)));
            OptimizeLabelStack(labelStack);
            if (!checkIfIncluded(labelStack)) {
                needUpdate = true;
                break;
            }
        }

        if (needUpdate) {
            log.debug("Need to update the policy {}", policyId);
            removePolicy();
            createPolicy();
        }
        else {
            log.debug("No need to update the policy {}, policyId");
        }
        
        return true;
    }

    private boolean checkIfIncluded(List<Integer> labelStack) {

        for (SegmentRoutingTunnel tunnel: tunnels) {
            if (tunnel.getLabelids().size() != labelStack.size())
                continue;

            int i = 0;
            boolean identical = true;
            for (Integer id: tunnel.getLabelids()) {
                if (id != labelStack.get(i++)) {
                    identical = false;
                    break;
                }
            }
            if (!identical)
                continue;
            else {
                return true;
            }
        }

        return false;
    }

    /**
     * Optimize the label stack removing unnecessary label IDs, resulting the
     * same path. It modifies the list given directly.
     *
     * @param labelStack List of label IDs
     */
    private void OptimizeLabelStack(List<Integer> labelStack) {

        // {101, 103, 104, 106}
        // source = 101
        // destination = 106
        // testNode = 104
        // nodeToAvoid = 105

        // check if 101 -> 104 includes 105
        // No -> connect directly from 101 to 104. Stop
        // Yes -> testNode = 103, repeat the check

        Switch srcNode = srManager.getSwitchFromNodeId(
                labelStack.get(0).toString());

        int i = 2;
        boolean violated = false;
        Switch nodeToCheck = srManager.getSwitchFromNodeId(
                labelStack.get(labelStack.size()-i).toString());
        ECMPShortestPathGraph ecmpGraph = new ECMPShortestPathGraph(srcNode);
        while (!nodeToCheck.getDpid().toString().equals(srcNode.getDpid().toString())) {
            List<Path> paths = ecmpGraph.getECMPPaths(nodeToCheck);
            for (Path path: paths) {
                for (LinkData link: path) {
                    if (dpidListToAvoid.contains(
                            link.getSrc().getDpid().toString())
                            || linkContains(link, linkListToAvoid)) {
                        violated = true;
                        break;
                    }
                }
                if (violated)
                    break;
            }
            if (violated) {
                i++;
                nodeToCheck = srManager.getSwitchFromNodeId(
                        labelStack.get(labelStack.size()-i).toString());
            }
            // remove the rest of the label Ids and stop here
            else {
                for (int j=1; j<i; j++) {
                    labelStack.remove(j);
                    return;
                }
            }
        }
    }

    private boolean linkContains(LinkData link, List<Link> links) {

        Dpid srcSwitch1 = link.getSrc().getDpid();
        Dpid dstSwitch1 = link.getDst().getDpid();
        long srcPort1 = link.getSrc().getPortNumber().value();
        long dstPort1 = link.getDst().getPortNumber().value();

        for (Link link2: links) {
            Switch srcSwitch2 = link2.getSrcSwitch();
            Switch dstSwitch2 = link2.getDstSwitch();
            long srcPort2 = link2.getSrcPort().getPortNumber().value();
            long dstPort2 = link2.getDstPort().getPortNumber().value();

            if (srcSwitch1.toString().equals(srcSwitch2.getDpid().toString())
             && dstSwitch1.toString().equals(dstSwitch2.getDpid().toString())
             && srcPort1 == srcPort2 && dstPort1 == dstPort2)
                return true;
        }

        return false;
    }

    public String getSource() {
        return this.srcDpid;
    }

    public String getDestination() {
        return this.dstDpid;
    }

    public List<String> getDpidListToAvoid() {
        return this.dpidListToAvoid;
    }

    public List<Link> getLinkListToAvoid() {
        return this.linkListToAvoid;
    }

    public List<SegmentRoutingTunnel> getTunnels() {
        return this.tunnels;
    }
}
