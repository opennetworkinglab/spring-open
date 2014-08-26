package net.onrc.onos.apps.websocket;

import net.onrc.onos.core.topology.ITopologyListener;
import net.onrc.onos.core.topology.ITopologyService;
import net.onrc.onos.core.topology.TopologyEvents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
 *    {@literal @ServerEndpoint}: an instance for this class is created, and method
 *    {@literal @OnOpen} is called. </li>
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
    // Ping-related state
    private static final int PING_INTERVAL_SEC = 30;   // Ping every 30 secs
    private static final int MAX_MISSING_PING = 3;
    private int missingPing = 0;        // Pings to the client without a pong

    /**
     * Shutdown the socket.
     */
    private void shutdown() {
        ITopologyService topologyService = WebSocketManager.topologyService;
        topologyService.removeListener(this);
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
        // The topologyEvents object is immutable, so we can add it as-is
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
        // The main loop for sending events to the clients.
        //
        // If there are no events, we send periodic PING messages to discover
        // unreachable clients.
        //
        while (this.isOpen && (!this.isInterrupted())) {
            String eventsJson = null;
            try {
                TopologyEvents events =
                    topologyEventsQueue.poll(PING_INTERVAL_SEC,
                                             TimeUnit.SECONDS);
                if (events != null) {
                    // Send the event
                    eventsJson = mapper.writeValueAsString(events);
                    socketSession.getBasicRemote().sendText(eventsJson);
                    continue;
                }
                // Send a PING message
                missingPing++;
                if (missingPing > MAX_MISSING_PING) {
                    // Timeout
                    log.debug("WebSocket session timeout");
                    shutdown();
                } else {
                    String msg = "PING(TopologyWebsocket)";
                    ByteBuffer pingBuffer =
                        ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
                    socketSession.getBasicRemote().sendPing(pingBuffer);
                }
            } catch (IOException e) {
                log.debug("Exception sending TopologyWebSocket events: ", e);
            } catch (InterruptedException e) {
                log.debug("TopologyWebSocket interrupted while waiting: ", e);
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
        ITopologyService topologyService = WebSocketManager.topologyService;
        topologyService.addListener(this, true);

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
        log.trace("WebSocket Pong message received for session: {}",
                  session.getId());
        missingPing = 0;
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
