package net.onrc.onos.ofcontroller.networkgraph;

/**
 * Link Object stored in In-memory Topology.
 *
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class LinkImpl extends NetworkGraphObject implements Link {
	protected Port srcPort;
	protected Port dstPort;

	protected static final Double DEFAULT_CAPACITY = Double.POSITIVE_INFINITY;
	protected Double capacity = DEFAULT_CAPACITY;

	protected static final int DEFAULT_COST = 1;
	protected int cost = DEFAULT_COST;
	
	private long srcSwitchDpid;
	private long srcPortNumber;
	private long dstSwitchDpid;
	private long dstPortNumber;
	
	/**
	 * Constructor for when a new link is being created because of a link
	 * discovery event.
	 * @param graph
	 * @param srcSwitchDpid
	 * @param srcPortNumber
	 * @param dstSwitchDpid
	 * @param dstPortNumber
	 */
	public LinkImpl(NetworkGraph graph, long srcSwitchDpid, long srcPortNumber,
			long dstSwitchDpid, long dstPortNumber) {
		super(graph);
		
		this.srcSwitchDpid = srcSwitchDpid;
		this.srcPortNumber = srcPortNumber;
		this.dstSwitchDpid = dstSwitchDpid;
		this.dstPortNumber = dstPortNumber;
	}

	/**
	 * Constructor for when a link is read from the database and the Ports
	 * already exist in the in-memory network graph.
	 * @param graph
	 * @param srcPort
	 * @param dstPort
	 */
	public LinkImpl(NetworkGraph graph, Port srcPort, Port dstPort) {
		super(graph);
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		setToPorts();
	}

	protected void setToPorts() {
		((PortImpl)srcPort).setOutgoingLink(this);
		((PortImpl)srcPort).setIncomingLink(this);		
	}
	
	protected void unsetFromPorts() {
		((PortImpl)srcPort).setOutgoingLink(null);
		((PortImpl)srcPort).setIncomingLink(null);
	}

	@Override
	public Port getSourcePort() {
		return srcPort;
	}

	@Override
	public Port getDestinationPort() {
		return dstPort;
	}

	@Override
	public Switch getSourceSwitch() {
		return srcPort.getSwitch();
	}

	@Override
	public Switch getDestinationSwitch() {
		return dstPort.getSwitch();
	}

	@Override
	public long getLastSeenTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	@Override
	public Long getSourceSwitchDpid() {
		return srcSwitchDpid;
	}

	@Override
	public Long getSourcePortNumber() {
		return srcPortNumber;
	}

	@Override
	public Long getDestinationSwitchDpid() {
		return dstSwitchDpid;
	}

	@Override
	public Long getDestinationPortNumber() {
		return dstPortNumber;
	}

	@Override
	public Double getCapacity() {
		return capacity;
	}
	
	@Override
	public String toString() {
		return String.format("%s --(cap:%f Mbps)--> %s",
				getSourcePort().toString(),
				getCapacity(),
				getDestinationPort().toString());
	}

	public void setCapacity(Double capacity) {
		this.capacity = capacity;
	}
}