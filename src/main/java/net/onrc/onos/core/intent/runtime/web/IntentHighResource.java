package net.onrc.onos.core.intent.runtime.web;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.intent.ApplicationIntent;
import net.onrc.onos.api.rest.RestError;
import net.onrc.onos.api.rest.RestErrorCodes;
import net.onrc.onos.core.intent.ConstrainedShortestPathIntent;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentMap;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.IPathCalcRuntimeService;
import net.onrc.onos.core.util.Dpid;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * A class to access the high-level intents.
 */
public class IntentHighResource extends ServerResource {
    private static final Logger log = LoggerFactory.getLogger(IntentHighResource.class);
    // TODO need to assign proper application id.
    private static final String APPLN_ID = "1";

    /**
     * Gets all high-level intents.
     *
     * @return a Representation for a collection with all of the high-level intents.
     */
    @Get("json")
    public Representation retrieve() throws IOException {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        Collection<Intent> intents = intentMap.getAllIntents();

        return toRepresentation(intents, null);
    }

    /**
     * Adds a collection of high-level intents.
     *
     * @param jsonIntent JSON representation of the intents to add.
     * @return a Representation of a collection containing the intents that were
     *         created.
     */
    @Post("json")
    public Representation store(String jsonIntent) {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext()
                .getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());
        if (pathRuntime == null) {
            log.warn("Failed to get path calc runtime");
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ApplicationIntent[] addOperations = null;
        try {
            if (jsonIntent != null) {
                addOperations = mapper.readValue(jsonIntent, ApplicationIntent[].class);
            }
        } catch (IOException ex) {
            log.error("Exception occurred parsing inbound JSON", ex);
        }

        if (addOperations == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            final RestError error =
                    RestError.createRestError(RestErrorCodes.RestErrorCode.INTENT_INVALID);
            return toRepresentation(error, null);
        }

        //
        // Process all intents one-by-one
        //
        // TODO: The Intent Type should be enum instead of a string,
        // and we should use a switch statement below to process the
        // different type of intents.
        //
        IntentOperationList intentOperations = new IntentOperationList();
        for (ApplicationIntent oper : addOperations) {
            String applnIntentId = APPLN_ID + ":" + oper.intentId();

            IntentOperation.Operator operator = IntentOperation.Operator.ADD;
            Dpid srcSwitchDpid = new Dpid(oper.srcSwitchDpid());
            Dpid dstSwitchDpid = new Dpid(oper.dstSwitchDpid());

            if (oper.intentType().equals("SHORTEST_PATH")) {
                //
                // Process Shortest-Path Intent
                //
                ShortestPathIntent spi =
                    new ShortestPathIntent(applnIntentId,
                                           srcSwitchDpid.value(),
                                           oper.srcSwitchPort(),
                                           MACAddress.valueOf(oper.matchSrcMac()).toLong(),
                                           dstSwitchDpid.value(),
                                           oper.dstSwitchPort(),
                                           MACAddress.valueOf(oper.matchDstMac()).toLong());
                spi.setPathFrozen(oper.isStaticPath());
                intentOperations.add(operator, spi);
            } else {
                //
                // Process Constrained Shortest-Path Intent
                //
                ConstrainedShortestPathIntent cspi =
                    new ConstrainedShortestPathIntent(applnIntentId,
                                                      srcSwitchDpid.value(),
                                                      oper.srcSwitchPort(),
                                                      MACAddress.valueOf(oper.matchSrcMac()).toLong(),
                                                      dstSwitchDpid.value(),
                                                      oper.dstSwitchPort(),
                                                      MACAddress.valueOf(oper.matchDstMac()).toLong(),
                                                      oper.bandwidth());
                cspi.setPathFrozen(oper.isStaticPath());
                intentOperations.add(operator, cspi);
            }
        }
        // Apply the Intent Operations
        pathRuntime.executeIntentOperations(intentOperations);

        setStatus(Status.SUCCESS_CREATED);

        return toRepresentation(intentOperations, null);
    }

    /**
     * Deletes all high-level intents.
     *
     * @return a null Representation.
     */
    @Delete("json")
    public Representation remove() {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        // Delete all intents
        // TODO: The implementation below is broken - waiting for the Java API
        // TODO: The deletion should use synchronous Java API?
        pathRuntime.purgeIntents();
        setStatus(Status.SUCCESS_NO_CONTENT);
        return null;      // TODO no reply yet from the purge intents call
    }
}
