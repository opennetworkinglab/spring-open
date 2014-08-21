package net.onrc.onos.apps.proxyarp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArpCacheTest {

    private static final int SHORTENED_TIMEOUT_MS = 500;

    ArpCache arpCache;
    InetAddress ip1, ip2;
    MACAddress mac, mac2;
    Map<InetAddress, MACAddress> map;

    @Before
    public void setUp() throws Exception {
        arpCache = new ArpCache();
        arpCache.setArpEntryTimeoutConfig(SHORTENED_TIMEOUT_MS);
        mac = MACAddress.valueOf("00:01:02:03:04:05");
        ip1 = InetAddress.getByAddress(new byte[]{10, 0, 0, 1});
        mac2 = MACAddress.valueOf("00:01:02:03:04:06");
        ip2 = InetAddress.getByAddress(new byte[]{10, 0, 0, 2});
    }

    @After
    public void tearDown() throws Exception {
        arpCache = null;
    }

    @Test
    public void testArpCache() {
        assertNotNull(new ArpCache());
    }

    @Test
    public void testLookup() {
        testUpdate();
        assertEquals(mac2, arpCache.lookup(ip2));
    }

    @Test
    public void testUpdate() {
        map = new HashMap<InetAddress, MACAddress>();
        arpCache.update(ip1, mac);
        map.put(ip1, mac);
        arpCache.update(ip2, mac2);
        map.put(ip2, mac2);
        assertEquals(mac, arpCache.lookup(ip1));
    }

    @Test
    public void testRemove() {
        testUpdate();
        arpCache.remove(ip1);
        assertNull(arpCache.lookup(ip1));
    }

    @Test
    public void testGetMappings() {
        testUpdate();
        for (String macStr :arpCache.getMappings()) {
            assertNotNull(macStr);
        }
    }

    @Test
    public void testGetExpiredArpCacheIps() {
        testUpdate();

        try {
            Thread.sleep(2 * SHORTENED_TIMEOUT_MS);
        } catch (InterruptedException e) {
            fail();
        }

        assertNotNull(arpCache.getExpiredArpCacheIps());
        assertEquals(map.size(), arpCache.getExpiredArpCacheIps().size());
        for (InetAddress ip : arpCache.getExpiredArpCacheIps()) {
           assertTrue(map.containsKey(ip));
        }
    }

    @Test
    public void testSetArpEntryTimeoutConfig() {
        long arpEntryTimeout = 10000;
        arpCache.setArpEntryTimeoutConfig(arpEntryTimeout);
        assertEquals(arpEntryTimeout, arpCache.getArpEntryTimeout());
    }
}
