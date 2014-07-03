/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
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
 */

package net.onrc.onos.core.linkdiscovery;

import net.onrc.onos.core.linkdiscovery.ILinkDiscovery.LinkType;

import com.google.common.primitives.Longs;

/**
 * Records information about a link.
 */
public final class LinkInfo {

    /**
     * The port states stored here are topology's last knowledge of
     * the state of the port. This mostly mirrors the state
     * maintained in the port list in IOFSwitch (i.e. the one returned
     * from getPort), except that during a port status message the
     * IOFSwitch port state will already have been updated with the
     * new port state, so topology needs to keep its own copy so that
     * it can determine if the port state has changed and therefore
     * requires the new state to be written to storage.
     *
     * Note the port state values are defined in the OF 1.0 spec.
     * These will change in some way once we move to OF 1.3.
     */
    private final int srcPortState;
    private final int dstPortState;

    private final long firstSeenTime;
    private final long lastLldpReceivedTime;

    /**
     * Constructs a LinkInfo object.
     *
     * @param firstSeenTime the timestamp when the link was first seen
     * @param lastLldpReceivedTime the timestamp when the link was last seen
     * @param srcPortState the port state of the source port
     * @param dstPortState the port state of the destination port
     */
    public LinkInfo(long firstSeenTime,
            long lastLldpReceivedTime,
            int srcPortState,
            int dstPortState) {
        this.srcPortState = srcPortState;
        this.dstPortState = dstPortState;
        this.firstSeenTime = firstSeenTime;
        this.lastLldpReceivedTime = lastLldpReceivedTime;
    }

    /**
     * Gets the timestamp when the link was first seen.
     *
     * @return the first seen timestamp
     */
    public long getFirstSeenTime() {
        return firstSeenTime;
    }

    /**
     * Gets the timestamp when the link was last seen.
     *
     * @return the last seen timestamp
     */
    public long getLastProbeReceivedTime() {
        return lastLldpReceivedTime;
    }

    /**
     * Gets the state of the source port.
     *
     * @return the source port state, as defined in the OF1.0 spec
     */
    public int getSrcPortState() {
        return srcPortState;
    }

    /**
     * Gets the state of the destination port.
     *
     * @return the destination port state, as defined in the OF1.0 spec
     */
    public int getDstPortState() {
        return dstPortState;
    }

    /**
     * Gets the link type.
     *
     * @return the link type
     * @see LinkType
     */
    public LinkType getLinkType() {
        return LinkType.DIRECT_LINK;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5557;
        int result = 1;
        result = prime * result + Longs.hashCode(firstSeenTime);
        result = prime * result + Longs.hashCode(lastLldpReceivedTime);
        result = prime * result + srcPortState;
        result = prime * result + dstPortState;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LinkInfo)) {
            return false;
        }

        LinkInfo other = (LinkInfo) obj;

        return firstSeenTime == other.firstSeenTime &&
               lastLldpReceivedTime == other.lastLldpReceivedTime &&
               srcPortState == other.srcPortState &&
               dstPortState == other.dstPortState;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LinkInfo [unicastValidTime=" + lastLldpReceivedTime
                + ", srcPortState=" + srcPortState
                + ", dstPortState=" + dstPortState
                + "]";
    }
}
