package net.onrc.onos.core.intent.runtime.web;

import java.io.IOException;
import java.util.Collection;

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
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @return a collection with all high-level intents.
     */
    @Get("json")
    public Collection<Intent> retrieve() throws IOException {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        IntentMap intentMap = pathRuntime.getHighLevelIntents();
        Collection<Intent> intents = intentMap.getAllIntents();

        return intents;
    }

    /**
     * Adds a collection of high-level intents.
     *
     * @return the status of the operation (TBD).
     */
    @Post("json")
    public String store(String jsonIntent) throws IOException {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext()
                .getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());
        if (pathRuntime == null) {
            log.warn("Failed to get path calc runtime");
            return "";
        }

        String reply = "";
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
            return mapper.writeValueAsString(error);
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

        return reply;
    }

    /**
     * Deletes all high-level intents.
     *
     * @return the status of the operation (TBD).
     */
    @Delete("json")
    public String store() {
        IPathCalcRuntimeService pathRuntime = (IPathCalcRuntimeService) getContext().
                getAttributes().get(IPathCalcRuntimeService.class.getCanonicalName());

        // Delete all intents
        // TODO: The implementation below is broken - waiting for the Java API
        // TODO: The deletion should use synchronous Java API?
        pathRuntime.purgeIntents();
        setStatus(Status.SUCCESS_NO_CONTENT);
        return "";      // TODO no reply yet from the purge intents call
    }
}
