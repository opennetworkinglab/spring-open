package net.onrc.onos.api.flowmanager;

import net.onrc.onos.core.util.IdGenerator;

/**
 * An generator of {@link FlowId}. It is defined only for
 * testing purpose to keep type safety on mock creation.
 */
public interface FlowIdGenerator extends IdGenerator<FlowId> {
}
