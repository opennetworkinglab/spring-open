package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.Switch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingPolicyAvoid extends SegmentRoutingPolicy {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingPolicyTunnel.class);

    private Switch srcSwitch;
    private Switch dstSwitch;
    private Switch switchToAvoid;

    public SegmentRoutingPolicyAvoid(PolicyNotification policyNotication) {
        super(policyNotication);
        // TODO Auto-generated constructor stub
    }

    public SegmentRoutingPolicyAvoid(SegmentRoutingManager srm, String pid,
            PacketMatch match, int priority, Switch from, Switch to, Switch swToAvoid) {
        super(srm, pid, PolicyType.AVOID, match, priority);
        this.srcSwitch = from;
        this.dstSwitch = to;
        this.switchToAvoid = swToAvoid;
    }

    @Override
    public boolean createPolicy() {

        //Create a tunnel from srcSwitch to dstSwitch avoiding swToAvoid;
        ECMPShortestPathGraph graph = new ECMPShortestPathGraph(srcSwitch, switchToAvoid);
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
            String nodeToAvoid = srManager.getMplsLabel(switchToAvoid.getDpid().toString());
            OptimizeLabelStack(labelStack, switchToAvoid);
            SegmentRoutingTunnel avoidTunnel = new SegmentRoutingTunnel(
                    srManager, "avoid-0", labelStack);
            if (avoidTunnel.createTunnel()) {
                //tunnelTable.put(tunnelId, srTunnel);
                //TunnelNotification tunnelNotification =
                //        new TunnelNotification(srTunnel);
                //tunnelEventChannel.addEntry(tunnelId, tunnelNotification);
                populateAclRule(avoidTunnel.getRoutes());
            }
            else {
                log.warn("Failed to create a tunnel for Policy Avoid");
                return false;
            }
        }

        return true;
    }

    /**
     * Optimize the label stack removing unnecessary label IDs, resulting the
     * same path. It modifies the list given directly.
     *
     * @param labelStack List of label IDs
     */
    private void OptimizeLabelStack(List<Integer> labelStack, Switch nodeToAvoid) {

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
        Switch dstNode = srManager.getSwitchFromNodeId(
                labelStack.get(labelStack.size()-1).toString());
        Switch nodeToCheck = srManager.getSwitchFromNodeId(
                labelStack.get(labelStack.size()-2).toString());

        while (!nodeToCheck.getDpid().equals(srcNode)) {


        }




    }

}
