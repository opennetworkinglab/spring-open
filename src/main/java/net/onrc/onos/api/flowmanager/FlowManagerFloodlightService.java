package net.onrc.onos.api.flowmanager;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * A flow manager service as a {@link IFloodlightService} service.
 */
public interface FlowManagerFloodlightService extends
        FlowManagerService,
        IFloodlightService {
}
