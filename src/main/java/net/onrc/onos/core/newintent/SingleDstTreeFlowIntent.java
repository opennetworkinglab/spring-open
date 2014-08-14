package net.onrc.onos.core.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.IntentId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intent containing {@link SingleDstTreeFlow} object, which defines an explicit tree.
 *
 * It is intended to establish a path by using Flow Manager's API.
 */
public class SingleDstTreeFlowIntent extends AbstractIntent implements InstallableIntent {

    private final SingleDstTreeFlow tree;

    /**
     * Constructs an intent containing the specified {@link SingleDstTreeFlow tree}.
     *
     * @param id intent identifier
     * @param tree tree
     */
    public SingleDstTreeFlowIntent(IntentId id, SingleDstTreeFlow tree) {
        super(id);
        this.tree = checkNotNull(tree);
    }

    /**
     * Returns the tree.
     *
     * @return tree
     */
    public SingleDstTreeFlow getTree() {
        return tree;
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

        SingleDstTreeFlowIntent that = (SingleDstTreeFlowIntent) o;
        return Objects.equal(this.tree, that.tree);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), tree);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", getId())
                .add("tree", tree)
                .toString();
    }
}
