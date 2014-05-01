package net.onrc.onos.core.topology;

import net.onrc.onos.core.util.SwitchPort;

/**
 * Link Object stored in In-memory Topology.
 * <p/>
 * TODO REMOVE following design memo: This object itself may hold the DBObject,
 * but this Object itself will not issue any read/write to the DataStore.
 */
public class LinkImpl extends NetworkGraphObject implements Link {
    private SwitchPort srcPort;
    private SwitchPort dstPort;

    protected static final Double DEFAULT_CAPACITY = Double.POSITIVE_INFINITY;
    protected Double capacity = DEFAULT_CAPACITY;

    protected static final int DEFAULT_COST = 1;
    protected int cost = DEFAULT_COST;

    /**
     * Constructor for when a link is read from the database and the Ports
     * already exist in the in-memory network graph.
     *
     * @param graph
     * @param srcPort
     * @param dstPort
     */
    public LinkImpl(NetworkGraph graph, Port srcPort, Port dstPort) {
        super(graph);
        this.srcPort = srcPort.asSwitchPort();
        this.dstPort = dstPort.asSwitchPort();
    }

    @Override
    public Switch getSrcSwitch() {
        return graph.getSwitch(srcPort.dpid().value());
    }

    @Override
    public Port getSrcPort() {
        return graph.getPort(srcPort.dpid().value(), (long) srcPort.port().value());
    }

    @Override
    public Switch getDstSwitch() {
        return graph.getSwitch(dstPort.dpid().value());
    }

    @Override
    public Port getDstPort() {
        return graph.getPort(dstPort.dpid().value(), (long) dstPort.port().value());
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
    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return String.format("%s --(cap:%f Mbps)--> %s",
                getSrcPort().toString(),
                getCapacity(),
                getDstPort().toString());
    }
}
