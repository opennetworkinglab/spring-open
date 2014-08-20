package net.onrc.onos.core.newintent;

import com.google.common.base.Objects;
import net.onrc.onos.api.flowmanager.SingleSrcTreeFlow;
import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.IntentId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intent containing {@link  SingleSrcTreeFlow} object, which defines an explicit tree.
 *
 * It is intended to establish a path by using Flow Manager's API.
 */
public class SingleSrcTreeFlowIntent extends AbstractIntent implements InstallableIntent {

    private final SingleSrcTreeFlow tree;

    /**
     * Constructs an intent containing the specified {@link SingleSrcTreeFlow tree}.
     *
     * @param id intent identifier
     * @param tree tree
     */
    public SingleSrcTreeFlowIntent(IntentId id, SingleSrcTreeFlow tree) {
        super(id);
        this.tree = checkNotNull(tree);
    }

    /**
     * Constructor for serializer.
     */
    protected SingleSrcTreeFlowIntent() {
        super();
        this.tree = null;
    }

    /**
     * Returns the tree.
     *
     * @return tree
     */
    public SingleSrcTreeFlow getTree() {
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

        SingleSrcTreeFlowIntent that = (SingleSrcTreeFlowIntent) o;
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
