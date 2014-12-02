package net.onrc.onos.apps.segmentrouting;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.util.Dpid;

public class TunnelRouteInfo {

    public String srcSwDpid;
    public List<Dpid> fwdSwDpids;
    public List<String> route;
    public int gropuId;

    public TunnelRouteInfo() {
        fwdSwDpids = new ArrayList<Dpid>();
        route = new ArrayList<String>();
    }

    public void setSrcDpid(String dpid) {
        this.srcSwDpid = dpid;
    }

    public void setFwdSwDpid(List<Dpid> dpid) {
        this.fwdSwDpids = dpid;
    }

    public void addRoute(String id) {
        route.add(id);
    }

    public void setGroupId(int groupId) {
        this.gropuId = groupId;
    }

    public String getSrcSwDpid() {
        return this.srcSwDpid;
    }

    public List<Dpid> getFwdSwDpid() {
        return this.fwdSwDpids;
    }

    public List<String> getRoute() {
        return this.route;
    }

    public int getGroupId() {
        return this.gropuId;
    }
}
