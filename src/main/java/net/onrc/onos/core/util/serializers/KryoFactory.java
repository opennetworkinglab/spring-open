package net.onrc.onos.core.util.serializers;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.concurrent.ThreadSafe;

import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.api.flowmanager.FlowBatchOperation;
import net.onrc.onos.api.flowmanager.FlowBatchState;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.FlowState;
import net.onrc.onos.api.flowmanager.OpticalPathFlow;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.SingleDstTreeFlow;
import net.onrc.onos.api.flowmanager.Tree;
import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.ConnectivityIntent;
import net.onrc.onos.api.newintent.IntentEvent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentState;
import net.onrc.onos.api.newintent.MultiPointToSinglePointIntent;
import net.onrc.onos.api.newintent.OpticalConnectivityIntent;
import net.onrc.onos.api.newintent.PacketConnectivityIntent;
import net.onrc.onos.api.newintent.PointToPointIntent;
import net.onrc.onos.api.newintent.SinglePointToMultiPointIntent;
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
import net.onrc.onos.core.matchaction.action.ModifyDstMacAction;
import net.onrc.onos.core.matchaction.action.ModifySrcMacAction;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.newintent.IntentCompilationResult;
import net.onrc.onos.core.newintent.PathFlowIntent;
import net.onrc.onos.core.newintent.SingleDstTreeFlowIntent;
import net.onrc.onos.core.newintent.SingleSrcTreeFlowIntent;
import net.onrc.onos.core.newintent.TestIntent;
import net.onrc.onos.core.newintent.TestSubclassIntent;
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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Class factory for allocating Kryo instances for
 * serialization/deserialization of classes.
 */
@ThreadSafe
public class KryoFactory {

    private static final int DEFAULT_PREALLOCATIONS = 100;
    /**
     * Default buffer size used for serialization.
     *
     * @see #serialize(Object)
     */
    public static final int DEFAULT_BUFFER_SIZE = 1 * 1000 * 1000;

    private static final KryoFactory GLOBAL_POOL = new KryoFactory();

    private ConcurrentLinkedQueue<Kryo> kryoList = new ConcurrentLinkedQueue<Kryo>();

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
        Kryo kryo = kryoList.poll();
        if (kryo == null) {
            // Will defer additional allocation until deleteKryo().
            // It is more likely that it is no longer latency sensitive
            // by the time caller is recycling Kryo instance.
            return newKryoObject();
        } else {
            return kryo;
        }
    }

    /**
     * Deletes an existing Kryo object.
     *
     * @param deleteKryo the object to delete.
     */
    public void deleteKryo(final Kryo deleteKryo) {
        if (kryoList.isEmpty()) {
            // buffer extra if kryo instance pool has exhausted.
            List<Kryo> kryos = new ArrayList<>(DEFAULT_PREALLOCATIONS);
            for (int i = 0; i < DEFAULT_PREALLOCATIONS; ++i) {
                kryos.add(newKryoObject());
            }
            kryoList.addAll(kryos);
        }

        // recycle
        kryoList.add(deleteKryo);
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
        kryo.register(TopologyEvent.Type.class);

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
        kryo.register(IntentId.class);
        kryo.register(IntentEvent.class);
        kryo.register(IntentState.class);
        kryo.register(IntentCompilationResult.class);
        kryo.register(AbstractIntent.class);
        kryo.register(ConnectivityIntent.class);
        kryo.register(PointToPointIntent.class);
        kryo.register(MultiPointToSinglePointIntent.class);
        kryo.register(SinglePointToMultiPointIntent.class);
        kryo.register(net.onrc.onos.api.newintent.PathIntent.class);
        kryo.register(PathFlowIntent.class);
        kryo.register(SingleSrcTreeFlowIntent.class);
        kryo.register(SingleDstTreeFlowIntent.class);
        kryo.register(PacketConnectivityIntent.class);
        kryo.register(OpticalConnectivityIntent.class);
        // FIXME: due to lack of functionality to register a serializer
        // in API user side, we added the following two classes.
        // Theoretically the classes are only for test. we should create
        // a way to register serializer without editing source code
        kryo.register(TestIntent.class);
        kryo.register(TestSubclassIntent.class);

        // New flow manager related classes
        kryo.register(FlowId.class);
        kryo.register(FlowState.class);
        kryo.register(FlowBatchOperation.class);
        kryo.register(FlowBatchOperation.Operator.class);
        kryo.register(FlowBatchState.class);
        kryo.register(net.onrc.onos.api.flowmanager.Path.class);
        kryo.register(Tree.class);
        kryo.register(FlowLink.class);
        kryo.register(OpticalPathFlow.class);
        kryo.register(PacketPathFlow.class);
        kryo.register(SingleDstTreeFlow.class);

        // New match action related classes
        kryo.register(PacketMatch.class);
        kryo.register(OutputAction.class);
        kryo.register(ModifyDstMacAction.class);
        kryo.register(ModifySrcMacAction.class);

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

    /**
     * Serializes given object to byte array using Kryo instance in global pool.
     * <p>
     * Note: Serialized bytes must be smaller than DEFAULT_BUFFER_SIZE (=1MB).
     *
     * @param obj Object to serialize (Class must be registered to KryoFactory)
     * @return serialized bytes
     */
    public static byte[] serialize(final Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Serializes given object to byte array using Kryo instance in global pool.
     *
     * @param obj Object to serialize (Class must be registered to KryoFactory)
     * @param bufferSize maximum size of serialized bytes
     * @return serialized bytes
     */
    public static byte[] serialize(final Object obj, final int bufferSize) {
        Output out = new Output(bufferSize);
        Kryo kryo = GLOBAL_POOL.newKryo();
        try {
            kryo.writeClassAndObject(out, obj);
            return out.toBytes();
        } finally {
            GLOBAL_POOL.deleteKryo(kryo);
        }
    }

    /**
     * Deserializes given byte array to Object using Kryo instance in global pool.
     *
     * @param bytes serialized bytes
     * @param <T> deserialized Object type
     * @return deserialized Object (Class must be registered to KryoFactory)
     */
    public static <T> T deserialize(final byte[] bytes) {
        Input in = new Input(bytes);
        Kryo kryo = GLOBAL_POOL.newKryo();
        try {
            @SuppressWarnings("unchecked")
            T obj = (T) kryo.readClassAndObject(in);
            return obj;
        } finally {
            GLOBAL_POOL.deleteKryo(kryo);
        }
    }
}
