package net.onrc.onos.ofcontroller.networkgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.ramcloud.JRamCloud.ObjectDoesntExistException;
import edu.stanford.ramcloud.JRamCloud.WrongVersionException;
import net.onrc.onos.datastore.topology.RCPort;
import net.onrc.onos.datastore.topology.RCSwitch;
import net.onrc.onos.ofcontroller.util.FlowEntry;

public class SwitchImpl extends NetworkGraphObject implements Switch {
	
	private long dpid;
	private final Map<Short, Port> ports;

	public SwitchImpl(NetworkGraph graph) {
		super(graph);
		
		ports = new HashMap<Short, Port>();
	}

	@Override
	public long getDpid() {
		return dpid;
	}

	@Override
	public Collection<Port> getPorts() {
		return Collections.unmodifiableCollection(ports.values());
	}

	@Override
	public Port getPort(short number) {
		return ports.get(number);
	}

	@Override
	public Collection<FlowEntry> getFlowEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Switch> getNeighbors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Link getLinkToNeighbor(long neighborDpid) {
		for (Link link : graph.getLinksFromSwitch(dpid)) {
			if (link.getDestinationSwitch().getDpid() == neighborDpid) {
				return link;
			}
		}
		return null;
	}

	@Override
	public Collection<Device> getDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}
	
	public void addPort(Port port) {
		this.ports.put(port.getNumber(), port);
	}

	@Override
	public Iterable<Link> getLinks() {
		return graph.getLinksFromSwitch(dpid);
	}
	
	public void store() {
		RCSwitch rcSwitch = new RCSwitch(dpid);
		
		for (Port port : ports.values()) {
			RCPort rcPort = new RCPort(dpid, (long)port.getNumber());
			rcSwitch.addPortId(rcPort.getId());
		}
		
		
		try {
			rcSwitch.update();
			
		} catch (ObjectDoesntExistException | WrongVersionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
