package net.onrc.onos.core.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.web.TopologyWebRoutable;

public class TopologyModule implements IFloodlightModule, ITopologyService {

    // This is initialized as a module for now

    private TopologyManager topologyManager;
    private IDatagridService datagridService;
    private IControllerRegistryService registryService;
    private IRestApiService restApi;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(ITopologyService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
    getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(ITopologyService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(IDatagridService.class);
        dependencies.add(IRestApiService.class);
        dependencies.add(IControllerRegistryService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        restApi = context.getServiceImpl(IRestApiService.class);
        datagridService = context.getServiceImpl(IDatagridService.class);
        registryService = context.getServiceImpl(IControllerRegistryService.class);
        topologyManager = new TopologyManager(registryService);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        restApi.addRestletRoutable(new TopologyWebRoutable());
        topologyManager.startup(datagridService);
    }

    @Override
    public MutableTopology getTopology() {
        return topologyManager.getTopology();
    }

    @Override
    public void addListener(ITopologyListener listener,
                            boolean startFromSnapshot) {
        topologyManager.addListener(listener, startFromSnapshot);
    }

    @Override
    public void removeListener(ITopologyListener listener) {
        topologyManager.removeListener(listener);
    }

    @Override
    public boolean publish(TopologyBatchOperation tbo) {
        // TODO: Replace with a call to the new (log-based) TopologyReplicator
        // return topologyReplicator.publish(tbo);
        return false;
    }
}
