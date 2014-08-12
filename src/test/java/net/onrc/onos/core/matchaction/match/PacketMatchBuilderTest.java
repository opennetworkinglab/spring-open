package net.onrc.onos.core.matchaction.match;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.util.IPv4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PacketMatchBuilderTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConstructor() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testSrcMacAddress() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setSrcMac(MACAddress.valueOf("00:01:02:03:04:05"));
        PacketMatch match = builder.build();

        assertNotNull(match.getSrcMacAddress());
        assertEquals(MACAddress.valueOf("00:01:02:03:04:05"), match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testDstMacAddress() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstMac(MACAddress.valueOf("00:01:02:03:04:05"));
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNotNull(match.getDstMacAddress());
        assertEquals(MACAddress.valueOf("00:01:02:03:04:05"), match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testEtherType() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setEtherType((short) 0x0800);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNotNull(match.getEtherType());
        assertEquals(Short.valueOf((short) 0x0800), match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testSrcIpAddress() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setSrcIp(new IPv4("10.0.0.1"));
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNotNull(match.getSrcIpAddress());
        assertEquals("10.0.0.1/32", match.getSrcIpAddress().toString());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testSrcIpAddressWithPrefix() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setSrcIp(new IPv4("10.0.0.0"), (short) 8);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNotNull(match.getSrcIpAddress());
        assertEquals("10.0.0.0/8", match.getSrcIpAddress().toString());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testDstIpAddress() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstIp(new IPv4("192.168.0.0"), (short) 24);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNotNull(match.getDstIpAddress());
        assertEquals("192.168.0.0/24", match.getDstIpAddress().toString());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testIpProtocolNumber() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setIpProto((byte) 7);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNotNull(match.getIpProtocolNumber());
        assertEquals(Byte.valueOf((byte) 7), match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testSrcTcpPortNumber() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setSrcTcpPort((short) 80);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNotNull(match.getSrcTcpPortNumber());
        assertEquals(Short.valueOf((short) 80), match.getSrcTcpPortNumber());
        assertNull(match.getDstTcpPortNumber());
    }

    @Test
    public void testDstTcpPortNumber() {
        PacketMatchBuilder builder = new PacketMatchBuilder();
        builder.setDstTcpPort((short) 8080);
        PacketMatch match = builder.build();

        assertNull(match.getSrcMacAddress());
        assertNull(match.getDstMacAddress());
        assertNull(match.getEtherType());
        assertNull(match.getSrcIpAddress());
        assertNull(match.getDstIpAddress());
        assertNull(match.getIpProtocolNumber());
        assertNull(match.getSrcTcpPortNumber());
        assertNotNull(match.getDstTcpPortNumber());
        assertEquals(Short.valueOf((short) 8080), match.getDstTcpPortNumber());
    }
}
