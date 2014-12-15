package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.floodlightcontroller.core.IOF13Switch;
import net.floodlightcontroller.core.IOF13Switch.GroupChain;
import net.floodlightcontroller.core.IOF13Switch.GroupChainParams;
import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.onrc.onos.core.drivermanager.OFSwitchImplDellOSR;
import net.onrc.onos.core.topology.Link;
import net.onrc.onos.core.topology.Switch;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.apps.segmentrouting.web.SegmentRouterTunnelRESTParams;
import net.onrc.onos.apps.segmentrouting.web.SegmentRouterTunnelsetRESTParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRoutingTunnelset {

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingTunnel.class);
	private SegmentRoutingManager srManager;
    private String tunnelsetId;
    private HashMap<String, SegmentRoutingTunnel> tunnelMap;
    private HashMap<String, HashMap<String,GroupChain>> tunnelIdGroupChainMap;
    private HashMap<String, List<GroupChain>> tunnelsetGroupChain;
    
	public SegmentRoutingTunnelset(SegmentRoutingManager srm,
    		SegmentRouterTunnelsetRESTParams tunnelsetParams) {
    	this.srManager = srm;
    	this.tunnelsetId = tunnelsetParams.getTunnelset_id();
    	tunnelsetGroupChain = new HashMap<String, List<GroupChain>>();
    	tunnelMap = new HashMap<String,SegmentRoutingTunnel>();
    	tunnelIdGroupChainMap = new HashMap<String, HashMap<String,GroupChain>>();
    	for (SegmentRouterTunnelRESTParams tunnelParams:tunnelsetParams.getTunnelParams()) {
    		SegmentRoutingTunnel tunnel = new 
    				SegmentRoutingTunnel(srm, tunnelParams.getTunnel_id(),
    						tunnelParams.getLabel_path(), tunnelsetId);
    		tunnel.computeTunnelLabelStack();
    		tunnelMap.put(tunnelParams.getTunnel_id(), tunnel);
    	}
    }
    
    public boolean createTunnelSet() {
    	HashMap<String, List<GroupChainParams>> tunnelsetGroupChainParamsMap = 
    			new HashMap<String, List<GroupChainParams>>();
    	for (SegmentRoutingTunnel tunnel:tunnelMap.values()) {
    		HashMap<String, GroupChainParams> tunnelGroupChainParams = 
    				tunnel.getGroupChainParams();
    		for (String targetSwDpid: tunnelGroupChainParams.keySet()) {
    			List<GroupChainParams> groupChainParams = 
    					tunnelsetGroupChainParamsMap.get(targetSwDpid);
    			if (groupChainParams == null) {
    				groupChainParams = new ArrayList<GroupChainParams>();
    				tunnelsetGroupChainParamsMap.put(targetSwDpid, groupChainParams);
    			}
    			groupChainParams.add(tunnelGroupChainParams.get(targetSwDpid));
    		}
    	}

		for (String targetSwDpid: tunnelsetGroupChainParamsMap.keySet()) {
	        IOF13Switch targetSw = srManager.getIOF13Switch(targetSwDpid);

	        if (targetSw == null) {
	            log.debug("Switch {} is gone.", targetSwDpid);
	            continue;
	        }

	        List<GroupChain> groupChainList = targetSw.createGroupChain(
	        		tunnelsetGroupChainParamsMap.get(targetSwDpid));
	        for (GroupChain groupChain:groupChainList) {
	        	HashMap<String,GroupChain> dpidGroupMap = 
	        			tunnelIdGroupChainMap.get(groupChain.getId());
	        	if (dpidGroupMap == null)
	        	{
	        		dpidGroupMap = new HashMap<String, IOF13Switch.GroupChain>();
	        		tunnelIdGroupChainMap.put(groupChain.getId(), dpidGroupMap);
	        	}
	        			
	        	dpidGroupMap.put(targetSwDpid, groupChain);
	        }
	        tunnelsetGroupChain.put(targetSwDpid, groupChainList);
		}
		
		return true;
    }

    public String getTunnelsetId() {
    	return this.tunnelsetId;
    }
    
    public HashMap<String,SegmentRoutingTunnel> getTunnels() {
    	return this.tunnelMap;
    }

    public HashMap<String, GroupChain> getTunnelGroupChain(String tunnelId) {
		return tunnelIdGroupChainMap.get(tunnelId);
	}

    public HashMap<String, List<GroupChain>> getTunnelsetGroupChain() {
		return tunnelsetGroupChain;
	}
}