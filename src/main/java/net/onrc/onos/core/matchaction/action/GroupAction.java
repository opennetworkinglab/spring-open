package net.onrc.onos.core.matchaction.action;

import net.floodlightcontroller.core.IOF13Switch.NeighborSet;
import net.onrc.onos.core.util.Dpid;

public class GroupAction implements Action {
    NeighborSet fwdSws;

    public GroupAction() {
        fwdSws = new NeighborSet();
    }

    public void addSwitch(Dpid d) {
        fwdSws.addDpid(d);
    }

    public NeighborSet getDpids() {
        return fwdSws;
    }
}
