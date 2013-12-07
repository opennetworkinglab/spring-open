package net.onrc.onos.datagrid;

import java.util.Collection;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.ofcontroller.flowmanager.IFlowEventHandlerService;
import net.onrc.onos.ofcontroller.proxyarp.ArpMessage;
import net.onrc.onos.ofcontroller.proxyarp.IArpEventHandler;
import net.onrc.onos.ofcontroller.topology.TopologyElement;
import net.onrc.onos.ofcontroller.util.FlowEntry;
import net.onrc.onos.ofcontroller.util.FlowEntryId;
import net.onrc.onos.ofcontroller.util.FlowId;
import net.onrc.onos.ofcontroller.util.FlowPath;

/**
 * Interface for providing Datagrid Service to other modules.
 */
public interface IDatagridService extends IFloodlightService {
    /**
     * Register Flow Event Handler Service for receiving Flow-related
     * notifications.
     *
     * NOTE: Only a single Flow Event Handler Service can be registered.
     *
     * @param flowEventHandlerService the Flow Event Handler Service to register.
     */
    void registerFlowEventHandlerService(IFlowEventHandlerService flowEventHandlerService);

    /**
     * De-register Flow Event Handler Service for receiving Flow-related
     * notifications.
     *
     * NOTE: Only a single Flow Event Handler Service can be registered.
     *
     * @param flowEventHandlerService the Flow Event Handler Service to
     * de-register.
     */
    void deregisterFlowEventHandlerService(IFlowEventHandlerService flowEventHandlerService);

    /**
     * Register event handler for ARP events.
     * 
     * @param arpEventHandler The ARP event handler to register.
     */
    public void registerArpEventHandler(IArpEventHandler arpEventHandler);
    
    /**
     * De-register event handler service for ARP events.
     * 
     * @param arpEventHandler The ARP event handler to de-register.
     */
    public void deregisterArpEventHandler(IArpEventHandler arpEventHandler);

    /**
     * Get all Flows that are currently in the datagrid.
     *
     * @return all Flows that are currently in the datagrid.
     */
    Collection<FlowPath> getAllFlows();

    /**
     * Get a Flow for a given Flow ID.
     *
     * @param flowId the Flow ID of the Flow to get.
     * @return the Flow if found, otherwise null.
     */
    FlowPath getFlow(FlowId flowId);

    /**
     * Send a notification that a Flow is added.
     *
     * @param flowPath the Flow that is added.
     */
    void notificationSendFlowAdded(FlowPath flowPath);

    /**
     * Send a notification that a Flow is removed.
     *
     * @param flowId the Flow ID of the Flow that is removed.
     */
    void notificationSendFlowRemoved(FlowId flowId);

    /**
     * Send a notification that a Flow is updated.
     *
     * @param flowPath the Flow that is updated.
     */
    void notificationSendFlowUpdated(FlowPath flowPath);

    /**
     * Send a notification that all Flows are removed.
     */
    void notificationSendAllFlowsRemoved();

    /**
     * Get all Flow Entries that are currently in the datagrid.
     *
     * @return all Flow Entries that are currently in the datagrid.
     */
    Collection<FlowEntry> getAllFlowEntries();

    /**
     * Get a Flow Entry for a given Flow Entry ID.
     *
     * @param flowEntryId the Flow Entry ID of the Flow Entry to get.
     * @return the Flow Entry if found, otherwise null.
     */
    FlowEntry getFlowEntry(FlowEntryId flowEntryId);

    /**
     * Send a notification that a FlowEntry is added.
     *
     * @param flowEntry the FlowEntry that is added.
     */
    void notificationSendFlowEntryAdded(FlowEntry flowEntry);

    /**
     * Send a notification that a FlowEntry is removed.
     *
     * @param flowEntryId the FlowEntry ID of the FlowEntry that is removed.
     */
    void notificationSendFlowEntryRemoved(FlowEntryId flowEntryId);

    /**
     * Send a notification that a FlowEntry is updated.
     *
     * @param flowEntry the FlowEntry that is updated.
     */
    void notificationSendFlowEntryUpdated(FlowEntry flowEntry);

    /**
     * Send a notification that all Flow Entries are removed.
     */
    void notificationSendAllFlowEntriesRemoved();

    /**
     * Get all Topology Elements that are currently in the datagrid.
     *
     * @return all Topology Elements that are currently in the datagrid.
     */
    Collection<TopologyElement> getAllTopologyElements();

    /**
     * Send a notification that a Topology Element is added.
     *
     * @param topologyElement the Topology Element that is added.
     */
    void notificationSendTopologyElementAdded(TopologyElement topologyElement);

    /**
     * Send a notification that a Topology Element is removed.
     *
     * @param topologyElement the Topology Element that is removed.
     */
    void notificationSendTopologyElementRemoved(TopologyElement topologyElement);

    /**
     * Send a notification that a Topology Element is updated.
     *
     * @param topologyElement the Topology Element that is updated.
     */
    void notificationSendTopologyElementUpdated(TopologyElement topologyElement);

    /**
     * Send a notification that all Topology Elements are removed.
     */
    void notificationSendAllTopologyElementsRemoved();
    
    /**
     * Send an ARP request to other ONOS instances
     * @param arpRequest The request packet to send
     */
    public void sendArpRequest(ArpMessage arpMessage);
}