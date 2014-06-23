package net.onrc.onos.apps.sdnip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrefixTest {

    private List<PrefixTestData> testPrefixes;

    private class PrefixTestData {
        public final String dotNotation;
        public final String binaryNotation;
        public final int prefixLength;

        public PrefixTestData(String dotNotation, int prefixLength,
                String binaryNotation) {
            this.dotNotation = dotNotation;
            this.prefixLength = prefixLength;
            this.binaryNotation = binaryNotation;
        }
    }

    @Before
    public void setUp() throws Exception {

        testPrefixes = new LinkedList<>();

        testPrefixes.add(new PrefixTestData("0.0.0.0", 0, ""));

        testPrefixes.add(new PrefixTestData("192.168.166.0", 22,
                "1100000010101000101001"));

        testPrefixes.add(new PrefixTestData("192.168.166.0", 23,
                "11000000101010001010011"));

        testPrefixes.add(new PrefixTestData("192.168.166.0", 24,
                "110000001010100010100110"));

        testPrefixes.add(new PrefixTestData("130.162.10.1", 25,
                "1000001010100010000010100"));

        testPrefixes.add(new PrefixTestData("255.255.255.255", 32,
                "11111111111111111111111111111111"));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testNewPrefixFromByteArray() {
        byte[] b1 = new byte[]{(byte) 0x8f, (byte) 0xa0, (byte) 0x00, (byte) 0x00};
        byte[] b2 = new byte[]{(byte) 0x8f, (byte) 0xa0, (byte) 0xff, (byte) 0xff};
        byte[] b3 = new byte[]{(byte) 0x8f, (byte) 0xac, (byte) 0x00, (byte) 0x00};
        byte[] b4 = new byte[]{(byte) 0x8f, (byte) 0xa0, (byte) 0x00, (byte) 0x00};

        Prefix p1 = new Prefix(b1, 12);
        Prefix p2 = new Prefix(b2, 12);
        Prefix p3 = new Prefix(b3, 12);
        Prefix p4 = new Prefix(b4, 11);

        //Have different input byte arrays, but should be equal after construction
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p3));

        //Same input byte array, but should be false
        assertFalse(p1.equals(p4));

        assertTrue(Arrays.equals(p1.getAddress(), p3.getAddress()));
        assertTrue(p1.toString().equals(p2.toString()));
        assertTrue(Arrays.equals(p1.getAddress(), p4.getAddress()));
        assertFalse(p1.toString().equals(p4.toString()));
    }

    @Test
    public void testPrefixString() {
        Prefix p1 = new Prefix("192.168.166.0", 24);
        Prefix p2 = new Prefix("192.168.166.0", 23);
        Prefix p3 = new Prefix("192.168.166.128", 24);
        Prefix p4 = new Prefix("192.168.166.128", 25);

        assertFalse(p1.equals(p2));
        assertTrue(Arrays.equals(p1.getAddress(), p2.getAddress()));

        assertTrue(p1.equals(p3));
        assertTrue(Arrays.equals(p1.getAddress(), p2.getAddress()));

        assertFalse(p3.equals(p4));
        assertFalse(Arrays.equals(p3.getAddress(), p4.getAddress()));

        assertTrue(p1.toString().equals(p3.toString()));
        assertEquals(p1.hashCode(), p3.hashCode());
    }

    @Test
    public void testPrefixReturnsSame() {
        //Create a prefix of all ones for each prefix length.
        //Check that Prefix doesn't mangle it
        for (int prefixLength = 1; prefixLength <= Prefix.MAX_PREFIX_LENGTH; prefixLength++) {
            byte[] address = new byte[Prefix.ADDRESS_LENGTH_BYTES];

            int lastByte = (prefixLength - 1) / Byte.SIZE;
            int lastBit = (prefixLength - 1) % Byte.SIZE;

            for (int j = 0; j < address.length; j++) {
                if (j < lastByte) {
                    address[j] = (byte) 0xff;
                } else if (j == lastByte) {
                    byte b = 0;
                    byte msb = (byte) 0x80;
                    for (int k = 0; k < Byte.SIZE; k++) {
                        if (k <= lastBit) {
                            b |= (msb >> k);
                        }
                    }
                    address[j] = b;
                } else {
                    address[j] = 0;
                }
            }

            Prefix p = new Prefix(address, prefixLength);

            assertTrue(Arrays.equals(address, p.getAddress()));
        }
    }

    @Test
    public void testToBinaryString() {
        for (PrefixTestData testPrefix : testPrefixes) {
            Prefix p = new Prefix(testPrefix.dotNotation, testPrefix.prefixLength);
            assertEquals(testPrefix.binaryNotation, p.toBinaryString());
            assertEquals(p.getPrefixLength(), p.toBinaryString().length());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStringConstructorPrefixLengthTooSmall() {
        String s = "255.255.255.255";

        // Check the first valid prefix length works
        Prefix p = new Prefix(s, Prefix.MIN_PREFIX_LENGTH);
        assertNotNull(p);

        // Should throw IllegalArgumentException
        new Prefix(s, Prefix.MIN_PREFIX_LENGTH - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStringConstructorPrefixLengthTooLarge() {
        String s = "255.255.255.255";

        // Check the last valid prefix length works
        Prefix p = new Prefix(s, Prefix.MAX_PREFIX_LENGTH);
        assertNotNull(p);

        // Should throw IllegalArgumentException
        new Prefix(s, Prefix.MAX_PREFIX_LENGTH + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteConstructorPrefixLengthTooSmall() {
        byte[] b = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

        // Check the first valid prefix length works
        Prefix p = new Prefix(b, Prefix.MIN_PREFIX_LENGTH);
        assertNotNull(p);

        // Should throw IllegalArgumentException
        new Prefix(b, Prefix.MIN_PREFIX_LENGTH - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteConstructorPrefixLengthTooLarge() {
        byte[] b = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

        // Check the last valid prefix length works
        Prefix p = new Prefix(b, Prefix.MAX_PREFIX_LENGTH);
        assertNotNull(p);

        // Should throw IllegalArgumentException
        new Prefix(b, Prefix.MAX_PREFIX_LENGTH + 1);
    }
}
