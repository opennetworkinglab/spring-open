package net.onrc.onos.core.newintent.web;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentFloodlightService;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.api.newintent.PointToPointIntent;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.representation.Representation;

/**
 * Tests for the {@link IntentResource} REST handler.
 */
public class IntentResourceTest {
    private IntentResource intentResource;

    /**
     * Set up the IntentResource for the test.
     */
    @Before
    public void setUp() {
        // Create some intent data
        Set<Intent> intentSet = createIntents();

        // Create a mock intent service that will return the intents
        IntentFloodlightService intentService =
                createMock(IntentFloodlightService.class);
        expect(intentService.getIntents()).andReturn(intentSet).anyTimes();
        replay(intentService);

        // Inject the intent service into a Restlet context
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(IntentFloodlightService.class.getCanonicalName(), intentService);
        Context context = new Context();
        context.setAttributes(attributes);

        // Create an IntentResource and initialize with the context
        intentResource = new IntentResource();
        intentResource.init(context, new Request(), null);
    }

    /**
     * Creates some intent data that the REST handler can retrieve.
     * The data is arbitrary because it is never verified during the test.
     *
     * @return a set of dummy Intent objects for the test
     */
    private Set<Intent> createIntents() {
        Set<Intent> intentSet = new HashSet<>();

        Match match = new PacketMatchBuilder().setDstTcpPort((short) 1).build();

        Intent pointToPointIntent = new PointToPointIntent(new IntentId(1L), match,
                new ModifyDstMacAction(MACAddress.valueOf(1L)),
                new SwitchPort(1L, 1L),
                new SwitchPort(2L, 2L));

        Set<SwitchPort> inPorts = new HashSet<>();
        inPorts.add(new SwitchPort(3L, 3L));
        inPorts.add(new SwitchPort(4L, 4L));

        Intent multiPointToPointIntent = new MultiPointToSinglePointIntent(
                new IntentId(2L), match,
                new ModifyDstMacAction(MACAddress.valueOf(2L)),
                inPorts, new SwitchPort(5L, 5L));

        intentSet.add(pointToPointIntent);
        intentSet.add(multiPointToPointIntent);

        return intentSet;
    }

    /**
     * Tests the handler method that retrieves all intent resources.
     *
     * @throws IOException if there's an error serializing the representation
     */
    @Test
    public void testGetAllIntents() throws IOException {
        Representation rep = intentResource.retrieve();

        StringWriter writer = new StringWriter();

        rep.write(writer);
        String output = writer.toString();

        System.out.println(output);

        assertNotNull(output);
        // Output should be a JSON array of JSON objects
        assertTrue(output.startsWith("[{"));
    }

}
