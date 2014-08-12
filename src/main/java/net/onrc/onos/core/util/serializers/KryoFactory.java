package net.onrc.onos.core.util.serializers;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.apps.proxyarp.ArpCacheNotification;
import net.onrc.onos.apps.proxyarp.ArpReplyNotification;
import net.onrc.onos.core.hostmanager.Host;
import net.onrc.onos.core.intent.ConstrainedShortestPathIntent;
import net.onrc.onos.core.intent.ErrorIntent;
import net.onrc.onos.core.intent.Intent;
import net.onrc.onos.core.intent.IntentOperation;
import net.onrc.onos.core.intent.IntentOperationList;
import net.onrc.onos.core.intent.Path;
import net.onrc.onos.core.intent.PathIntent;
import net.onrc.onos.core.intent.ShortestPathIntent;
import net.onrc.onos.core.intent.runtime.IntentStateList;
import net.onrc.onos.core.packetservice.BroadcastPacketOutNotification;
import net.onrc.onos.core.packetservice.PacketOutNotification;
import net.onrc.onos.core.packetservice.SinglePacketOutNotification;
import net.onrc.onos.core.topology.AdminStatus;
import net.onrc.onos.core.topology.ConfigState;
import net.onrc.onos.core.topology.HostEvent;
import net.onrc.onos.core.topology.LinkEvent;
import net.onrc.onos.core.topology.MastershipEvent;
import net.onrc.onos.core.topology.PortEvent;
import net.onrc.onos.core.topology.SwitchEvent;
import net.onrc.onos.core.topology.TopologyBatchOperation;
import net.onrc.onos.core.topology.TopologyElement;
import net.onrc.onos.core.topology.TopologyEvent;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.IPv4;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.IPv6;
import net.onrc.onos.core.util.IPv6Net;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import com.esotericsoftware.kryo.Kryo;

/**
 * Class factory for allocating Kryo instances for
 * serialization/deserialization of classes.
 */
public class KryoFactory {
    private static final int DEFAULT_PREALLOCATIONS = 100;
    private ArrayList<Kryo> kryoList = new ArrayList<Kryo>();

    /**
     * Default constructor.
     * <p/>
     * Preallocates {@code DEFAULT_PREALLOCATIONS} Kryo instances.
     */
    public KryoFactory() {
        this(DEFAULT_PREALLOCATIONS);
    }

    /**
     * Constructor to explicitly specify number of Kryo instances to pool.
     *
     * @param initialCapacity number of Kryo instance to preallocate
     */
    public KryoFactory(final int initialCapacity) {
        // Preallocate
        kryoList.ensureCapacity(initialCapacity);
        for (int i = 0; i < initialCapacity; i++) {
            Kryo kryo = newKryoObject();
            kryoList.add(kryo);
        }
    }

    /**
     * Gets a new Kryo object.
     *
     * @return the Kryo object.
     */
    public Kryo newKryo() {
        return newDeleteKryo(null);
    }

    /**
     * Deletes an existing Kryo object.
     *
     * @param deleteKryo the object to delete.
     */
    public void deleteKryo(Kryo deleteKryo) {
        newDeleteKryo(deleteKryo);
    }

    /**
     * Creates or deletes a Kryo object.
     *
     * @param deleteKryo if null, then allocate and return a new object,
     *                   otherwise delete the provided object.
     * @return a new Kryo object if needed, otherwise null.
     */
    private synchronized Kryo newDeleteKryo(Kryo deleteKryo) {
        if (deleteKryo != null) {
            // Delete an entry by moving it back to the buffer
            kryoList.add(deleteKryo);
            return null;
        } else {
            Kryo kryo = null;
            if (kryoList.isEmpty()) {
                // Preallocate
                for (int i = 0; i < 100; i++) {
                    kryo = newKryoObject();
                    kryoList.add(kryo);
                }
            }

            kryo = kryoList.remove(kryoList.size() - 1);
            return kryo;
        }
    }

    /**
     * Creates and initializes a new Kryo object.
     *<p>
     * NOTE: This operation can be slow and should be used only if the
     * application needs a single Kryo instance (e.g., during startup).
     * For faster allocation, the application should use #newKryo()
     * and #deleteKryo() factory methods.
     *
     * @return the created Kryo object.
     */
    public static Kryo newKryoObject() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
        //
        // WARNING: Order of register() calls affects serialized bytes.
        //  - Do no insert new entry in the middle, always add to the end.
        //  - Do not simply remove existing entry
        //

        // kryo.setReferences(false);
        //
        kryo.register(ArrayList.class);

        // FlowPath and related classes
        kryo.register(Dpid.class);
        kryo.register(IPv4.class);
        kryo.register(IPv4Net.class);
        kryo.register(IPv6.class);
        kryo.register(IPv6Net.class);
        kryo.register(byte[].class);
        kryo.register(MACAddress.class);
        kryo.register(PortNumber.class);
        kryo.register(SwitchPort.class);
        kryo.register(LinkTuple.class);

        // New data model-related classes
        kryo.register(AdminStatus.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(ConfigState.class);
        kryo.register(HostEvent.class);
        kryo.register(LinkedList.class);
        kryo.register(LinkEvent.class);
        kryo.register(MastershipEvent.class);
        kryo.register(OnosInstanceId.class);
        kryo.register(PortEvent.class);
        kryo.register(Role.class);
        kryo.register(SwitchEvent.class);
        kryo.register(TopologyBatchOperation.class);
        kryo.register(TopologyBatchOperation.Operator.class);
        kryo.register(TopologyElement.class);
        kryo.register(TopologyEvent.class);

        // Intent-related classes
        kryo.register(Path.class);
        kryo.register(Intent.class);
        kryo.register(Intent.IntentState.class);
        kryo.register(PathIntent.class);
        kryo.register(ShortestPathIntent.class);
        kryo.register(ConstrainedShortestPathIntent.class);
        kryo.register(ErrorIntent.class);
        kryo.register(ErrorIntent.ErrorType.class);
        kryo.register(IntentOperation.class);
        kryo.register(IntentOperation.Operator.class);
        kryo.register(IntentOperationList.class);
        kryo.register(IntentStateList.class);
        kryo.register(HashMap.class);

        // New intent-related classes
        kryo.register(BatchOperationEntry.class);

        // Host-related classes
        kryo.register(HashSet.class);
        kryo.register(Host.class);
        kryo.register(Date.class);

        // ProxyArp-related classes
        kryo.register(PacketOutNotification.class);
        kryo.register(BroadcastPacketOutNotification.class);
        kryo.register(SinglePacketOutNotification.class);
        kryo.register(ArpReplyNotification.class);
        kryo.register(ArpCacheNotification.class);
        // TODO check if InetAddress related is still used
        // TODO check if InetAddress can be correctly serialized
        kryo.register(InetAddress.class);
        kryo.register(Inet4Address.class);

        return kryo;
    }
}
