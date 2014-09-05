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
        String version = (String) getRequestAttributes().get("version");
        if (version.equals("new")) {
            sdnIp.beginRoutingWithNewIntent();
            return "SdnIp SetupBgpPaths Succeeded with New intent";
        } else if (version.equals("old")) {

            sdnIp.beginRouting();
            return "SdnIp SetupBgpPaths Succeeded";
        }

        return "URL is wrong!";
    }

}
