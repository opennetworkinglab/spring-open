package net.onrc.onos.api.newintent;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Intent service as {@link IFloodlightService}.
 */
public interface IntentFloodlightService
        extends IFloodlightService, IntentService, IntentExtensionService {
}
