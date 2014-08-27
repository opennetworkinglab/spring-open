package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.onrc.onos.api.flowmanager.FlowBatchId;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.util.serializers.KryoFactory;

import com.hazelcast.core.IMap;

/**
 * An implementation of shared distributed map of {@link FlowBatchMap}.
 * <p>
 * This class's implementation is almost the same to {@link SharedFlowMap}. The
 * base class for them should be implemented.
 */
public class SharedFlowBatchMap implements FlowBatchMap {
    private static final String FLOWBATCHMAP_NAME = "flowbatch_map";
    private static final String FLOWBATCHSTATEMAP_NAME = "flowbatchstate_map";

    private final IMap<String, byte[]> flowBatchMap;
    private final IMap<String, byte[]> flowBatchStateMap;
    private final SharedFlowBatchMapEventDispatcher dispatcher;

    /**
     * Creates instance using {@link ISharedCollectionsService} service.
     *
     * @param service the {@link ISharedCollectionsService} service
     */
    SharedFlowBatchMap(ISharedCollectionsService service) {
        this.flowBatchMap = checkNotNull(service.getConcurrentMap(
                FLOWBATCHMAP_NAME, String.class, byte[].class));
        this.flowBatchStateMap = checkNotNull(service.getConcurrentMap(
                FLOWBATCHSTATEMAP_NAME, String.class, byte[].class));
        this.dispatcher = new SharedFlowBatchMapEventDispatcher(
                flowBatchMap, flowBatchStateMap);
    }

    @Override
    public FlowBatchOperation get(FlowBatchId id) {
        byte[] buf = flowBatchMap.get(checkNotNull(id).toString());
        if (buf == null) {
            return null;
        }
        return KryoFactory.deserialize(buf);
    }

    @Override
    public boolean put(FlowBatchId id, FlowBatchOperation flowOp) {
        byte[] buf = KryoFactory.serialize(checkNotNull(flowOp));
        flowBatchMap.set(id.toString(), checkNotNull(buf));
        return true;
    }

    @Override
    public FlowBatchOperation remove(FlowBatchId id) {
        String flowBatchIdStr = checkNotNull(id).toString();
        byte[] buf = flowBatchMap.remove(flowBatchIdStr);
        if (buf == null) {
            return null;
        }
        flowBatchStateMap.remove(flowBatchIdStr);
        return KryoFactory.deserialize(buf);
    }

    @Override
    public Set<FlowBatchOperation> getAll() {
        Set<FlowBatchOperation> flowBatchs = new HashSet<>();
        for (Entry<String, byte[]> entry : flowBatchMap.entrySet()) {
            flowBatchs.add((FlowBatchOperation)
                    KryoFactory.deserialize(entry.getValue()));
        }
        return flowBatchs;
    }

    @Override
    public boolean setState(FlowBatchId id, FlowBatchState state,
            FlowBatchState expectedState) {
        final String key = checkNotNull(
                id, "FlowBatchId is not specified.").toString();
        final byte[] oldValue = KryoFactory.serialize(expectedState);
        final byte[] newValue = KryoFactory.serialize(state);

        if (!flowBatchMap.containsKey(key)) {
            return false;
        }

        if (expectedState == FlowBatchState.SUBMITTED) {
            // The absence of the key means SUBMITTED state.
            return flowBatchStateMap.putIfAbsent(key, newValue) == null;
        }

        return flowBatchStateMap.replace(key, oldValue, newValue);
    }

    @Override
    public FlowBatchState getState(FlowBatchId id) {
        final String key = checkNotNull(
                id, "FlowBatchId is not specified.").toString();
        if (!flowBatchMap.containsKey(key)) {
            return null;
        }

        final byte[] buf = flowBatchStateMap.get(key);
        if (buf == null) {
            // The absence of the key means SUBMITTED state.
            return FlowBatchState.SUBMITTED;
        }
        return KryoFactory.deserialize(buf);
    }

    @Override
    public void addListener(FlowBatchMapEventListener listener) {
        dispatcher.addListener(listener);
    }

    @Override
    public void removeListener(FlowBatchMapEventListener listener) {
        dispatcher.removeListener(listener);
    }

    @Override
    public boolean isLocal(FlowBatchId id) {
        return flowBatchMap.localKeySet().contains(checkNotNull(id).toString());
    }
}
