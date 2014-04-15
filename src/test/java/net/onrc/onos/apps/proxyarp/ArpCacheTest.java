package net.onrc.onos.apps.proxyarp;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArpCacheTest {
    ArpCache arpCache;
    InetAddress ip, ip2;
    MACAddress mac, mac2;
    Map<InetAddress, MACAddress> map;

    @Before
    public void setUp() throws Exception {
        arpCache = new ArpCache();
        arpCache.setArpEntryTimeoutConfig(1000);
        mac = MACAddress.valueOf("00:01:02:03:04:05");
        ip = InetAddress.getByAddress(new byte[]{10, 0, 0, 1});
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
        arpCache.update(ip, mac);
        map.put(ip, mac);
        arpCache.update(ip2, mac2);
        map.put(ip2, mac2);
        assertEquals(mac, arpCache.lookup(ip));
    }

    @Test
    public void testRemove() {
        testUpdate();
        arpCache.remove(ip);
        assertNull(arpCache.lookup(ip));
    }

    @Test
    public void testGetMappings() {
        testUpdate();
        for(String macStr :arpCache.getMappings()) {
            assertNotNull(macStr);
        }
    }

    @Test
    public void testGetExpiredArpCacheIps() {
        testUpdate();
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertNotNull(arpCache.getExpiredArpCacheIps());
        assertEquals(map.size(), arpCache.getExpiredArpCacheIps().size());
        for(InetAddress ip : arpCache.getExpiredArpCacheIps()) {
           assertTrue(map.containsKey(ip));
        }
    }

}
