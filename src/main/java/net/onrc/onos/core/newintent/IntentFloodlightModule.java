package net.onrc.onos.core.newintent;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.api.flowmanager.FlowIdGenerator;
import net.onrc.onos.api.flowmanager.FlowManagerFloodlightService;
import net.onrc.onos.api.flowmanager.FlowManagerService;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentCompiler;
import net.onrc.onos.api.newintent.IntentEventListener;
import net.onrc.onos.api.newintent.IntentFloodlightService;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentIdGenerator;
import net.onrc.onos.api.newintent.IntentInstaller;
import net.onrc.onos.api.newintent.IntentManager;
import net.onrc.onos.api.newintent.IntentOperations;
import net.onrc.onos.api.newintent.IntentState;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.api.newintent.PathIntent;
import net.onrc.onos.api.newintent.PointToPointIntent;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;
import net.onrc.onos.core.registry.IControllerRegistryService;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.util.IdBlockAllocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Floodlight module for Intent Service.
 */
public class IntentFloodlightModule implements IntentFloodlightService, IFloodlightModule {
    private IntentManager intentManager;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> services = new ArrayList<>();
        services.add(IntentFloodlightService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls = new HashMap<>();
        impls.put(IFloodlightService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Arrays.asList(
                ISharedCollectionsService.class,
                IControllerRegistryService.class,
                FlowManagerFloodlightService.class,
                ITopologyService.class
        );
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        intentManager =
                new IntentManagerRuntime(
                        context.getServiceImpl(ISharedCollectionsService.class)
                );

        IdBlockAllocator idBlockAllocator =
                context.getServiceImpl(IControllerRegistryService.class);
        IntentIdGenerator intentIdGenerator =
                new IdBlockAllocatorBasedIntentIdGenerator(idBlockAllocator);
        FlowManagerService flowManagerService =
                context.getServiceImpl(FlowManagerFloodlightService.class);
        FlowIdGenerator flowIdGenerator =
                flowManagerService.getFlowIdGenerator();

        ITopologyService topologyService =
                context.getServiceImpl(ITopologyService.class);

        registerDefaultCompilers(intentIdGenerator, flowIdGenerator, topologyService);
        registerDefaultInstallers(flowManagerService);
    }

    private void registerDefaultCompilers(IntentIdGenerator intentIdGenerator,
                                          FlowIdGenerator flowIdGenerator,
                                          ITopologyService topologyService) {
        intentManager.registerCompiler(PointToPointIntent.class,
                new PointToPointIntentCompiler(intentIdGenerator,
                        flowIdGenerator, topologyService));

        intentManager.registerCompiler(PathIntent.class,
                new PathIntentCompiler(intentIdGenerator, flowIdGenerator));

        intentManager.registerCompiler(MultiPointToSinglePointIntent.class,
                new MultiPointToSinglePointIntentCompiler(intentIdGenerator,
                        flowIdGenerator, topologyService));
    }

    private void registerDefaultInstallers(FlowManagerService flowManagerService) {
        intentManager.registerInstaller(PathFlowIntent.class,
                new PathFlowIntentInstaller(flowManagerService));
        intentManager.registerInstaller(SingleDstTreeFlowIntent.class,
                new SingleDstTreeFlowIntentInstaller(flowManagerService));
        intentManager.registerInstaller(SingleSrcTreeFlowIntent.class,
                new SingleSrcTreeFlowIntentInstaller(flowManagerService));
    }

    /*
     All methods defined in IntentFloodlightService are delegated to IntentManager
     implementation this class has. It helps to reduce the code size of this class
     (IntentModule) and to make IntentManager implementation more testable.
     */
    // All methods below are methods defined in IntentFloodlightService.
    @Override
    public void submit(Intent intent) {
        intentManager.submit(intent);
    }

    @Override
    public void withdraw(Intent intent) {
        intentManager.withdraw(intent);
    }

    @Override
    public void execute(IntentOperations operations) {
        intentManager.execute(operations);
    }

    @Override
    public Set<Intent> getIntents() {
        return intentManager.getIntents();
    }

    @Override
    public Intent getIntent(IntentId id) {
        return intentManager.getIntent(id);
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        return intentManager.getIntentState(id);
    }

    @Override
    public void addListener(IntentEventListener listener) {
        intentManager.addListener(listener);
    }

    @Override
    public void removeListener(IntentEventListener listener) {
        intentManager.removeListener(listener);
    }

    @Override
    public <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler) {
        intentManager.registerCompiler(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        intentManager.unregisterCompiler(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return intentManager.getCompilers();
    }

    @Override
    public <T extends InstallableIntent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        intentManager.registerInstaller(cls, installer);
    }

    @Override
    public <T extends InstallableIntent> void unregisterInstaller(Class<T> cls) {
        intentManager.unregisterInstaller(cls);
    }

    @Override
    public Map<Class<? extends InstallableIntent>, IntentInstaller<? extends InstallableIntent>> getInstallers() {
        return intentManager.getInstallers();
    }
}
