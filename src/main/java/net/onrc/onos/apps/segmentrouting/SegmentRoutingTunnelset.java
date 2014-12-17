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
    
    public HashMap<String,SegmentRoutingTunnel> addNewTunnelsToTunnelset(
    		SegmentRouterTunnelsetRESTParams tunnelsetParams) {
    	
    	HashMap<String,SegmentRoutingTunnel> newTunnelMap = null;
    	
    	for (SegmentRouterTunnelRESTParams tunnelParams:tunnelsetParams.getTunnelParams()) {
    		SegmentRoutingTunnel tunnel = new 
    				SegmentRoutingTunnel(srManager, tunnelParams.getTunnel_id(),
    						tunnelParams.getLabel_path(), tunnelsetId);
    		tunnel.computeTunnelLabelStack();

    	
    		HashMap<String, GroupChainParams> tunnelGroupChainParams = 
    				tunnel.getGroupChainParams();
    		for (String targetSwDpid: tunnelGroupChainParams.keySet()) {
    	        IOF13Switch targetSw = srManager.getIOF13Switch(targetSwDpid);

    	        if (targetSw == null) {
    	            log.debug("Switch {} is gone.", targetSwDpid);
    	            continue;
    	        }
    	        
    	        List<GroupChainParams> groupChainParamsList = new 
    	        		ArrayList<IOF13Switch.GroupChainParams>();
    	        groupChainParamsList.add(tunnelGroupChainParams.get(
						targetSwDpid));
    	        List<GroupChain> currentGroupChainList = tunnelsetGroupChain.get(targetSwDpid);
    	        List<GroupChain> newGroupChainList = null;
    	        
    	        if (currentGroupChainList == null) {
    	        	newGroupChainList = targetSw.
    	        			createGroupChain(groupChainParamsList);
    	        }
    	        else {
	    	        newGroupChainList = targetSw.addNewEntryToGroupChain(
	    	        				currentGroupChainList, 
	    	        				groupChainParamsList);
    	        }
    	        
    	        if (newGroupChainList == null) {
    	        	log.warn("CreateGroupChain for Switch {} failed at driver", targetSwDpid);
    	            continue;
    	        }

    	        for (GroupChain groupChain:newGroupChainList) {
    	        	HashMap<String,GroupChain> dpidGroupMap = 
    	        			tunnelIdGroupChainMap.get(groupChain.getId());
    	        	if (dpidGroupMap == null)
    	        	{
    	        		dpidGroupMap = new HashMap<String, IOF13Switch.GroupChain>();
    	        		tunnelIdGroupChainMap.put(groupChain.getId(), dpidGroupMap);
    	        	}
    	        			
    	        	dpidGroupMap.put(targetSwDpid, groupChain);
    	        }
    	        tunnelsetGroupChain.put(targetSwDpid, newGroupChainList);
    		}    	
    		tunnelMap.put(tunnelParams.getTunnel_id(), tunnel);
    		if (newTunnelMap == null)
    			newTunnelMap = new HashMap<String, SegmentRoutingTunnel>();
    		newTunnelMap.put(tunnelParams.getTunnel_id(), tunnel);
    	}
    	return newTunnelMap;
    }

    public boolean removeConstituentTunnelFromTunnelset(
    		SegmentRouterTunnelsetRESTParams tunnelsetParams) {
    	for (String tunnelId:tunnelsetParams.getRemove_tunnel_params()) {
    		HashMap<String, GroupChain> tunnelIdGroupChain = 
    				tunnelIdGroupChainMap.get(tunnelId);
    		for (String targetSwDpid:tunnelIdGroupChain.keySet()) {
                IOF13Switch sw13 = srManager.getIOF13Switch(targetSwDpid);
                if (sw13 != null) {
                	GroupChain tunnelIdSwGroupChain = tunnelIdGroupChain.get(targetSwDpid);
                	List<Integer> groupsPointedByInnermostGroup = new ArrayList<Integer>();
                	for (PortNumber sp:tunnelIdSwGroupChain.getGroupChain().keySet()) {
                		List<Integer> portGroupList = 
                				tunnelIdSwGroupChain.getGroupChain().get(sp);
                		/* Get the last group in the list. 
                		 * This will be pointed by innermost group 
                		 */
                		int groupId = portGroupList.get(portGroupList.size()-1);
                		groupsPointedByInnermostGroup.add(groupId);
                	}
                	
                	if (groupsPointedByInnermostGroup.size() >0) {
	                	if (!sw13.removeOutGroupBucketsFromGroup(
	                			tunnelIdSwGroupChain.getInnermostGroupId(), 
	                			groupsPointedByInnermostGroup)) {
	                		log.warn("Faied to remove outgroup buckets "
	                				+ "from group {} tunnelset {} at driver",
	                				tunnelIdSwGroupChain.getInnermostGroupId(), 
	                				tunnelsetId);
	                        return false;
	                	}
	                	for (List<Integer> groupList: tunnelIdSwGroupChain.getGroupChain().values()) {
	                        for (int i = groupList.size()-1; i >= 0; i--) {
	                            int groupId = groupList.get(i);
	                            if (!sw13.removeGroup(groupId)) {
	                                log.warn("Faied to remove the tunnelset {} at driver",
	                                        tunnelsetId);
	                                return false;
	                            }
	                        }
	                	}
                	}
                	else
                	{
                		/* No Group chain created for this tunnel in this switch
                		 * Just remove the innermost group
                		 */
	                	if (!sw13.removeGroup(
	                			tunnelIdSwGroupChain.getInnermostGroupId())) {
	                		log.warn("Faied to remove innermost group "
	                				+ "{} of tunnelset {} at driver",
	                				tunnelIdSwGroupChain.getInnermostGroupId(), 
	                				tunnelsetId);
	                        return false;
	                	}
                	}
                	List<GroupChain> swGroupChainList = tunnelsetGroupChain.get(targetSwDpid);
                	for (int idx=0;idx<swGroupChainList.size();idx++) {
                		if (swGroupChainList.get(idx).getId().equals(tunnelId)) {
                			swGroupChainList.remove(idx);
                		}
                	}
                	if (swGroupChainList.size()==0)
                		tunnelsetGroupChain.remove(targetSwDpid);
                }
    		}
    		tunnelIdGroupChainMap.remove(tunnelId);
    		tunnelMap.remove(tunnelId);
    	}
    	return true;
    }
    /**
     * Remove the tunnelset.
     * It requests driver to remove all groups for the tunnelset
     *
     * @return true if succeeds, false otherwise.
     */
    public boolean removeTunnelset() {

    	for (String targetSwDpid:tunnelsetGroupChain.keySet()) {
            IOF13Switch sw13 = srManager.getIOF13Switch(targetSwDpid);
            if (sw13 != null) {
            	List<GroupChain> groupChainList = tunnelsetGroupChain.get(targetSwDpid);
            	
            	if (groupChainList != null) {
                    // Innermost Group needs to be removed first because
                    // the group being pointed by any other group cannot be removed
            		int innermostGroupId = groupChainList.
            				get(0).getInnermostGroupId();
                    if (!sw13.removeGroup(innermostGroupId)) {
                        log.warn("Faied to remove the tunnelset {} at driver",
                                tunnelsetId);
                        return false;
                    }
                    
                    for (GroupChain groupChain: groupChainList) {
                    	HashMap<PortNumber,List<Integer>> portGroupChain = 
                    								groupChain.getGroupChain();
                    	for (List<Integer> groupList: portGroupChain.values()) {
	                        for (int i = groupList.size()-1; i >= 0; i--) {
	                            int groupId = groupList.get(i);
	                            if (!sw13.removeGroup(groupId)) {
	                                log.warn("Faied to remove the tunnelset {} at driver",
	                                        tunnelsetId);
	                                return false;
	                            }
	                        }
                    	}
                    }
            	}
            }
    	}
    	
    	tunnelsetGroupChain.clear();
    	tunnelIdGroupChainMap.clear();

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