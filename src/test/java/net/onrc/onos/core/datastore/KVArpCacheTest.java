package net.onrc.onos.core.datastore;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datastore.IKVTable.IKVEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KVArpCacheTest {

    KVArpCache arpCache = null;
    InetAddress ip = null;
    MACAddress mac = null;

    @Before
    public void setUp() throws Exception {
        arpCache = new KVArpCache();
        try {
            mac = MACAddress.valueOf("00:01:02:03:04:05");
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            fail();
        }
    }

    @After
    public void tearDown() throws Exception {
        arpCache.dropArpCache();
        arpCache = null;
    }

    @Test
    public void testKVArpCache() {
        assertNotNull(arpCache);
    }

    @Test
    public void testCreate() {
        byte[] byteMac = mac.toBytes();
        try {
            long verison = arpCache.create(ip, byteMac);
            assertNotEquals(arpCache.getVersionNonexistant(), verison);
        } catch (ObjectExistsException e) {
            fail();
        }
    }

    @Test
    public void testForceCreate() {
        byte[] byteMac = mac.toBytes();
        long version = arpCache.forceCreate(ip, byteMac);
        assertNotEquals(arpCache.getVersionNonexistant(), version);
    }

    @Test
    public void testRead() {
        byte[] byteMac = mac.toBytes();
        try {
            arpCache.create(ip, byteMac);
            byte[] entry = arpCache.read(ip).getValue();
            assertEquals(MACAddress.valueOf(byteMac), MACAddress.valueOf(entry));
        } catch (ObjectDoesntExistException | ObjectExistsException e) {
            fail();
        }
    }

    @Test
    public void testUpdateInetAddressByteArray() {
        byte[] byteMac = mac.toBytes();
        byte[] byteMac2 = MACAddress.valueOf("00:01:02:03:04:06").toBytes();
        try {
            arpCache.create(ip, byteMac);
            arpCache.update(ip, byteMac2);
            byte[] entry = arpCache.read(ip).getValue();
            assertEquals(MACAddress.valueOf(byteMac2), MACAddress.valueOf(entry));
        } catch (ObjectDoesntExistException | ObjectExistsException e) {
            fail();
        }
    }

    @Test
    public void testForceDelete() {
        byte[] byteMac = mac.toBytes();
        long ver = arpCache.forceCreate(ip, byteMac);
        long deletedVer = arpCache.forceDelete(ip);
        assertEquals(ver, deletedVer);
    }

    @Test
    public void testGetAllEntries() {
        byte[] ipAddr = new byte[]{10, 0, 0, 1};
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            fail();
        }

        byte[] byteMac = mac.toBytes();
        byte[] byteMac2 = MACAddress.valueOf("00:01:02:03:04:06").toBytes();
        Map<InetAddress, byte[]> map = new HashMap<InetAddress, byte[]>();
        try {
            arpCache.create(ip, byteMac);
            map.put(ip, byteMac);
            arpCache.create(ip2, byteMac2);
            map.put(ip2, byteMac2);
            for (IKVEntry entry : arpCache.getAllEntries()) {
                try {
                    assertTrue(map.containsKey(InetAddress.getByAddress(entry.getKey())));
                    MACAddress mac1 = MACAddress.valueOf(map.get(InetAddress.getByAddress(entry.getKey())));
                    MACAddress mac2 = MACAddress.valueOf(entry.getValue());
                    assertEquals(mac1, mac2);
                } catch (UnknownHostException e) {
                    fail();
                }
            }
        } catch (ObjectExistsException e) {
            fail();
        }
    }

}
