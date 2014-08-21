package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.IntentExtensionService;
import net.onrc.onos.api.newintent.IntentService;

/**
 * An interface, which mixes Intent Service and Intent Extension Service.
 */
public interface IntentManager extends IntentService, IntentExtensionService {
}
