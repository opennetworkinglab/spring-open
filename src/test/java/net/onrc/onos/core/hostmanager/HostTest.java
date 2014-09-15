package net.onrc.onos.core.hostmanager;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This is the test for the Host class.
 */
public class HostTest {

    MACAddress mac1;
    MACAddress mac2;
    Long dpid1;
    Long dpid2;
    Long portNum1;
    Long portNum2;
    Date date1;
    Date date2;

    @Before
    public void setUp() throws Exception {
        mac1 = MACAddress.valueOf("00:00:00:00:00:01");
        mac2 = MACAddress.valueOf("00:00:00:00:00:01");
        dpid1 = 1L;
        dpid2 = 1L;
        portNum1 = 1L;
        portNum2 = 1L;
        date1 = new Date(1L);
        date2 = new Date(2L);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test for making sure hashCode function works properly.
     */
    @Test
    public void testHashCode() {
        Host host1 = new Host(mac1, 0, null, dpid1, portNum1, date1);
        Host host2 = new Host(mac2, 0, null, dpid2, portNum2, date2);

        assertTrue(host1.hashCode() == host2.hashCode());
    }

    /**
     * Test for making sure equals function works properly.
     */
    @Test
    public void testEqualsObject() {
        Host host1 = new Host(mac1, 0, null, dpid1, portNum1, date1);
        Host host2 = new Host(mac2, 0, null, dpid2, portNum2, date2);

        assertTrue(host1.equals(host2));
    }

}
