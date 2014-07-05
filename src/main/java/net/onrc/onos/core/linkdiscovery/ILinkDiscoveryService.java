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

package net.onrc.onos.core.linkdiscovery;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Interface to the link discovery module.
 */
public interface ILinkDiscoveryService extends IFloodlightService {
    /**
     * Represents the type of a link.
     * <p/>
     * This is a placeholder at the moment. Floodlight had defined a number of
     * different link types which are irrelevant to us now we no longer use
     * BDDP or support OpenFlow clusters.
     * Currently we have no differentiation of link types, but in the future we
     * may want to differentiate between intra-instance links and
     * inter-instance links.
     */
    public enum LinkType {
        DIRECT_LINK {
            @Override
            public String toString() {
                return "internal";
            }
        }
    }

    /**
     * Retrieves a map of all known link connections between OpenFlow switches
     * and the associated info (valid time, port states) for the link.
     */
    public Map<Link, LinkInfo> getLinks();

    /**
     * Adds a listener to listen for ILinkDiscoveryService messages.
     *
     * @param listener The listener that wants the notifications
     */
    public void addListener(ILinkDiscoveryListener listener);

    /**
     * Removes a link discovery listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ILinkDiscoveryListener listener);

    /**
     * Gets the set of switch ports on which link discovery is disabled.
     */
    public Set<NodePortTuple> getDiscoveryDisabledPorts();

    /**
     * Disables link discovery on a switch port. This method suppresses
     * discovery probes from being sent from the port, and deletes any existing
     * links that the discovery module has previously detected on the port.
     *
     * @param sw the dpid of the switch the port is on
     * @param port the port number to disable discovery on
     */
    public void disableDiscoveryOnPort(long sw, short port);

    /**
     * Enables link discovery on a switch port. Discovery probes will now be
     * sent from the port and any links on the port will be discovered.
     * <p/>
     * Note: All ports are enabled for discovery by default, however this
     * method is provided to re-enable link discovery if it had previously been
     * disabled on the port by a call to
     * {@link #disableDiscoveryOnPort(long, short)}.
     *
     * @param sw the dpid of the switch the port is on
     * @param port the port number to enable discovery on
     */
    public void enableDiscoveryOnPort(long sw, short port);
}
