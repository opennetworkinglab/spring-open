/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.onrc.onos.core.linkdiscovery.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.test.MockThreadPoolService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.restserver.RestApiServer;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryListener;
import net.onrc.onos.core.linkdiscovery.ILinkDiscoveryService;
import net.onrc.onos.core.linkdiscovery.Link;
import net.onrc.onos.core.linkdiscovery.LinkInfo;
import net.onrc.onos.core.linkdiscovery.NodePortTuple;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// CHECKSTYLE IGNORE WriteTag FOR NEXT 2 LINES
/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class LinkDiscoveryManagerTest extends FloodlightTestCase {

    private TestLinkDiscoveryManager ldm;
    protected static final Logger log = LoggerFactory.getLogger(LinkDiscoveryManagerTest.class);

    public class TestLinkDiscoveryManager extends LinkDiscoveryManager {
        public boolean isSendLLDPsCalled = false;
        public boolean isClearLinksCalled = false;

        @Override
        protected void discoverOnAllPorts() {
            isSendLLDPsCalled = true;
            super.discoverOnAllPorts();
        }

        public void reset() {
            isSendLLDPsCalled = false;
            isClearLinksCalled = false;
        }
    }

    public LinkDiscoveryManager getTopology() {
        return ldm;
    }

    public IOFSwitch createMockSwitch(Long id) {
        IOFSwitch mockSwitch = createNiceMock(IOFSwitch.class);
        expect(mockSwitch.getId()).andReturn(id).anyTimes();
        return mockSwitch;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        FloodlightModuleContext cntx = new FloodlightModuleContext();
        ldm = new TestLinkDiscoveryManager();
        ldm.linkDiscoveryAware = new ArrayList<ILinkDiscoveryListener>();
        MockThreadPoolService tp = new MockThreadPoolService();
        RestApiServer restApi = new RestApiServer();
        cntx.addService(IRestApiService.class, restApi);
        cntx.addService(IThreadPoolService.class, tp);
        cntx.addService(ILinkDiscoveryService.class, ldm);
        cntx.addService(IFloodlightProviderService.class, getMockFloodlightProvider());
        restApi.init(cntx);
        tp.init(cntx);
        ldm.init(cntx);
        restApi.startUp(cntx);
        tp.startUp(cntx);
        ldm.startUp(cntx);

        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        Map<Long, IOFSwitch> switches = new HashMap<Long, IOFSwitch>();
        switches.put(1L, sw1);
        switches.put(2L, sw2);
        getMockFloodlightProvider().setSwitches(switches);
        replay(sw1, sw2);
    }

    @Test
    public void testAddOrUpdateLink() throws Exception {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 2, 2L, 1);
        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);


        NodePortTuple srcNpt = new NodePortTuple(1L, 2);
        NodePortTuple dstNpt = new NodePortTuple(2L, 1);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc()));
        assertTrue(topology.switchLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(srcNpt));
        assertTrue(topology.portLinks.get(srcNpt).contains(lt));
        assertNotNull(topology.portLinks.get(dstNpt));
        assertTrue(topology.portLinks.get(dstNpt).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLink() throws Exception {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 2, 2L, 1);
        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);
        topology.deleteLinks(Collections.singletonList(lt), "Test");

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc()));
        assertNull(topology.switchLinks.get(lt.getDst()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testAddOrUpdateLinkToSelf() throws Exception {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 2, 2L, 3);
        NodePortTuple srcNpt = new NodePortTuple(1L, 2);
        NodePortTuple dstNpt = new NodePortTuple(2L, 3);

        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc()));
        assertTrue(topology.switchLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(srcNpt));
        assertTrue(topology.portLinks.get(srcNpt).contains(lt));
        assertNotNull(topology.portLinks.get(dstNpt));
        assertTrue(topology.portLinks.get(dstNpt).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLinkToSelf() throws Exception {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 2, 1L, 3);
        NodePortTuple srcNpt = new NodePortTuple(1L, 2);
        NodePortTuple dstNpt = new NodePortTuple(2L, 3);

        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);
        topology.deleteLinks(Collections.singletonList(lt), "Test to self");

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc()));
        assertNull(topology.switchLinks.get(lt.getDst()));
        assertNull(topology.portLinks.get(srcNpt));
        assertNull(topology.portLinks.get(dstNpt));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitch() {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 2, 2L, 1);
        NodePortTuple srcNpt = new NodePortTuple(1L, 2);
        NodePortTuple dstNpt = new NodePortTuple(2L, 1);
        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);

        IOFSwitch sw1 = getMockFloodlightProvider().getSwitches().get(1L);
        IOFSwitch sw2 = getMockFloodlightProvider().getSwitches().get(2L);
        // Mock up our expected behavior
        topology.removedSwitch(sw1);
        verify(sw1, sw2);

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc()));
        assertNull(topology.switchLinks.get(lt.getDst()));
        assertNull(topology.portLinks.get(srcNpt));
        assertNull(topology.portLinks.get(dstNpt));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitchSelf() {
        LinkDiscoveryManager topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        replay(sw1);
        Link lt = new Link(1L, 2, 1L, 3);
        LinkInfo info = new LinkInfo(System.currentTimeMillis(),
                System.currentTimeMillis(), 0, 0);
        topology.addOrUpdateLink(lt, info);

        // Mock up our expected behavior
        topology.removedSwitch(sw1);

        verify(sw1);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testAddUpdateLinks() throws Exception {
        LinkDiscoveryManager topology = getTopology();

        Link lt = new Link(1L, 1, 2L, 1);
        NodePortTuple srcNpt = new NodePortTuple(1L, 1);
        NodePortTuple dstNpt = new NodePortTuple(2L, 1);

        LinkInfo info;

        info = new LinkInfo(System.currentTimeMillis() - 40000,
                System.currentTimeMillis() - 40000, 0, 0);
        topology.addOrUpdateLink(lt, info);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc()));
        assertTrue(topology.switchLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(srcNpt));
        assertTrue(topology.portLinks.get(srcNpt).contains(lt));
        assertNotNull(topology.portLinks.get(dstNpt));
        assertTrue(topology.portLinks.get(dstNpt).contains(lt));
        assertTrue(topology.links.containsKey(lt));

        topology.timeoutLinks();


        info = new LinkInfo(System.currentTimeMillis(), /* firstseen */
                null, /* unicast */0, 0);
        topology.addOrUpdateLink(lt, info);
        assertTrue(topology.links.get(lt).getUnicastValidTime() == null);


        // Add a link info based on info that would be obtained from unicast LLDP
        // Setting the unicast LLDP reception time to be 40 seconds old, so we can use
        // this to test timeout after this test.  Although the info is initialized
        // with LT_OPENFLOW_LINK, the link property should be changed to LT_NON_OPENFLOW
        // by the addOrUpdateLink method.
        info = new LinkInfo(System.currentTimeMillis() - 40000,
                System.currentTimeMillis() - 40000, 0, 0);
        topology.addOrUpdateLink(lt, info);

        // Expect to timeout the unicast Valid Time, but not the multicast Valid time
        // So the link type should go back to non-openflow link.
        topology.timeoutLinks();
        assertTrue(topology.links.get(lt) == null);
    }

    /**
     * This test case verifies that LinkDiscoveryManager.sendDiscoveryMessage()
     * performs "write" operation on the specified IOFSwitch object
     * with a LLDP packet.
     *
     * @throws IOException
     */
    @Test
    public void testSendDiscoveryMessage() throws IOException {
        byte[] macAddress = new byte[] {0x0, 0x0, 0x0, 0x0, 0x0, 0x1};

        LinkDiscoveryManager topology = getTopology();

        // Mock up our expected behavior
        IOFSwitch swTest = createMockSwitch(3L);
        getMockFloodlightProvider().getSwitches().put(3L, swTest);

        short portNum = 1;
        OFPhysicalPort ofpPort = new OFPhysicalPort();
        ofpPort.setPortNumber(portNum);
        ofpPort.setHardwareAddress(macAddress);

        /* sendDiscoverMessage() should perform the following actions on
         * IOFSwitch object
         * - getPort() with argument as "1"
         * - write() with OFPacketOut
         * - flush()
         */
        expect(swTest.getPort(portNum)).andReturn(ofpPort).atLeastOnce();
        swTest.write(EasyMock.anyObject(OFMessage.class), EasyMock.anyObject(FloodlightContext.class));
        EasyMock.expectLastCall().times(1);
        swTest.flush();
        EasyMock.expectLastCall().once();
        replay(swTest);

        topology.sendDiscoveryMessage(3L, portNum, false);

        verify(swTest);
    }
}
