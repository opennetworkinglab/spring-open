package net.onrc.onos.apps.websocket;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import net.onrc.onos.core.topology.ITopologyService;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The WebSocket manager class.
 * There is a single instance for all WebSocket endpoints.
 */
class WebSocketManager {
    protected static ITopologyService topologyService;

    private static final Logger log =
        LoggerFactory.getLogger(WebSocketManager.class);
    private int webSocketPort;
    private JettyServer jettyServer;

    /**
     * Constructor.
     *
     * @param topologyService the Topology Service to use.
     * @param webSocketPort the WebSocket port to use.
     */
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
                        justification = "The writing to WebSocketManager.topologyService happens only once on startup")
    WebSocketManager(ITopologyService topologyService,
                     int webSocketPort) {
        WebSocketManager.topologyService = topologyService;
        this.webSocketPort = webSocketPort;
    }

    /**
     * Startup processing.
     */
    void startup() {
        log.debug("Starting WebSocket server on port {}", webSocketPort);

        jettyServer = new JettyServer(webSocketPort);
        this.jettyServer.start();
    }

    /**
     * Class for creating the WebSocket server and associated state.
     */
    static class JettyServer extends Thread {
        private Server server;
        private ServletContextHandler context;
        private ServerContainer container;

        /**
         * Constructor.
         *
         * @param port the port to listen on.
         */
        JettyServer(final int port) {
            server = new Server(port);

            // Initialize the context handler
            context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/ws/onos");
            server.setHandler(context);

            // Initialize the WebSocket layer
            container =
                WebSocketServerContainerInitializer.configureContext(context);
            try {
                container.addEndpoint(TopologyWebSocket.class);
            } catch (DeploymentException e) {
                log.debug("Exception adding WebSocket endpoint: ", e);
            }
        }

        /**
         * Run the thread.
         */
        @Override
        public void run() {
            try {
                this.server.start();
            } catch (final Exception e) {
                log.debug("Exception starting the WebSocket server: ", e);
            }
            try {
                this.server.join();
            } catch (final InterruptedException e) {
                log.debug("Exception joining the WebSocket server: ", e);
            }
        }
    }
}
