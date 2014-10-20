package net.onrc.onos.core.matchaction.action;

import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.onrc.onos.core.util.Dpid;

public class GroupAction implements Action {
    NeighborSet fwdSws;
    int groupId;
    String tunnelId;

    public GroupAction() {
        fwdSws = new NeighborSet();
    }

    public void addSwitch(Dpid d) {
        fwdSws.addDpid(d);
    }

    public void setEdgeLabel(int edgeLabel) {
        fwdSws.setEdgeLabel(edgeLabel);
    }

    public NeighborSet getDpids() {
        return fwdSws;
    }

    public void setGroupId(int id) {
        this.groupId = id;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public void setTunnelId(String tid) {
        this.tunnelId = tid;
    }

    public String getTunnelId() {
        return tunnelId;
    }
}
