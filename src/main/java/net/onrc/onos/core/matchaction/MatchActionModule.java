package net.onrc.onos.core.matchaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.flowprogrammer.IFlowPusherService;
import net.onrc.onos.core.matchaction.web.MatchActionWebRoutable;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.util.IdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Floodlight module for Match Action service.
 */

public class MatchActionModule implements MatchActionFloodlightService, IFloodlightModule {

    private static final Logger log = LoggerFactory
            .getLogger(MatchActionModule.class);
    private MatchActionComponent matchActionComponent;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services = new ArrayList<>();
        log.info("get module services");
        services.add(MatchActionFloodlightService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<>();
        impls.put(MatchActionFloodlightService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies = new ArrayList<>();
        dependencies.add(IDatagridService.class);
        dependencies.add(IFlowPusherService.class);
        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(IControllerRegistryService.class);
        dependencies.add(IRestApiService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        final IDatagridService datagrid = context.getServiceImpl(IDatagridService.class);
        final IFlowPusherService pusher = context.getServiceImpl(IFlowPusherService.class);
        final IFloodlightProviderService provider = context.getServiceImpl(IFloodlightProviderService.class);
        final IControllerRegistryService registry = context.getServiceImpl(IControllerRegistryService.class);
        final IRestApiService restApi = context.getServiceImpl(IRestApiService.class);

        restApi.addRestletRoutable(new MatchActionWebRoutable());

        matchActionComponent = new MatchActionComponent(datagrid, pusher, provider, registry);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        matchActionComponent.start();
    }

    @Override
    public boolean addMatchAction(MatchAction matchAction) {
        return matchActionComponent.addMatchAction(matchAction);
    }

    @Override
    public Set<MatchAction> getMatchActions() {
        return matchActionComponent.getMatchActions();
    }

    @Override
    public boolean executeOperations(MatchActionOperations operations) {
        matchActionComponent.installMatchActionOperations(operations);
        return false; // TODO define return value semantics
    }

    @Override
    public void setConflictDetectionPolicy(ConflictDetectionPolicy policy) {
        throw new UnsupportedOperationException("conflict detection not implemented yet");
    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        return null;
    }

    @Override
    public IdGenerator<MatchActionId> getMatchActionIdGenerator() {
        return matchActionComponent.getMatchActionIdGenerator();
    }

    @Override
    public IdGenerator<MatchActionOperationsId> getMatchActionOperationsIdGenerator() {
        return matchActionComponent.getMatchActionOperationsIdGenerator();
    }

    @Override
    public void addEventListener(EventListener listener) {
        // TODO
        log.warn("Could not add MatchAction EventListener: {}", listener);
    }

    @Override
    public void removeEventListener(EventListener listener) {
        // TODO
        log.warn("Could not remove MatchAction EventListener: {}", listener);
    }
}
