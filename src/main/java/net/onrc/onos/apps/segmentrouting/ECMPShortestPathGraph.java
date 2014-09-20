package net.onrc.onos.apps.segmentrouting;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;

import org.projectfloodlight.openflow.util.HexString;

import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.intent.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates bandwidth constrained breadth first tree and returns paths
 * from root switch to leaf switches which satisfies the bandwidth condition. If
 * bandwidth parameter is not specified, the normal breadth first tree will be
 * calculated. The paths are snapshot paths at the point of the class
 * instantiation.
 */
public class ECMPShortestPathGraph {
    LinkedList<Switch> switchQueue = new LinkedList<>();
    LinkedList<Integer> distanceQueue = new LinkedList<>();
    HashMap<Dpid, Integer> switchSearched = new HashMap<>();
    HashMap<Dpid, ArrayList<LinkData>> upstreamLinks = new HashMap<>();
    HashMap<Dpid, ArrayList<Path>> paths = new HashMap<>();
    HashMap<Integer, ArrayList<Switch>> distanceSwitchMap = new HashMap<>();
    Switch rootSwitch;
    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);
    
    /**
     * Constructor.
     *
     * @param rootSwitch root of the BFS tree
     */
    public ECMPShortestPathGraph(Switch rootSwitch) {
        this.rootSwitch = rootSwitch;
        calcECMPShortestPathGraph();
    }

    /**
     * Calculates the BFS tree using any provided constraints and Intents.
     */
    private void calcECMPShortestPathGraph() {
        switchQueue.add(rootSwitch);
        int currDistance = 0;
        distanceQueue.add(currDistance);
        switchSearched.put(rootSwitch.getDpid(),currDistance);
        while (!switchQueue.isEmpty()) {
            Switch sw = switchQueue.poll();
            Switch prevSw = null;
            currDistance = distanceQueue.poll();
            for (Link link : sw.getOutgoingLinks()) {
                Switch reachedSwitch = link.getDstPort().getSwitch();
                if ((prevSw != null) && (prevSw.getDpid().equals(reachedSwitch.getDpid())))
                {
                    /* Ignore LAG links between the same set of switches */
                	continue;
                }
                else
                {
                    prevSw = reachedSwitch;
                }
                	
                Integer distance = switchSearched.get(reachedSwitch.getDpid());
                if ((distance != null) && (distance.intValue() < (currDistance+1))) {
                    continue;
                }
                if (distance == null){
                	/* First time visiting this switch node */
                    switchQueue.add(reachedSwitch);
                    distanceQueue.add(currDistance+1);
                    switchSearched.put(reachedSwitch.getDpid(),currDistance+1);
                    
                    ArrayList<Switch> distanceSwArray = distanceSwitchMap.get(currDistance+1);
                    if (distanceSwArray == null)
                    {
                            distanceSwArray = new ArrayList<Switch>();
                            distanceSwArray.add(reachedSwitch);
                            distanceSwitchMap.put(currDistance+1, distanceSwArray);
                    }
                    else
                            distanceSwArray.add(reachedSwitch);
                }

                ArrayList<LinkData> upstreamLinkArray = 
                		upstreamLinks.get(reachedSwitch.getDpid());
                if (upstreamLinkArray == null)
                {
                	upstreamLinkArray = new ArrayList<LinkData>();
                	upstreamLinkArray.add(new LinkData(link));
                	upstreamLinks.put(reachedSwitch.getDpid(), upstreamLinkArray);
                }
                else
                	/* ECMP links */
                	upstreamLinkArray.add(new LinkData(link));
            }
        }

        log.debug("ECMPShortestPathGraph:switchSearched for switch {} is {}",
                        HexString.toHexString(rootSwitch.getDpid().value()), switchSearched);
        log.debug("ECMPShortestPathGraph:upstreamLinks for switch {} is {}",
                        HexString.toHexString(rootSwitch.getDpid().value()), upstreamLinks);
        log.debug("ECMPShortestPathGraph:distanceSwitchMap for switch {} is {}",
                        HexString.toHexString(rootSwitch.getDpid().value()), distanceSwitchMap);
        /*
        for (Integer distance: distanceSwitchMap.keySet()){
                for (Switch sw: distanceSwitchMap.get(distance)){
                        ArrayList<Path> path = getPath(sw);
                        log.debug("ECMPShortestPathGraph:Paths in Pass{} from switch {} to switch {} is {}",
                                        distance,
                                        HexString.toHexString(rootSwitch.getDpid().value()),
                                        HexString.toHexString(sw.getDpid().value()), path);
                }
        }*/
    }

    private void getDFSPaths(Dpid dstSwitchDpid, Path path, ArrayList<Path> paths) {
        Dpid rootSwitchDpid = rootSwitch.getDpid();
        for (LinkData upstreamLink : upstreamLinks.get(dstSwitchDpid)) {
        	/* Deep clone the path object */
            Path sofarPath = new Path();
            if (!path.isEmpty())
            	sofarPath.addAll(path.subList(0, path.size()));
            sofarPath.add(upstreamLink);
            if (upstreamLink.getSrc().getDpid().equals(rootSwitchDpid))
            {
                    paths.add(sofarPath);
                    return;
            }
            else
                    getDFSPaths(upstreamLink.getSrc().getDpid(),sofarPath, paths);
        }
    }

    /**
     * Return the computed path from the root switch to the leaf switch.
     *
     * @param leafSwitch the leaf switch
     * @return the Path from the root switch to the leaf switch
     */
    public ArrayList<Path> getPath(Switch leafSwitch) {
        ArrayList<Path> pathArray = paths.get(leafSwitch.getDpid());
        if (pathArray == null && switchSearched.containsKey(leafSwitch.getDpid())) {
            pathArray = new ArrayList<>();
            Path path = new Path();
            Dpid sw = leafSwitch.getDpid();
            getDFSPaths(sw, path, pathArray);
            paths.put(leafSwitch.getDpid(), pathArray);
        }
        return pathArray;
    }
}
