package net.onrc.onos.apps.websocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.core.topology.ITopologyService;

/**
 * The WebSocket module class.
 */
public class WebSocketModule implements IFloodlightModule, IWebSocketService {
    private WebSocketManager webSocketManager;
    private static final int DEFAULT_WEBSOCKET_PORT = 8081;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        List<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>();
        services.add(IWebSocketService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
    getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        impls.put(IWebSocketService.class, this);
        return impls;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        List<Class<? extends IFloodlightService>> dependencies =
                new ArrayList<Class<? extends IFloodlightService>>();
        dependencies.add(ITopologyService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        ITopologyService topologyService =
            context.getServiceImpl(ITopologyService.class);

        //
        // Read the configuration options
        //
        int webSocketPort = DEFAULT_WEBSOCKET_PORT;
        Map<String, String> configOptions = context.getConfigParams(this);
        String port = configOptions.get("port");
        if (port != null) {
            webSocketPort = Integer.parseInt(port);
        }

        // Initialize the WebSocketManager
        webSocketManager = new WebSocketManager(topologyService,
                                                webSocketPort);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        webSocketManager.startup();
    }
}
