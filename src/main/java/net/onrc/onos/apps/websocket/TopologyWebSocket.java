package net.onrc.onos.apps.websocket;

import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.Topology;
import net.onrc.onos.core.topology.TopologyEvents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Topology WebSocket class. A single instance is allocated per client.
 *
 * <p>
 * The object lifecycle is as follows:
 * <p>
 * <ol>
 * <li> WebSocket Client opens a WebSocket connection to the corresponding
 *    @ServerEndpoint: an instance for this class is created, and method
 *    @OnOpen is called. </li>
 * <li> If the client sends a text message: method @OnMessage for String
 *    argument is called. </li>
 * <li> If the client sends a binary message: method @OnMessage
 *    for ByteBuffer argument is called. </li>
 * <li> If the client sends WebSocket Pong message: method @OnMessage
 *    for PongMessage is called. </li>
 * <li> If the client closes the connection: method @OnClose is called. </li>
 * <li> If there is any error with the connection: method @OnError is
 *    called. </li>
 * </ol>
 *<p>
 * When the client opens the WebSocket, the server sends back the whole
 * topology first. From that moment on, the server sends topology events
 * (deltas) if there are any changes in the topology. Currently, all objects
 * are encoded in JSON.
 */
@ServerEndpoint(value = "/topology")
public class TopologyWebSocket extends Thread implements ITopologyListener {

    private static final Logger log =
        LoggerFactory.getLogger(TopologyWebSocket.class);
    private BlockingQueue<TopologyEvents> topologyEventsQueue =
        new LinkedBlockingQueue<>();
    private Session socketSession;
    private boolean isOpen = false;

    /**
     * Shutdown the socket.
     */
    private void shutdown() {
        ITopologyService topologyService = WebSocketManager.topologyService;
        topologyService.deregisterTopologyListener(this);
        this.isOpen = false;            // Stop the thread
    }

    /**
     * Topology events have been generated.
     *
     * @param topologyEvents the generated Topology Events
     * @see TopologyEvents
     */
    @Override
    public void topologyEvents(TopologyEvents topologyEvents) {
        // The topologyEvents object is a deep copy so we can add it as-is
        this.topologyEventsQueue.add(topologyEvents);
    }

    /**
     * Run the thread.
     */
    @Override
    public void run() {
        this.setName("TopologyWebSocket " + this.getId());
        ObjectMapper mapper = new ObjectMapper();

        //
        // The main loop for sending events to the clients
        //
        while (this.isOpen && (!this.isInterrupted())) {
            String eventsJson = null;
            try {
                TopologyEvents events = topologyEventsQueue.take();
                eventsJson = mapper.writeValueAsString(events);
                if (eventsJson != null) {
                    socketSession.getBasicRemote().sendText(eventsJson);
                }
            } catch (IOException e) {
                log.debug("Exception sending TopologyWebSocket events: ", e);
            } catch (Exception exception) {
                log.debug("Exception processing TopologyWebSocket events: ",
                          exception);
            }
        }
    }

    /**
     * Connection opened by a client.
     *
     * @param session the WebSocket session for the connection.
     * @param conf the Endpoint configuration.
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf) {
        log.debug("WebSocket new session: {}", session.getId());
        this.isOpen = true;

        //
        // Initialization and Topology Service registration
        //
        this.socketSession = session;
        ObjectMapper mapper = new ObjectMapper();
        String topologyJson = null;
        ITopologyService topologyService = WebSocketManager.topologyService;
        topologyService.registerTopologyListener(this);

        //
        // Get the initial topology and encode it in JSON
        //
        Topology topology = topologyService.getTopology();
        topology.acquireReadLock();
        try {
            topologyJson = mapper.writeValueAsString(topology);
        } catch (IOException e) {
            log.debug("Exception encoding topology as JSON: ", e);
        } finally {
            topology.releaseReadLock();
        }

        //
        // Send the initial topology
        //
        if (topologyJson != null) {
            try {
                session.getBasicRemote().sendText(topologyJson);
            } catch (IOException e) {
                log.debug("Exception sending TopologyWebSocket topology: ", e);
            }
        }

        // Start the thread
        start();
    }

    /**
     * Received a text message.
     *
     * @param session the WebSocket session for the connection.
     * @param msg the received message.
     */
    @OnMessage
    public void onTextMessage(Session session, String msg) {
        log.debug("WebSocket Text message received: {}", msg);

        // TODO: Sample code below for sending a response back
        // NOTE: The transmission here must be synchronized by
        // by the transmission by another thread within the run() method.
        //
        // String result = msg + " (from your server)";
        // session.getBasicRemote().sendText(result);
        //
        // RemoteEndpoint.Basic basic = session.getBasicRemote();
        // RemoteEndpoint.Async async = session.getAsyncRemote();
        // session.getAsyncRemote().sendBinary(ByteBuffer data);
        // session.getAsyncRemote().sendPing(ByteBuffer appData);
        // session.getAsyncRemote().sendPong(ByteBuffer appData);
        //
    }

    /**
     * Received a binary message.
     *
     * @param session the WebSocket session for the connection.
     * @param msg the received message.
     */
    @OnMessage
    public void onBinaryMessage(Session session, ByteBuffer msg) {
        log.debug("WebSocket Binary message received: {}", msg);
    }

    /**
     * Received a Pong message.
     *
     * @param session the WebSocket session for the connection.
     * @param msg the received message.
     */
    @OnMessage
    public void onPongMessage(Session session, PongMessage msg) {
        log.debug("WebSocket Pong message received: {}",
                  msg.getApplicationData());
    }

    /**
     * Error occured on the connection.
     *
     * @param session the WebSocket session for the connection.
     * @param error the occured error.
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.debug("WebSocket session error: ", error);
        shutdown();
    }

    /**
     * Connection closed.
     *
     * @param session the WebSocket session for the connection.
     * @param reason the reason for closing the connection.
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        log.debug("WebSocket session closed: {}", reason);
        shutdown();
    }
}
