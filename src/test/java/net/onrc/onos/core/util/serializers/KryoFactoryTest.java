package net.onrc.onos.core.util.serializers;

import static org.junit.Assert.*;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.topology.HostData;
import net.onrc.onos.core.topology.LinkData;
import net.onrc.onos.core.topology.PortData;
import net.onrc.onos.core.topology.TopologyBatchOperation;
import net.onrc.onos.core.topology.TopologyElement;
import net.onrc.onos.core.topology.TopologyEvent;
import net.onrc.onos.core.topology.SwitchData;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.OnosInstanceId;
import net.onrc.onos.core.util.PortNumber;
import net.onrc.onos.core.util.SwitchPort;

import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Sets;

/**
 * Tests to capture Kryo serialization characteristics.
 * <p/>
 * <ul>
 *  <li>Serialization/deserialization throughput</li>
 *  <li>Serialized size</li>
 *  <li>Equality of object before and after serializaton,deserialization</li>
 *  <li>TODO bit by bit comparison of serialized bytes</li>
 * </ul>
 */
public class KryoFactoryTest {

    private static final int NUM_ITERATIONS = Integer.parseInt(
                                    System.getProperty("iterations", "100"));

    private static final Dpid DPID_A = new Dpid(0x1234L);
    private static final Dpid DPID_B = new Dpid(Long.MAX_VALUE);
    private static final PortNumber PORT_NO_A = PortNumber.uint16((short) 42);
    private static final PortNumber PORT_NO_B = PortNumber.uint16((short) 65534);
    private static final String ONOS_INSTANCE_NAME = "ONOS-Instance-Test";

    private static final double SEC_IN_NANO = 1000 * 1000 * 1000.0;

    private KryoFactory kryoFactory;

    @Before
    public void setUp() throws Exception {
        kryoFactory = new KryoFactory(1);
    }

    /**
     * Test case to check recycling behavior of KryoFactory.
     */
    @Test
    public void testReallocation() {
        final int poolSize = 3;
        KryoFactory pool = new KryoFactory(poolSize);


        // getting Kryo instance smaller than pool size should work just fine
        Set<Kryo> kryos = new HashSet<>();
        for (int i = 0; i < poolSize - 1; ++i) {
            Kryo kryo = pool.newKryo();
            assertNotNull(kryo);
            assertTrue("KryoFactory should return unique instances",
                        kryos.add(kryo));
        }

        // recycle Kryo instance
        for (Kryo kryo : kryos) {
            pool.deleteKryo(kryo);
        }


        // recycling behavior check
        Set<Kryo> kryos2 = new HashSet<>();
        for (int i = 0; i < poolSize - 1; ++i) {
            Kryo kryo = pool.newKryo();
            assertNotNull(kryo);
            assertTrue("KryoFactory should return unique instances",
                        kryos2.add(kryo));
        }
        // should at least have some recycled instances
        assertTrue("Kryo instances should be reused after deleting",
                    !Sets.difference(kryos2, kryos).isEmpty());

        for (Kryo kryo : kryos2) {
            pool.deleteKryo(kryo);
        }


        // pool expansion behavior check
        Set<Kryo> kryos3 = new HashSet<>();
        // it should be able to get Kryo instances larger than pool size set
        for (int i = 0; i < poolSize * 2; ++i) {
            Kryo kryo = pool.newKryo();
            assertNotNull(kryo);
            assertTrue("KryoFactory should return unique instances",
                        kryos3.add(kryo));
        }
        // recycle Kryo instance (should trigger pool expansion)
        for (Kryo kryo : kryos3) {
            pool.deleteKryo(kryo);
        }

        // should at least have some Kryo instances we haven't seen initially.
        assertTrue("New Kryo instances should be added to the pool",
                !Sets.difference(kryos3, kryos).isEmpty());
    }

    /**
     * Tests static serialize/deserialize methods.
     */
    @Test
    public void testStaticSerializeDeserialize() {

        final List<Object> objects = new ArrayList<>();
        Dpid dpid1 = new Dpid(1);
        PortNumber port10 = PortNumber.uint32(10);
        SwitchPort switchPort = new SwitchPort(dpid1, port10);

        objects.add(dpid1);
        objects.add(port10);
        objects.add(switchPort);

        final byte[] bytes = KryoFactory.serialize(objects);
        final List<Object> deserialized = KryoFactory.deserialize(bytes);

        assertEquals(objects, deserialized);
    }

    /**
     * Tests deserializing to wrong type result in ClassCastException.
     */
    @Test
    public void testStaticSerializeDeserializeCastFailure() {
        final Integer integer = 42;
        final byte[] integerBytes = KryoFactory.serialize(integer);

        final Number validSuperType = KryoFactory.deserialize(integerBytes);
        assertEquals(integer, validSuperType);

        try {
            final String wrongType = KryoFactory.deserialize(integerBytes);
            fail("Should have thrown exception" + wrongType);
        } catch (ClassCastException e) { // CHECKSTYLE IGNORE THIS LINE
        }
    }

    /**
     * Benchmark result.
     */
    private static final class Result {
        /**
         * Serialized type name.
         */
        String type;
        /**
         * Serialized size.
         */
        int size;
        /**
         * Serialization throughput (ops/sec).
         */
        double ser;
        /**
         * Deserialization throughput (ops/sec).
         */
        double deser;

        public Result(String type, int size, double ser, double deser) {
            this.type = type;
            this.size = size;
            this.ser = ser;
            this.deser = deser;
        }
    }

    private static enum EqualityCheck {
        /**
         * No way to compare equality provided.
         */
        IMPOSSIBLE,
        /**
         * custom equals method is defined.
         */
        EQUALS,
        /**
         * Can be compared using toString() result.
         */
        TO_STRING,
    }

    /**
     * Benchmark serialization of specified object.
     *
     * @param obj the object to benchmark
     * @param equalityCheck how to check equality of deserialized object.
     * @return benchmark {@link Result}
     */
    private Result benchType(Object obj, EqualityCheck equalityCheck) {

        Kryo kryo = kryoFactory.newKryo();
        try {
            byte[] buffer = new byte[1 * 1000 * 1000];
            Output output = new Output(buffer);

            // Measurement: serialization size
            kryo.writeClassAndObject(output, obj);
            int serializedBytes = output.toBytes().length;

            // Measurement: serialization throughput
            byte[] result = null;

            long t1 = System.nanoTime();
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                output.clear();
                kryo.writeClassAndObject(output, obj);
                result = output.toBytes();
            }
            long t2 = System.nanoTime();
            double serTput = NUM_ITERATIONS * SEC_IN_NANO / (t2 - t1);

            // Measurement: deserialization throughput
            Object objOut = null;
            Input input = new Input(result);
            long t3 = System.nanoTime();
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                input.setBuffer(result);
                objOut = kryo.readClassAndObject(input);
            }
            long t4 = System.nanoTime();

            switch (equalityCheck) {
            case IMPOSSIBLE:
                break;
            case EQUALS:
                assertEquals(obj, objOut);
                break;
            case TO_STRING:
                assertEquals(obj.toString(), objOut.toString());
                break;
            default:
                break;
            }
            double deserTput = NUM_ITERATIONS * SEC_IN_NANO / (t4 - t3);

            return new Result(obj.getClass().getSimpleName(),
                    serializedBytes, serTput, deserTput);
        } finally {
            kryoFactory.deleteKryo(kryo);
        }
    }

    /**
     * Benchmark serialization of types registered to KryoFactory.
     */
    @Test
    public void benchmark() throws Exception {

        List<Result> results = new ArrayList<>();

        // To be more strict, we should be checking serialized byte[].
        { // CHECKSTYLE IGNORE THIS LINE
            HostData obj = new HostData(MACAddress.valueOf(0x12345678));
            obj.createStringAttribute(TopologyElement.TYPE,
                                      TopologyElement.TYPE_PACKET_LAYER);
            obj.addAttachmentPoint(new SwitchPort(DPID_A, PORT_NO_A));
            // avoid using System.currentTimeMillis() var-int size may change
            obj.setLastSeenTime(392860800000L);
            obj.freeze();
            Result result = benchType(obj, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(43, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            LinkData obj = new LinkData(new SwitchPort(DPID_A, PORT_NO_A),
                                          new SwitchPort(DPID_B, PORT_NO_B));
            obj.createStringAttribute(TopologyElement.TYPE,
                                      TopologyElement.TYPE_PACKET_LAYER);
            obj.freeze();
            Result result = benchType(obj, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(49, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            PortData obj = new PortData(DPID_A, PORT_NO_A);
            obj.createStringAttribute(TopologyElement.TYPE,
                                      TopologyElement.TYPE_PACKET_LAYER);
            obj.freeze();
            Result result = benchType(obj, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(24, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            SwitchData obj = new SwitchData(DPID_A);
            obj.createStringAttribute(TopologyElement.TYPE,
                                      TopologyElement.TYPE_PACKET_LAYER);
            obj.freeze();
            Result result = benchType(obj, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(21, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            SwitchData evt = new SwitchData(DPID_A);
            evt.createStringAttribute(TopologyElement.TYPE,
                                      TopologyElement.TYPE_PACKET_LAYER);
            evt.freeze();
            OnosInstanceId onosInstanceId =
                new OnosInstanceId(ONOS_INSTANCE_NAME);

            // using the back door to access package-scoped constructor
            Constructor<TopologyEvent> swConst
                = TopologyEvent.class.getDeclaredConstructor(SwitchData.class,
                        OnosInstanceId.class);
            swConst.setAccessible(true);
            TopologyEvent obj = swConst.newInstance(evt, onosInstanceId);

            Result result = benchType(obj, EqualityCheck.TO_STRING);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(45, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            OnosInstanceId id = new OnosInstanceId(ONOS_INSTANCE_NAME);

            Result result = benchType(id, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(21, result.size);
        }

        { // CHECKSTYLE IGNORE THIS LINE
            TopologyBatchOperation tbo = new TopologyBatchOperation();
            OnosInstanceId onosInstanceId =
                new OnosInstanceId(ONOS_INSTANCE_NAME);

            // using the back door to access package-scoped constructor
            Constructor<TopologyEvent> swConst
                = TopologyEvent.class.getDeclaredConstructor(SwitchData.class,
                        OnosInstanceId.class);
            swConst.setAccessible(true);

            for (int i = 1; i <= 10; i++) {
                Dpid dpid = new Dpid(i);
                SwitchData switchData = new SwitchData(dpid);
                TopologyEvent topologyEvent =
                    swConst.newInstance(switchData, onosInstanceId);
                tbo.appendAddOperation(topologyEvent);
            }

            Result result = benchType(tbo, EqualityCheck.EQUALS);
            results.add(result);
            // update me if serialized form is expected to change
            assertEquals(186, result.size);
        }

        // TODO Add registered classes we still use.


        // Output for plot plugin
        List<String> slabels = new ArrayList<>();
        List<Number> svalues = new ArrayList<>();
        List<String> tlabels = new ArrayList<>();
        List<Number> tvalues = new ArrayList<>();

        // Type, size, serialize T-put, deserialize T-put, N
        System.out.println("Type, size, serialize T-put, deserialize T-put, N");

        for (Result result : results) {
            System.out.printf("%s, %d, %f, %f, %d\n",
                    result.type, result.size, result.ser, result.deser,
                    NUM_ITERATIONS);

            // Output for plot plugin
            // <Type>_size, <Type>_ser, <Type>_deser
            slabels.addAll(Arrays.asList(
                    result.type + "_size"
                    ));
            svalues.addAll(Arrays.asList(
                    result.size
                    ));
            tlabels.addAll(Arrays.asList(
                    result.type + "_ser",
                    result.type + "_deser"
                    ));
            tvalues.addAll(Arrays.asList(
                    result.ser,
                    result.deser
                    ));
        }

        // Output for plot plugin
        PrintStream size = new PrintStream("target/KryoFactoryTest_size.csv");
        PrintStream tput = new PrintStream("target/KryoFactoryTest_tput.csv");

        for (String label : slabels) {
                size.print(label);
                size.print(", ");
        }
        size.println();
        for (Number value : svalues) {
            size.print(value);
            size.print(", ");
        }
        size.close();

        for (String label : tlabels) {
            tput.print(label);
            tput.print(", ");
        }
        tput.println();
        for (Number value : tvalues) {
            tput.print(value);
            tput.print(", ");
        }
        tput.close();
    }
}
