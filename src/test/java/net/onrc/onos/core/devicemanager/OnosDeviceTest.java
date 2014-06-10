package net.onrc.onos.core.devicemanager;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 * This is the test for OnosDevice.class.
 */
public class OnosDeviceTest {

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

    /*
     * Test for making sure hashCode function works properly.
     */
    @Test
    public void testHashCode() {
        OnosDevice dev1 = new OnosDevice(mac1, null, dpid1, portNum1, date1);
        OnosDevice dev2 = new OnosDevice(mac2, null, dpid2, portNum2, date2);

        assertTrue(dev1.hashCode() == dev2.hashCode());
    }

    /*
     * Test for making sure equals function works properly.
     */
    @Test
    public void testEqualsObject() {
        OnosDevice dev1 = new OnosDevice(mac1, null, dpid1, portNum1, date1);
        OnosDevice dev2 = new OnosDevice(mac2, null, dpid2, portNum2, date2);

        assertTrue(dev1.equals(dev2));
    }

}
