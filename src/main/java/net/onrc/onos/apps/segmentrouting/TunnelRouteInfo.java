package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.util.Dpid;

public class TunnelRouteInfo {

    public String srcSwDpid;
    public List<Dpid> fwdSwDpids;
    public List<String> route;
    public List<Integer> groupIdList;

    /**
     * Constructor
     */
    public TunnelRouteInfo() {
        fwdSwDpids = new ArrayList<Dpid>();
        route = new ArrayList<String>();
    }

    /**
     * Set the source switch dpid for the sub tunnel
     *
     * @param dpid Source router DPID
     */
    public void setSrcDpid(String dpid) {
        this.srcSwDpid = dpid;
    }

    /**
     * Set the next hop router DPIDs
     *
     * @param dpid List of DPIDs
     */
    public void setFwdSwDpid(List<Dpid> dpid) {
        this.fwdSwDpids = dpid;
    }

    /**
     * Add the Label ID for the sub tunnel
     *
     * @param id router ID
     */
    public void addRoute(String id) {
        route.add(id);
    }

    /**
     * Set the group ID list for the tunnel
     *
     * @param gidLIst List of group ID
     */
    public void setGroupIdList(List<Integer> gidLIst) {
        this.groupIdList = gidLIst;
    }

    /**
     * Get the source router DPID
     *
     * @return source router DPID
     */
    public String getSrcSwDpid() {
        return this.srcSwDpid;
    }

    /**
     * Get the next hop router DPIDs
     *
     * @return List of DPIDs
     */
    public List<Dpid> getFwdSwDpid() {
        return this.fwdSwDpids;
    }

    /**
     * Get the label stack of the sub tunnel
     *
     * @return List of router IDs
     */
    public List<String> getRoute() {
        return this.route;
    }

    /**
     * Get the group ID for the sub tunnel
     *
     * @return Group ID
     */
    public int getGroupId() {
        return groupIdList.get(groupIdList.size()-1);
    }

    /**
     * Get the group ID list pushing IDs in the label stack for the tunnel
     *
     * @return List of the group ID
     */
    public List<Integer> getGroupIdList() {
        return this.groupIdList;
    }

}
