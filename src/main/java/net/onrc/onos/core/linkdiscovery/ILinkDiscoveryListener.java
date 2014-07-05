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

/**
 * Provides callbacks for link discovery events.
 */
public interface ILinkDiscoveryListener {

    /**
     * Called when a new link is detected. A link discovery probe has been
     * received on a port, and the link was not previously known to the link
     * discovery manager.
     *
     * @param link the new link that was detected
     */
    public void linkAdded(Link link);

    /**
     * Called when a link is removed. The link may have been removed because it
     * timed out (no probes received on the destination port for an interval),
     * or because the port is no longer available for link discovery, either
     * because the switch was removed, the port went down, or link discovery
     * was disabled on the port.
     *
     * @param link the link that was removed
     */
    public void linkRemoved(Link link);
}
