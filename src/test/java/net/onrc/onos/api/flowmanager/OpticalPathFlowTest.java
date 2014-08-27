package net.onrc.onos.api.flowmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;
import net.onrc.onos.core.util.serializers.KryoFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpticalPathFlowTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests if the object can be serialized and deserialized properly with
     * Kryo.
     */
    @Test
    public void testKryo() {
        final FlowId id = new FlowId(1);
        final PortNumber ingressPort = PortNumber.uint32(12345);
        final Path path = new Path(Arrays.asList(
                new FlowLink(new SwitchPort(1, (short) 1), new SwitchPort(2, (short) 2)),
                new FlowLink(new SwitchPort(2, (short) 1), new SwitchPort(3, (short) 2))
                ));
        final List<Action> egressActions =
                Arrays.asList((Action) new OutputAction(PortNumber.uint32(54321)));
        final int lambda = 100;

        final OpticalPathFlow originalFlow =
                new OpticalPathFlow(id, ingressPort, path, egressActions, lambda);

        assertNotNull(originalFlow);
        byte[] buf = KryoFactory.serialize(originalFlow);

        final OpticalPathFlow obtainedFlow = KryoFactory.deserialize(buf);

        assertEquals(id, obtainedFlow.getId());
        assertEquals(ingressPort, obtainedFlow.getIngressPortNumber());
        assertEquals(path, obtainedFlow.getPath());
        assertEquals(egressActions, obtainedFlow.getEgressActions());
        assertEquals(lambda, obtainedFlow.getLambda());
    }
}
