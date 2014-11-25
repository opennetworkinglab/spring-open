/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.internal.Controller.Counters;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.util.OnosInstanceId;

import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFType;

/**
 * The interface exposed by the core bundle that allows you to interact
 * with connected switches.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IFloodlightProviderService extends IFloodlightService {

    /**
     * A value stored in the floodlight context containing a parsed packet
     * representation of the payload of a packet-in message.
     */
    public static final String CONTEXT_PI_PAYLOAD =
            "net.floodlightcontroller.core.IFloodlightProvider.piPayload";
    public static final String CONTEXT_PI_INPORT =
            "net.floodlightcontroller.core.IFloodlightProvider.piInPort";;
    /**
     * The role of the controller as it pertains to a particular switch.
     * Note that this definition of the role enum is different from the
     * OF1.3 definition. It is maintained here to be backward compatible to
     * earlier versions of the controller code. This enum is translated
     * to the OF1.3 enum, before role messages are sent to the switch.
     * See sendRoleRequestMessage method in OFSwitchImpl
     */
    public static enum Role {
        EQUAL(OFControllerRole.ROLE_EQUAL),
        MASTER(OFControllerRole.ROLE_MASTER),
        SLAVE(OFControllerRole.ROLE_SLAVE);

        private final int nxRole;

        private Role(OFControllerRole nxRole) {
            this.nxRole = nxRole.ordinal();
        }
        /*
        private static Map<Integer,Role> nxRoleToEnum
                = new HashMap<Integer,Role>();
        static {
            for(Role r: Role.values())
                nxRoleToEnum.put(r.toNxRole(), r);
        }
        public int toNxRole() {
            return nxRole;
        }
        // Return the enum representing the given nxRole or null if no
        // such role exists
        public static Role fromNxRole(int nxRole) {
            return nxRoleToEnum.get(nxRole);
        }*/
    }

    /**
     * A FloodlightContextStore object that can be used to retrieve the
     * packet-in payload
     */
    public static final FloodlightContextStore<Ethernet> bcStore =
            new FloodlightContextStore<Ethernet>();


    //************************
    //  Controller related
    //************************

    /**
     * Get the current mapping of controller IDs to their IP addresses
     * Returns a copy of the current mapping.
     *
     * @see IHAListener
     */
    public Map<String, String> getControllerNodeIPs();

    /**
     * Return the controller start time in  milliseconds
     *
     * @return
     */
    public long getSystemStartTime();

    /**
     * Run the main I/O loop of the Controller.
     */
    public void run();

//    /**
//     * Terminate the process
//     */
//    public void terminate();

    //************************
    //  OF Message Listener related
    //************************

    /**
     * Adds an OpenFlow message listener
     *
     * @param type     The OFType the component wants to listen for
     * @param listener The component that wants to listen for the message
     */
    public void addOFMessageListener(OFType type, IOFMessageListener listener);

    /**
     * Removes an OpenFlow message listener
     *
     * @param type     The OFType the component no long wants to listen for
     * @param listener The component that no longer wants to receive the message
     */
    public void removeOFMessageListener(OFType type, IOFMessageListener listener);

    /**
     * Return a non-modifiable list of all current listeners
     *
     * @return listeners
     */
    public Map<OFType, List<IOFMessageListener>> getListeners();

    //************************
    //  Switch & SwitchListener related
    //************************

    /**
     * Returns an unmodifiable map of all actively connected OpenFlow switches. This doesn't
     * contain switches that are connected but the controller's in the slave role.
     *
     * @return the set of actively connected switches
     */
    public Map<Long, IOFSwitch> getSwitches();

    /**
     * Configure controller to always clear the flow table on the switch,
     * when it connects to controller. This will be true for first time switch
     * reconnect, as well as a switch re-attaching to Controller after HA
     * switch over to ACTIVE role
     */
    public void setAlwaysClearFlowsOnSwAdd(boolean value);

    /**
     * Gets the unique ID used to identify this ONOS instance in the cluster.
     *
     * @return ONOS Instance ID.
     */
    public OnosInstanceId getOnosInstanceId();

    /**
     * Add a switch listener
     *
     * @param listener The module that wants to listen for events
     */
    public void addOFSwitchListener(IOFSwitchListener listener);

    /**
     * Remove a switch listener
     *
     * @param listener The The module that no longer wants to listen for events
     */
    public void removeOFSwitchListener(IOFSwitchListener listener);


    Set<Long> getAllSwitchDpids();

    IOFSwitch getSwitch(long dpid);

    /**
     * Record a switch event in in-memory debug-event
     *
     * @param switchDPID
     * @param reason Reason for this event
     * @param flushNow see debug-event flushing in IDebugEventService
     */
    public void addSwitchEvent(long switchDPID, String reason, boolean flushNow);

    Set<Long> getAllMasterSwitchDpids();

    Set<Long> getAllSlaveSwitchDpids();

    IOFSwitch getMasterSwitch(long dpid);

    IOFSwitch getSlaveSwitch(long dpid);

    void setAlwaysClearFlowsOnSwActivate(boolean value);

    //************************
    //  Utility methods
    //************************

    /**
     * Publish updates to Controller updates queue
     *
     * @param IUpdate
     */
    public void publishUpdate(IUpdate update);

//    /**
//     * Re-injects an OFMessage back into the packet processing chain
//     *
//     * @param sw  The switch to use for the message
//     * @param msg the message to inject
//     * @return True if successfully re-injected, false otherwise
//     */
//    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg);
//
//    /**
//     * Re-injects an OFMessage back into the packet processing chain
//     *
//     * @param sw       The switch to use for the message
//     * @param msg      the message to inject
//     * @param bContext a floodlight context to use if required
//     * @return True if successfully re-injected, false otherwise
//     */
//    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg,
//                                   FloodlightContext bContext);
//
//    /**
//     * Process written messages through the message listeners for the controller
//     *
//     * @param sw The switch being written to
//     * @param m  the message
//     * @param bc any accompanying context object
//     */
//    public void handleOutgoingMessage(IOFSwitch sw, OFMessage m,
//                                      FloodlightContext bc);


    /**
     * Return the default set of counters
     * @return
     */
    public Counters getCounters();



    Map<String, Long> getMemory();

    Long getUptime();


    public INetworkConfigService getNetworkConfigService();
}
