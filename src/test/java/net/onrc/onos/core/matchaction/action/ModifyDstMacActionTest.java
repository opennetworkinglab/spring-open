package net.onrc.onos.core.matchaction.action;

import static org.junit.Assert.*;
import net.floodlightcontroller.util.MACAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModifyDstMacActionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConstructor() {
        ModifyDstMacAction action =
                new ModifyDstMacAction(MACAddress.valueOf("00:01:02:03:04:05"));
        assertEquals(MACAddress.valueOf("00:01:02:03:04:05"), action.getDstMac());
    }
}
