package net.onrc.onos.core.metrics;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.core.registry.IControllerRegistryService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Floodlight module to add the REST APIs for Metrics.
 */
public class OnosMetricsModule implements IFloodlightModule {

    private IRestApiService restApi;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
           getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<>();
        dependencies.add(IControllerRegistryService.class);
        dependencies.add(IRestApiService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        restApi = context.getServiceImpl(IRestApiService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        restApi.addRestletRoutable(new MetricsWebRoutable());
    }
}

