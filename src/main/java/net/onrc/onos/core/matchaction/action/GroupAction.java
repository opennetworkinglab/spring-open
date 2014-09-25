package net.onrc.onos.core.matchaction.action;

import java.util.ArrayList;
import java.util.List;

import net.onrc.onos.core.util.Dpid;

public class GroupAction implements Action {
    List<Dpid> fwdSws;

    public GroupAction() {
        fwdSws = new ArrayList<Dpid>();
    }

    public void addSwitch(Dpid d) {
        fwdSws.add(d);
    }

    public List<Dpid> getDpids() {
        return fwdSws;
    }
}
