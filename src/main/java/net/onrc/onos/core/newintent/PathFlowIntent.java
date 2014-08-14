package net.onrc.onos.core.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.api.flowmanager.PathFlow;
import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.IntentId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intent containing {@link PathFlow} object, which defines an explicit path.
 *
 * It is intended to establish a path by using Flow Manager's API.
 */
public class PathFlowIntent extends AbstractIntent implements InstallableIntent {

    private final PathFlow flow;

    public PathFlowIntent(IntentId id, PathFlow flow) {
        super(id);
        this.flow = checkNotNull(flow);
    }

    /**
     * Returns {@link PathFlow} object, which defines an explicit path.
     *
     * @return {@link PathFlow path}
     */
    public PathFlow getFlow() {
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
