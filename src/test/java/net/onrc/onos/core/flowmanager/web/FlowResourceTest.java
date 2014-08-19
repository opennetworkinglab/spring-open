package net.onrc.onos.core.flowmanager.web;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.flowmanager.OpticalPathFlow;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.ModifyLambdaAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.representation.Representation;

/**
 * Tests for the {@link FlowResource} REST handler.
 */
public class FlowResourceTest {
    FlowResource flowResource;

    /**
     * Set up the FlowResource for the test.
     */
    @Before
    public void setUp() {
        // Create some flow data
        Set<Flow> flowSet = createFlows();

        // Create a mock flow manager service that will return the flows
        FlowManagerService flowManager = createMock(FlowManagerService.class);
        expect(flowManager.getFlows()).andReturn(flowSet);
        replay(flowManager);

        // Inject the flow manager service into a Restlet context
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(FlowManagerService.class.getCanonicalName(), flowManager);
        Context context = new Context();
        context.setAttributes(attributes);

        // Create a FlowResource and initialize with the context
        flowResource = new FlowResource();
        flowResource.init(context, new Request(), null);
    }

    /**
     * Creates some flow data that the REST handler can retrieve.
     * The data is arbitrary because it is never verified during the test.
     *
     * @return a set of dummy Flow objects for the test
     */
    private Set<Flow> createFlows() {
        Set<Flow> flowSet = new HashSet<>();

        PacketMatch match = new PacketMatchBuilder().setDstTcpPort((short) 1).build();

        List<FlowLink> links = new ArrayList<>();
        links.add(new FlowLink(new SwitchPort(1L, 2L), new SwitchPort(2L, 1L)));
        links.add(new FlowLink(new SwitchPort(2L, 2L), new SwitchPort(3L, 1L)));

        Path path = new Path(links);

        PacketPathFlow packetFlow = new PacketPathFlow(new FlowId(1L),
                match, PortNumber.uint32(1), path,
                Collections.<Action>singletonList(new ModifyDstMacAction(MACAddress.valueOf(4L))),
                0, 0);

        OpticalPathFlow opticalFlow = new OpticalPathFlow(new FlowId(2L),
                PortNumber.uint32(3), path,
                Collections.<Action>singletonList(new ModifyLambdaAction(2)), 4);

        flowSet.add(packetFlow);
        flowSet.add(opticalFlow);

        return flowSet;
    }

    /**
     * Tests the handler method that retrieves all flow resources.
     *
     * @throws IOException if there's an error serializing the representation
     */
    @Test
    public void testRetrieve() throws IOException {
        Representation rep = flowResource.retrieve();

        StringWriter writer = new StringWriter();

        rep.write(writer);
        String output = writer.toString();

        System.out.println(writer);

        assertNotNull(output);
        // Output should be a JSON array of JSON objects
        assertTrue(output.startsWith("[{"));
    }

}
