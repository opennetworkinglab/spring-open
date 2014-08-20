package net.onrc.onos.core.newintent;

import static com.google.common.base.Preconditions.checkNotNull;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.IntentId;

import com.google.common.base.Objects;

/**
 * Intent containing {@link PacketPathFlow} object, which defines an explicit path.
 *
 * It is intended to establish a path by using Flow Manager's API.
 */
public class PathFlowIntent extends AbstractIntent implements InstallableIntent {

    private final PacketPathFlow flow;

    public PathFlowIntent(IntentId id, PacketPathFlow flow) {
        super(id);
        this.flow = checkNotNull(flow);
    }

    /**
     * Constructor for serializer.
     */
    protected PathFlowIntent() {
        super();
        this.flow = null;
    }

    /**
     * Returns {@link PacketPathFlow} object, which defines an explicit path.
     *
     * @return {@link PacketPathFlow path}
     */
    public PacketPathFlow getFlow() {
        return flow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PathFlowIntent that = (PathFlowIntent) o;
        return Objects.equal(this.flow, that.flow);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), flow);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("flow", flow)
                .toString();
    }
}
