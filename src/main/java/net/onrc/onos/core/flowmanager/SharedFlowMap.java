package net.onrc.onos.core.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.onrc.onos.api.flowmanager.Flow;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowState;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.util.serializers.KryoFactory;

import com.hazelcast.core.IMap;

/**
 * Manages the distributed shared flow map.
 */
class SharedFlowMap implements FlowMap {
    private static final String FLOWMAP_NAME = "flow_map";
    private static final String FLOWSTATEMAP_NAME = "flowstate_map";

    private final IMap<String, byte[]> flowMap;
    private final IMap<String, byte[]> flowStateMap;
    private final SharedFlowMapEventDispatcher dispatcher;

    /**
     * Creates instance using {@link ISharedCollectionsService} service.
     *
     * @param service the {@link ISharedCollectionsService} service
     */
    SharedFlowMap(ISharedCollectionsService service) {
        this.flowMap = checkNotNull(service.getConcurrentMap(
                FLOWMAP_NAME, String.class, byte[].class));
        this.flowStateMap = checkNotNull(service.getConcurrentMap(
                FLOWSTATEMAP_NAME, String.class, byte[].class));
        this.dispatcher = new SharedFlowMapEventDispatcher(flowMap, flowStateMap);
    }

    @Override
    public Flow get(FlowId id) {
        byte[] buf = flowMap.get(checkNotNull(id).toString());
        if (buf == null) {
            return null;
        }
        return KryoFactory.deserialize(buf);
    }

    @Override
    public boolean put(Flow flow) {
        checkNotNull(flow);
        byte[] buf = KryoFactory.serialize(flow);
        flowMap.set(flow.getId().toString(), buf);
        return true;
    }

    @Override
    public Flow remove(FlowId id) {
        String flowIdStr = checkNotNull(id).toString();
        byte[] buf = flowMap.remove(flowIdStr);
        if (buf == null) {
            return null;
        }
        flowStateMap.remove(flowIdStr);
        return KryoFactory.deserialize(buf);
    }

    @Override
    public Set<Flow> getAll() {
        Set<Flow> flows = new HashSet<>();
        for (Entry<String, byte[]> entry : flowMap.entrySet()) {
            flows.add((Flow) KryoFactory.deserialize(entry.getValue()));
        }
        return flows;
    }

    @Override
    public boolean setState(FlowId id, FlowState state, FlowState expectedState) {
        final String key = checkNotNull(id, "FlowId is not specified.").toString();
        final byte[] oldValue = KryoFactory.serialize(expectedState);
        final byte[] newValue = KryoFactory.serialize(state);

        if (!flowMap.containsKey(key)) {
            return false;
        }

        if (expectedState == FlowState.SUBMITTED) {
            // The absence of the key means SUBMITTED state.
            return flowStateMap.putIfAbsent(key, newValue) == null;
        }

        return flowStateMap.replace(key, oldValue, newValue);
    };

    @Override
    public FlowState getState(FlowId id) {
        final String key = checkNotNull(id, "FlowId is not specified.").toString();
        if (!flowMap.containsKey(key)) {
            return null;
        }

        final byte[] buf = flowStateMap.get(key);
        if (buf == null) {
            // The absence of the key means SUBMITTED state.
            return FlowState.SUBMITTED;
        }
        return KryoFactory.deserialize(buf);
    }

    @Override
    public void addListener(FlowMapEventListener listener) {
        dispatcher.addListener(listener);
    }

    @Override
    public void removeListener(FlowMapEventListener listener) {
        dispatcher.removeListener(listener);
    }
}
