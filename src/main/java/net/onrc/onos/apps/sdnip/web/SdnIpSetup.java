package net.onrc.onos.apps.sdnip.web;

import net.onrc.onos.apps.sdnip.ISdnIpService;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * REST call to start SDN-IP routing.
 */
public class SdnIpSetup extends ServerResource {
    @Get("json")
    public String sdnipSetupMethod() {
        ISdnIpService sdnIp = (ISdnIpService) getContext()
                              .getAttributes().get(ISdnIpService.class.getCanonicalName());
        sdnIp.beginRoutingNew();
        return "SdnIp SetupBgpPaths Succeeded";
    }

}
