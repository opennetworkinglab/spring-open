/**
 *    Copyright 2011,2012, Big Switch Networks, Inc.
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

package net.onrc.onos.core.devicemanager;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import net.floodlightcontroller.util.MACAddress;



/**
 * An entity on the network is a visible trace of a device that corresponds
 * to a packet received from a particular interface on the edge of a network,
 * with a particular VLAN tag, and a particular MAC address, along with any
 * other packet characteristics we might want to consider as helpful for
 * disambiguating devices.
 * <p/>
 * Entities are the most basic element of devices; devices consist of one or
 * more entities.  Entities are immutable once created, except for the last
 * seen timestamp.
 *
 * @author readams
 */
public class OnosDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The MAC address associated with this entity.
     */
    private MACAddress macAddress;

    /**
     * The VLAN tag on this entity, or null if untagged.
     */
    private Short vlan;

    /**
     * The DPID of the switch for the ingress point for this entity,
     * or null if not present.
     */
    private Long switchDPID;

    /**
     * The port number of the switch for the ingress point for this entity,
     * or null if not present.
     */
    private Long switchPort;

    /**
     * The last time we observed this entity on the network.
     */
    private Date lastSeenTimestamp;

    private int hashCode = 0;

    // ************
    // Constructors
    // ************
    protected OnosDevice() {
    }

    /**
     * Create a new device and its information.
     *
     * @param macAddress mac address of this device
     * @param vlan vlan ID of this device
     * @param switchDPID switch DPID where the device is attached
     * @param switchPort port number where the device is attached
     * @param lastSeenTimestamp last packet-in time of this device
     */
    public OnosDevice(MACAddress macAddress, Short vlan, Long switchDPID,
            Long switchPort, Date lastSeenTimestamp) {
        this.macAddress = macAddress;
        this.vlan = vlan;
        this.switchDPID = switchDPID;
        this.switchPort = switchPort;
        if (lastSeenTimestamp != null) {
            this.lastSeenTimestamp = new Date(lastSeenTimestamp.getTime());
        } else {
            this.lastSeenTimestamp = null;
        }
    }

    // ***************
    // Getters/Setters
    // ***************

    public MACAddress getMacAddress() {
        return macAddress;
    }

    public Short getVlan() {
        return vlan;
    }

    public Long getSwitchDPID() {
        return switchDPID;
    }

    public Long getSwitchPort() {
        return switchPort;
    }

    public Date getLastSeenTimestamp() {
        if (this.lastSeenTimestamp == null) {
            return null;
        }
        return new Date(this.lastSeenTimestamp.getTime());
    }

    public void setLastSeenTimestamp(Date lastSeenTimestamp) {
        this.lastSeenTimestamp = new Date(lastSeenTimestamp.getTime());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        hashCode = 1;
        hashCode = prime * hashCode + (int) (macAddress.toLong() ^ (macAddress.toLong() >>> 32));
        hashCode = prime * hashCode + ((switchDPID == null) ? 0 : switchDPID.hashCode());
        hashCode = prime * hashCode + ((switchPort == null) ? 0 : switchPort.hashCode());
        hashCode = prime * hashCode + ((vlan == null) ? 0 : vlan.hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OnosDevice other = (OnosDevice) obj;
        if (hashCode() != other.hashCode()) {
            return false;
        }
        if (!Objects.equals(macAddress, other.macAddress)) {
            return false;
        }
        if (!Objects.equals(switchDPID, other.switchDPID)) {
            return false;
        }
        if (!Objects.equals(switchPort, other.switchPort)) {
            return false;
        }
        if (!Objects.equals(vlan, other.vlan)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OnosDevice [macAddress=");
        builder.append(macAddress.toString());
        builder.append(", vlan=");
        builder.append(vlan);
        builder.append(", switchDPID=");
        builder.append(switchDPID);
        builder.append(", switchPort=");
        builder.append(switchPort);
        builder.append(", lastSeenTimestamp=");
        builder.append(lastSeenTimestamp == null ? "null" : lastSeenTimestamp.getTime());
        builder.append("]");
        return builder.toString();
    }
}
