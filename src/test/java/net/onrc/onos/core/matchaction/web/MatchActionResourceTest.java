package net.onrc.onos.core.matchaction.web;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionService;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatchBuilder;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.representation.Representation;

/**
 * Tests for the {@link MatchActionResource} REST handler.
 */
public class MatchActionResourceTest {

    private MatchActionResource matchActionResource;

    /**
     * Set up the MatchActionResource for the test.
     */
    @Before
    public void setUp() {
        // Create some match-action data
        Set<MatchAction> matchActionSet = createMatchActions();

        // Create a mock match-action service that will return the match-actions
        MatchActionService matchActionService = createMock(MatchActionService.class);
        expect(matchActionService.getMatchActions()).andReturn(matchActionSet).anyTimes();
        replay(matchActionService);

        // Inject the match-action service into a Restlet context
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(MatchActionService.class.getCanonicalName(), matchActionService);
        Context context = new Context();
        context.setAttributes(attributes);

        // Create a MatchActionResource and initialize with the context
        matchActionResource = new MatchActionResource();
        matchActionResource.init(context, new Request(), null);
    }

    /**
     * Creates some match-action data that the REST handler can retrieve.
     * The data is arbitrary because it is never verified during the test.
     *
     * @return a set of dummy MatchAction objects for the test
     */
    private Set<MatchAction> createMatchActions() {
        Set<MatchAction> matchActionSet = new HashSet<>();

        Match match = new PacketMatchBuilder().setDstTcpPort((short) 1)
                .setDstIp(new IPv4(5)).build();

        List<Action> actions = new ArrayList<>();
        actions.add(new ModifyDstMacAction(MACAddress.valueOf(10L)));
        actions.add(new OutputAction(PortNumber.uint32(4)));

        MatchAction ma = new MatchAction(new MatchActionId(1L), new SwitchPort(1L, 1L), match,
                actions);

        matchActionSet.add(ma);

        return matchActionSet;
    }

    /**
     * Tests the handler method that retrieves all match-action resources.
     *
     * @throws IOException if there's an error serializing the representation
     */
    @Test
    public void testRetrieve() throws IOException {
        Representation rep = matchActionResource.retrieve();

        StringWriter writer = new StringWriter();

        rep.write(writer);
        String output = writer.toString();

        System.out.println(output);

        assertNotNull(output);
        // Output should be a JSON array of JSON objects
        assertTrue(output.startsWith("[{"));
    }

}
