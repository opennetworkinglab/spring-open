package net.onrc.onos.core.newintent;

import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowIdGenerator;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentIdGenerator;
import net.onrc.onos.core.topology.BaseTopology;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.MockTopology;
import net.onrc.onos.core.topology.MutableTopology;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.PortNumber;
import org.junit.Before;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * A base test class for intent compiler aggregating
 * common set up logic and variables.
 */
public class IntentCompilerTest {
    protected final IntentId intentId1 = new IntentId(1L);
    protected final IntentId intentId2 = new IntentId(2L);
    protected final FlowId flowId = new FlowId(1L);
    protected final Dpid dpid1 = new Dpid(1L);
    protected final Dpid dpid2 = new Dpid(2L);
    protected final Dpid dpid3 = new Dpid(3L);
    protected final Dpid dpid4 = new Dpid(4L);
    protected final PortNumber port1h = PortNumber.uint32(15);
    protected final PortNumber port12 = PortNumber.uint32(12);
    protected final PortNumber port14 = PortNumber.uint32(14);
    protected final PortNumber port3h = PortNumber.uint32(35);
    protected final PortNumber port32 = PortNumber.uint32(32);
    protected final PortNumber port21 = PortNumber.uint32(21);
    protected final PortNumber port23 = PortNumber.uint32(23);
    protected final PortNumber port41 = PortNumber.uint32(41);
    protected final PortNumber port43 = PortNumber.uint32(43);
    protected IntentIdGenerator intentIdGenerator;
    protected FlowIdGenerator flowIdGenerator;
    protected ITopologyService topologyService;

    @Before
    public void commonSetUp() {
        intentIdGenerator = createMock(IntentIdGenerator.class);
        flowIdGenerator = createMock(FlowIdGenerator.class);
        topologyService = createMock(ITopologyService.class);

        // configure mocks
        expect(intentIdGenerator.getNewId())
                .andReturn(intentId1)
                .andReturn(intentId2);
        expect(flowIdGenerator.getNewId()).andReturn(flowId);
        expect(topologyService.getTopology()).andReturn((MutableTopology) createFakeTopology());
        replay(intentIdGenerator, flowIdGenerator, topologyService);
    }

    protected BaseTopology createFakeTopology() {
        MockTopology mock = new MockTopology();
        mock.createSampleTopology2();

        return mock;
    }
}
