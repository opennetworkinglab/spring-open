/**
 *    Copyright 2013, Big Switch Networks, Inc.
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

package net.floodlightcontroller.core.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.IOFSwitch.PortChangeEvent;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.debugcounter.DebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;

import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRole;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

public class OFSwitchImplBaseTest {

    IFloodlightProviderService floodlightProvider;
    Map<Long, IOFSwitch> switches;

    private class OFSwitchTest extends OFSwitchImplBase {
        public OFSwitchTest(IFloodlightProviderService fp) {
            super();
            stringId = "whatever";
            datapathId = DatapathId.of(1L);
            floodlightProvider = fp;
        }

        @Override
        public void write(OFMessage msg, FloodlightContext cntx) {}


        @Override
        public String toString() {
            return "OFSwitchTest";
        }
    }

    private OFSwitchTest sw;

    /*
     * AAS: Setting the factory to default value of OF1.0 wire protocol.
     * TODO: revisit this when we do 1.2 unit testing.
     */
    private OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);

    private OFPortDesc p1a;
    private OFPortDesc p1b;
    private OFPortDesc p2a;
    private OFPortDesc p2b;
    private OFPortDesc p3;
    private final OFPortDesc portFoo1 = factory10.buildPortDesc()
                                                 .setName("foo")
                                                 .setPortNo(OFPort.of(11))
                                                 .build();
    private final OFPortDesc portFoo2 = factory10.buildPortDesc()
                                                 .setName("foo")
                                                 .setPortNo(OFPort.of(12))
                                                 .build();
    private final OFPortDesc portBar1 = factory10.buildPortDesc()
                                                 .setName("bar")
                                                 .setPortNo(OFPort.of(11))
                                                 .build();
    private final OFPortDesc portBar2 = factory10.buildPortDesc()
                                                 .setName("bar")
                                                 .setPortNo(OFPort.of(12))
                                                 .build();
    private final PortChangeEvent portFoo1Add =
            new PortChangeEvent(portFoo1, PortChangeType.ADD);
    private final PortChangeEvent portFoo2Add =
            new PortChangeEvent(portFoo2, PortChangeType.ADD);
    private final PortChangeEvent portBar1Add =
            new PortChangeEvent(portBar1, PortChangeType.ADD);
    private final PortChangeEvent portBar2Add =
            new PortChangeEvent(portBar2, PortChangeType.ADD);
    private final PortChangeEvent portFoo1Del =
            new PortChangeEvent(portFoo1, PortChangeType.DELETE);
    private final PortChangeEvent portFoo2Del =
            new PortChangeEvent(portFoo2, PortChangeType.DELETE);
    private final PortChangeEvent portBar1Del =
            new PortChangeEvent(portBar1, PortChangeType.DELETE);
    private final PortChangeEvent portBar2Del =
            new PortChangeEvent(portBar2, PortChangeType.DELETE);

    @Before
    public void setUp() throws Exception {

        floodlightProvider = createMock(IFloodlightProviderService.class);
        sw = new OFSwitchTest(floodlightProvider);
        IDebugCounterService debugCounter = new DebugCounter();
        sw.setDebugCounterService(debugCounter);
        switches = new ConcurrentHashMap<Long, IOFSwitch>();
        switches.put(sw.getId(), sw);
        expect(floodlightProvider.getSwitch(sw.getId())).andReturn(sw).anyTimes();

    }

    /**
     * Takes a state and adds it to the passed state set
     *
     * @param state the set to add to or remove from
     * @param aState the state to be added or removed.
     * @param op add or remove operation
     * @return
     */
    private <T> Set<T> modState(Set<T> state, T aState, boolean op) {
        if (state == null)
            state = new HashSet<T>();
        if (op) {
            state.add(aState);
        } else {
            state.remove(aState);
        }
        return state;
    }

    /**
     * Check if a port is enabled
     * @param p the port
     * @return true id port is enabled and false otherwise.
     */
    private boolean isEnabled(OFPortDesc p) {
        return (p != null &&
                !p.getState().contains(OFPortState.LINK_DOWN) &&
                !p.getState().contains(OFPortState.BLOCKED) &&
                !p.getConfig().contains(OFPortConfig.PORT_DOWN));
    }

    @Before
    public void setUpPorts() {
        /*
         * Convenience variables to enhance readability.
         */
        final boolean ADD = true;
        final boolean REM = !ADD;

        OFPortDesc.Builder bld = factory10.buildPortDesc();
        // p1a is disabled

        p1a = bld.setName("port1")
                 .setPortNo(OFPort.of(1))
                 .setState(modState(bld.getState(), OFPortState.LINK_DOWN, ADD))
                 .build();

        assertFalse("Sanity check portEnabled", isEnabled(p1a));

        bld = factory10.buildPortDesc();
        // p1b is enabled
        // p1b has different feature from p1a
        p1b = bld.setName("port1")
                 .setPortNo(OFPort.of(1))
                 .setAdvertised(modState(bld.getAdvertised(),
                           OFPortFeatures.PF_1GB_FD, ADD))
                 .setState(modState(bld.getState(),
                           OFPortState.LINK_DOWN, REM))
                 .setConfig(modState(bld.getConfig(), OFPortConfig.PORT_DOWN, REM))
                 .build();
        assertTrue("Sanity check portEnabled", isEnabled(p1b));

        // p2 is disabled
        // p2 has mixed case
        bld = factory10.buildPortDesc();
        p2a = bld.setName("Port2")
                .setState(modState(bld.getState(),
                        OFPortState.LINK_DOWN, REM))
                .setConfig(modState(bld.getConfig(),
                        OFPortConfig.PORT_DOWN, ADD))
                .setPortNo(OFPort.of(2))
                .build();

     // p2b only differs in PortFeatures
        bld = factory10.buildPortDesc();

        p2b = bld.setName("Port2")
                .setState(modState(bld.getState(),
                        OFPortState.LINK_DOWN, REM))
                .setConfig(modState(bld.getConfig(),
                        OFPortConfig.PORT_DOWN, ADD))
                .setPortNo(OFPort.of(2))
                .setAdvertised(modState(bld.getAdvertised(),
                        OFPortFeatures.PF_100MB_HD, ADD))
                .build();
        assertFalse("Sanity check portEnabled", isEnabled(p2a));

        // p3 is enabled
        // p3 has mixed case
        bld = factory10.buildPortDesc();
        p3 = bld.setName("porT3")
                .setState(modState(bld.getState(),
                        OFPortState.LINK_DOWN, REM))
                .setPortNo(OFPort.of(3))
                .build();
        assertTrue("Sanity check portEnabled", isEnabled(p3));

    }

    /**
     * Test whether two collections contains the same elements, regardless
     * of the order in which the elements appear in the collections
     * @param expected
     * @param actual
     */
    private static <T> void assertCollectionEqualsNoOrder(Collection<T> expected,
                                         Collection<T> actual) {
        String msg = String.format("expected=%s, actual=%s",
                                   expected, actual);
        assertEquals(msg, expected.size(), actual.size());
        for(T e: expected) {
            if (!actual.contains(e)) {
                msg = String.format("Expected element %s not found in " +
                        "actual. expected=%s, actual=%s",
                    e, expected, actual);
                fail(msg);
            }
        }
    }


    /**
     * Test "normal" setPorts() and comparePorts() methods. No name<->number
     * conflicts or exception testing.
     */
    @Test
    public void testBasicSetPortOperations() {
        Collection<OFPortDesc> oldPorts = Collections.emptyList();
        Collection<OFPortDesc> oldEnabledPorts = Collections.emptyList();
        Collection<Integer> oldEnabledPortNumbers = Collections.emptyList();
        List<OFPortDesc> ports = new ArrayList<OFPortDesc>();


        Collection<PortChangeEvent> expectedChanges =
                new ArrayList<IOFSwitch.PortChangeEvent>();

        Collection<PortChangeEvent> actualChanges = sw.comparePorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertEquals(0, sw.getPorts().size());
        assertEquals(0, sw.getEnabledPorts().size());
        assertEquals(0, sw.getEnabledPortNumbers().size());

        actualChanges = sw.setPorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertEquals(0, sw.getPorts().size());
        assertEquals(0, sw.getEnabledPorts().size());
        assertEquals(0, sw.getEnabledPortNumbers().size());

        //---------------------------------------------
        // Add port p1a and p2a
        ports.add(p1a);
        ports.add(p2a);

        PortChangeEvent evP1aAdded =
                new PortChangeEvent(p1a, PortChangeType.ADD);
        PortChangeEvent evP2aAdded =
                new PortChangeEvent(p2a, PortChangeType.ADD);

        expectedChanges.clear();
        expectedChanges.add(evP1aAdded);
        expectedChanges.add(evP2aAdded);

        actualChanges = sw.comparePorts(ports);
        assertEquals(0, sw.getPorts().size());
        assertEquals(0, sw.getEnabledPorts().size());
        assertEquals(0, sw.getEnabledPortNumbers().size());
        assertEquals(2, actualChanges.size());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        actualChanges = sw.setPorts(ports);
        assertEquals(2, actualChanges.size());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        assertTrue("enabled ports should be empty",
                   sw.getEnabledPortNumbers().isEmpty());
        assertTrue("enabled ports should be empty",
                   sw.getEnabledPorts().isEmpty());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(null, sw.getPort((short)3));
        assertEquals(null, sw.getPort("port3"));
        assertEquals(null, sw.getPort("PoRt3")); // case insensitive get


        //----------------------------------------------------
        // Set the same ports again. No changes
        oldPorts = sw.getPorts();
        oldEnabledPorts = sw.getEnabledPorts();
        oldEnabledPortNumbers = sw.getEnabledPortNumbers();

        expectedChanges.clear();

        actualChanges = sw.comparePorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());

        actualChanges = sw.setPorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        assertTrue("enabled ports should be empty",
                   sw.getEnabledPortNumbers().isEmpty());
        assertTrue("enabled ports should be empty",
                   sw.getEnabledPorts().isEmpty());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(null, sw.getPort((short)3));
        assertEquals(null, sw.getPort("port3"));
        assertEquals(null, sw.getPort("PoRt3")); // case insensitive get

        //----------------------------------------------------
        // Remove p1a, add p1b. Should receive a port up
        oldPorts = sw.getPorts();
        oldEnabledPorts = sw.getEnabledPorts();
        oldEnabledPortNumbers = sw.getEnabledPortNumbers();
        ports.clear();
        ports.add(p2a);
        ports.add(p1b);

        // comparePorts
        PortChangeEvent evP1bUp = new PortChangeEvent(p1b, PortChangeType.UP);
        actualChanges = sw.comparePorts(ports);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());
        assertEquals(1, actualChanges.size());
        assertTrue("No UP event for port1", actualChanges.contains(evP1bUp));

        // setPorts
        actualChanges = sw.setPorts(ports);
        assertEquals(1, actualChanges.size());
        assertTrue("No UP event for port1", actualChanges.contains(evP1bUp));
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        List<OFPortDesc> enabledPorts = new ArrayList<OFPortDesc>();
        enabledPorts.add(p1b);
        List<Integer> enabledPortNumbers = new ArrayList<Integer>();
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(null, sw.getPort((short)3));
        assertEquals(null, sw.getPort("port3"));
        assertEquals(null, sw.getPort("PoRt3")); // case insensitive get

        //----------------------------------------------------
        // Remove p2a, add p2b. Should receive a port modify
        oldPorts = sw.getPorts();
        oldEnabledPorts = sw.getEnabledPorts();
        oldEnabledPortNumbers = sw.getEnabledPortNumbers();
        ports.clear();
        ports.add(p2b);
        ports.add(p1b);

        PortChangeEvent evP2bModified =
                new PortChangeEvent(p2b, PortChangeType.OTHER_UPDATE);

        // comparePorts
        actualChanges = sw.comparePorts(ports);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());
        assertEquals(1, actualChanges.size());
        assertTrue("No OTHER_CHANGE event for port2",
                   actualChanges.contains(evP2bModified));

        // setPorts
        actualChanges = sw.setPorts(ports);
        assertEquals(1, actualChanges.size());
        assertTrue("No OTHER_CHANGE event for port2",
                   actualChanges.contains(evP2bModified));
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts = new ArrayList<OFPortDesc>();
        enabledPorts.add(p1b);
        enabledPortNumbers = new ArrayList<Integer>();
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2b, sw.getPort((short)2));
        assertEquals(p2b, sw.getPort("port2"));
        assertEquals(p2b, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(null, sw.getPort((short)3));
        assertEquals(null, sw.getPort("port3"));
        assertEquals(null, sw.getPort("PoRt3")); // case insensitive get


        //----------------------------------------------------
        // Remove p1b, add p1a. Should receive a port DOWN
        // Remove p2b, add p2a. Should receive a port modify
        // Add p3, should receive an add
        oldPorts = sw.getPorts();
        oldEnabledPorts = sw.getEnabledPorts();
        oldEnabledPortNumbers = sw.getEnabledPortNumbers();
        ports.clear();
        ports.add(p2a);
        ports.add(p1a);
        ports.add(p3);

        PortChangeEvent evP1aDown =
                new PortChangeEvent(p1a, PortChangeType.DOWN);
        PortChangeEvent evP2aModified =
                new PortChangeEvent(p2a, PortChangeType.OTHER_UPDATE);
        PortChangeEvent evP3Add =
                new PortChangeEvent(p3, PortChangeType.ADD);
        expectedChanges.clear();
        expectedChanges.add(evP1aDown);
        expectedChanges.add(evP2aModified);
        expectedChanges.add(evP3Add);

        // comparePorts
        actualChanges = sw.comparePorts(ports);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        // setPorts
        actualChanges = sw.setPorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPorts.add(p3);
        enabledPortNumbers.clear();
        enabledPortNumbers.add(3);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(p3, sw.getPort((short)3));
        assertEquals(p3, sw.getPort("port3"));
        assertEquals(p3, sw.getPort("PoRt3")); // case insensitive get


        //----------------------------------------------------
        // Remove p1b Should receive a port DELETE
        // Remove p2b Should receive a port DELETE
        oldPorts = sw.getPorts();
        oldEnabledPorts = sw.getEnabledPorts();
        oldEnabledPortNumbers = sw.getEnabledPortNumbers();
        ports.clear();
        ports.add(p3);

        PortChangeEvent evP1aDel =
                new PortChangeEvent(p1a, PortChangeType.DELETE);
        PortChangeEvent evP2aDel =
                new PortChangeEvent(p2a, PortChangeType.DELETE);
        expectedChanges.clear();
        expectedChanges.add(evP1aDel);
        expectedChanges.add(evP2aDel);

        // comparePorts
        actualChanges = sw.comparePorts(ports);
        assertEquals(oldPorts, sw.getPorts());
        assertEquals(oldEnabledPorts, sw.getEnabledPorts());
        assertEquals(oldEnabledPortNumbers, sw.getEnabledPortNumbers());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        // setPorts
        actualChanges = sw.setPorts(ports);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPorts.add(p3);
        enabledPortNumbers.clear();
        enabledPortNumbers.add(3);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());

        assertEquals(p3, sw.getPort((short)3));
        assertEquals(p3, sw.getPort("port3"));
        assertEquals(p3, sw.getPort("PoRt3")); // case insensitive get
    }


    /**
     * Test "normal" OFPortStatus handling. No name<->number
     * conflicts or exception testing.
     */
    @Test
    public void testBasicPortStatusOperation() {
        OFPortStatus ps = null;
        List<OFPortDesc> ports = new ArrayList<OFPortDesc>();
        ports.add(p1a);
        ports.add(p2a);


        // Set p1a and p2a as baseline
        PortChangeEvent evP1aAdded =
                new PortChangeEvent(p1a, PortChangeType.ADD);
        PortChangeEvent evP2aAdded =
                new PortChangeEvent(p2a, PortChangeType.ADD);

        Collection<PortChangeEvent> expectedChanges =
                new ArrayList<IOFSwitch.PortChangeEvent>();
        expectedChanges.add(evP1aAdded);
        expectedChanges.add(evP2aAdded);

        Collection<PortChangeEvent> actualChanges = sw.comparePorts(ports);
        assertEquals(0, sw.getPorts().size());
        assertEquals(0, sw.getEnabledPorts().size());
        assertEquals(0, sw.getEnabledPortNumbers().size());
        assertEquals(2, actualChanges.size());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        actualChanges = sw.setPorts(ports);
        assertEquals(2, actualChanges.size());
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);

        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        assertTrue("enabled ports should be empty",
                   sw.getEnabledPortNumbers().isEmpty());
        assertTrue("enabled ports should be empty",
                   sw.getEnabledPorts().isEmpty());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        //----------------------------------------------------
        // P1a -> p1b. Should receive a port up
        ports.clear();
        ports.add(p2a);
        ports.add(p1b);

        ps =  factory10.buildPortStatus().setReason(OFPortReason.MODIFY).setDesc(p1b).build();

        PortChangeEvent evP1bUp = new PortChangeEvent(p1b, PortChangeType.UP);
        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP1bUp);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        List<OFPortDesc> enabledPorts = new ArrayList<OFPortDesc>();
        enabledPorts.add(p1b);
        List<Integer> enabledPortNumbers = new ArrayList<Integer>();
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get

        //----------------------------------------------------
        // p2a -> p2b. Should receive a port modify
        ports.clear();
        ports.add(p2b);
        ports.add(p1b);

        PortChangeEvent evP2bModified =
                new PortChangeEvent(p2b, PortChangeType.OTHER_UPDATE);

        ps = ps.createBuilder().setReason(OFPortReason.MODIFY).setDesc(p2b).build();



        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP2bModified);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts = new ArrayList<OFPortDesc>();
        enabledPorts.add(p1b);
        enabledPortNumbers = new ArrayList<Integer>();
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2b, sw.getPort((short)2));
        assertEquals(p2b, sw.getPort("port2"));
        assertEquals(p2b, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(null, sw.getPort((short)3));
        assertEquals(null, sw.getPort("port3"));
        assertEquals(null, sw.getPort("PoRt3")); // case insensitive get


        //----------------------------------------------------
        // p1b -> p1a. Via an OFPPR_ADD, Should receive a port DOWN
        ports.clear();
        ports.add(p2b);
        ports.add(p1a);

        // we use an ADD here. We treat ADD and MODIFY the same way
        ps = ps.createBuilder().setReason(OFPortReason.ADD).setDesc(p1a).build();


        PortChangeEvent evP1aDown =
                new PortChangeEvent(p1a, PortChangeType.DOWN);
        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP1aDown);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPortNumbers.clear();
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2b, sw.getPort((short)2));
        assertEquals(p2b, sw.getPort("port2"));
        assertEquals(p2b, sw.getPort("PoRt2")); // case insensitive get


        //----------------------------------------------------
        // p2b -> p2a. Via an OFPPR_ADD, Should receive a port MODIFY
        ports.clear();
        ports.add(p2a);
        ports.add(p1a);

        // we use an ADD here. We treat ADD and MODIFY the same way
        ps = ps.createBuilder().setReason(OFPortReason.ADD).setDesc(p2a).build();

        PortChangeEvent evP2aModify =
                new PortChangeEvent(p2a, PortChangeType.OTHER_UPDATE);
        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP2aModify);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPortNumbers.clear();
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(p2a, sw.getPort((short)2));
        assertEquals(p2a, sw.getPort("port2"));
        assertEquals(p2a, sw.getPort("PoRt2")); // case insensitive get


        //----------------------------------------------------
        // Remove p2a
        ports.clear();
        ports.add(p1a);

        ps = ps.createBuilder().setReason(OFPortReason.DELETE).setDesc(p2a).build();

        PortChangeEvent evP2aDel =
                new PortChangeEvent(p2a, PortChangeType.DELETE);
        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP2aDel);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPortNumbers.clear();
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get

        //----------------------------------------------------
        // Remove p2a again. Nothing should happen.
        ports.clear();
        ports.add(p1a);

        ps = ps.createBuilder().setReason(OFPortReason.DELETE).setDesc(p2a).build();

        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPortNumbers.clear();
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1a, sw.getPort((short)1));
        assertEquals(p1a, sw.getPort("port1"));
        assertEquals(p1a, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get


        //----------------------------------------------------
        // Remove p1a
        ports.clear();

        ps = ps.createBuilder().setReason(OFPortReason.DELETE).setDesc(p1a).build();

        PortChangeEvent evP1aDel =
                new PortChangeEvent(p1a, PortChangeType.DELETE);
        actualChanges = sw.processOFPortStatus(ps);
        expectedChanges.clear();
        expectedChanges.add(evP1aDel);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPortNumbers.clear();
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(null, sw.getPort((short)1));
        assertEquals(null, sw.getPort("port1"));
        assertEquals(null, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get


        //----------------------------------------------------
        // Add p3, should receive an add
        ports.clear();
        ports.add(p3);

        PortChangeEvent evP3Add =
                new PortChangeEvent(p3, PortChangeType.ADD);
        expectedChanges.clear();
        expectedChanges.add(evP3Add);

        ps = ps.createBuilder().setReason(OFPortReason.ADD).setDesc(p3).build();

        actualChanges = sw.processOFPortStatus(ps);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPorts.add(p3);
        enabledPortNumbers.clear();
        enabledPortNumbers.add(3);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(null, sw.getPort((short)1));
        assertEquals(null, sw.getPort("port1"));
        assertEquals(null, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(p3, sw.getPort((short)3));
        assertEquals(p3, sw.getPort("port3"));
        assertEquals(p3, sw.getPort("PoRt3")); // case insensitive get

        //----------------------------------------------------
        // Add p1b, back should receive an add
        ports.clear();
        ports.add(p1b);
        ports.add(p3);

        PortChangeEvent evP1bAdd =
                new PortChangeEvent(p1b, PortChangeType.ADD);
        expectedChanges.clear();
        expectedChanges.add(evP1bAdd);

        // use a modify to add the port
        ps = ps.createBuilder().setReason(OFPortReason.MODIFY).setDesc(p1b).build();

        actualChanges = sw.processOFPortStatus(ps);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPorts.add(p3);
        enabledPorts.add(p1b);
        enabledPortNumbers.clear();
        enabledPortNumbers.add(3);
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(p3, sw.getPort((short)3));
        assertEquals(p3, sw.getPort("port3"));
        assertEquals(p3, sw.getPort("PoRt3")); // case insensitive get

        //----------------------------------------------------
        // Modify, but nothing really changed
        ports.clear();
        ports.add(p1b);
        ports.add(p3);

        expectedChanges.clear();

        // use a modify to add the port
        ps = ps.createBuilder().setReason(OFPortReason.MODIFY).setDesc(p1b).build();

        actualChanges = sw.processOFPortStatus(ps);
        assertCollectionEqualsNoOrder(expectedChanges, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
        enabledPorts.clear();
        enabledPorts.add(p3);
        enabledPorts.add(p1b);
        enabledPortNumbers.clear();
        enabledPortNumbers.add(3);
        enabledPortNumbers.add(1);
        assertCollectionEqualsNoOrder(enabledPorts, sw.getEnabledPorts());
        assertCollectionEqualsNoOrder(enabledPortNumbers,
                                   sw.getEnabledPortNumbers());
        assertEquals(p1b, sw.getPort((short)1));
        assertEquals(p1b, sw.getPort("port1"));
        assertEquals(p1b, sw.getPort("PoRt1")); // case insensitive get

        assertEquals(null, sw.getPort((short)2));
        assertEquals(null, sw.getPort("port2"));
        assertEquals(null, sw.getPort("PoRt2")); // case insensitive get

        assertEquals(p3, sw.getPort((short)3));
        assertEquals(p3, sw.getPort("port3"));
        assertEquals(p3, sw.getPort("PoRt3")); // case insensitive get
    }


    /**
     * Test exception handling for setPorts() and comparePorts()
     */
    @Test
    @SuppressWarnings("EmptyStatement")
    public void testSetPortExceptions() {
        try {
            sw.setPorts(null);
            fail("Expected exception not thrown");
        } catch (NullPointerException e) { };

        // two ports with same name
        List<OFPortDesc> ports = new ArrayList<OFPortDesc>();
        ports.add(factory10.buildPortDesc().setName("port1")
                                           .setPortNo(OFPort.of(1))
                                           .build());
        ports.add(factory10.buildPortDesc().setName("port1")
                                           .setPortNo(OFPort.of(2))
                                           .build());

        try {
            sw.setPorts(ports);
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) { };

        // two ports with same number
        ports.clear();
        ports.add(factory10.buildPortDesc().setName("port1")
                                           .setPortNo(OFPort.of(1))
                                           .build());
        ports.add(factory10.buildPortDesc().setName("port2")
                                           .setPortNo(OFPort.of(1))
                                           .build());

        try {
            sw.setPorts(ports);
            fail("Expected exception not thrown");
        } catch (IllegalArgumentException e) { };

        // null port in list
        ports.clear();
        ports.add(factory10.buildPortDesc().setName("port1")
                .setPortNo(OFPort.of(1))
                .build());

        ports.add(null);
        try {
            sw.setPorts(ports);
            fail("Excpeted exception not thrown");
        } catch (NullPointerException e) { };

        // try getPort(null)
        try {
            sw.getPort(null);
            fail("Excpeted exception not thrown");
        } catch (NullPointerException e) { };

        //--------------------------
        // comparePorts()
        try {
            sw.comparePorts(null);
            fail("Excpeted exception not thrown");
        } catch (NullPointerException e) { };

        // two ports with same name
        ports = new ArrayList<OFPortDesc>();

        ports.add(factory10.buildPortDesc().setName("port1")
                .setPortNo(OFPort.of(1))
                .build());
        ports.add(factory10.buildPortDesc().setName("port1")
                .setPortNo(OFPort.of(2))
                .build());

        try {
            sw.comparePorts(ports);
            fail("Excpeted exception not thrown");
        } catch (IllegalArgumentException e) { };

        // two ports with same number
        ports.clear();
        ports.add(factory10.buildPortDesc().setName("port1")
                .setPortNo(OFPort.of(1))
                .build());
        ports.add(factory10.buildPortDesc().setName("port2")
                .setPortNo(OFPort.of(1))
                .build());

        try {
            sw.comparePorts(ports);
            fail("Excpeted exception not thrown");
        } catch (IllegalArgumentException e) { };

        // null port in list
        ports.clear();
        ports.add(factory10.buildPortDesc().setName("port1")
                .setPortNo(OFPort.of(1))
                .build());
        ports.add(null);
        try {
            sw.comparePorts(ports);
            fail("Excpeted exception not thrown");
        } catch (NullPointerException e) { };

        // try getPort(null)
        try {
            sw.getPort(null);
            fail("Excpeted exception not thrown");
        } catch (NullPointerException e) { };

    }

    @Test
    public void testPortStatusExceptions() {

        try {
            sw.processOFPortStatus(null);
            fail("Expected exception not thrown");
        } catch (NullPointerException e)  { }

        // illegal reason code

        /*
         *
         * AAS: Can't do this test because LOXI doesn't give you the ability to
         * set your own reason as a byte.
         *
         * ps = ps.createBuilder().setReason(OFPortReason.).build();
         * ps.setDesc(OFPortDesc.create("p1", (short)1).toOFPhysicalPort());
         * try {
         *   sw.processOFPortStatus(ps);
         *   fail("Expected exception not thrown");
         * } catch (IllegalArgumentException e)  { }
         */

        /*
         *  AAS: Loxi does not allow you to define a PortStatus message
         *   with no port so skipping this test.
         *
         *   // null port
         *   ps = factory10.buildPortStatus().setReason(OFPortReason.ADD)
         *                      .setDesc(null)
         *                      .build();
         *
         *   try {
         *       sw.processOFPortStatus(ps);
         *       fail("Expected exception not thrown");
         *   } catch (NullPointerException e)  { }
         */
    }

    /**
     * Assert that the expected PortChangeEvents have been recevied, asserting
     * the expected ordering.
     *
     * All events in earlyEvents have to appear in actualEvents before any
     * event in lateEvent appears. Events in anytimeEvents can appear at any
     * given time. earlyEvents, lateEvents, and anytimeEvents must be mutually
     * exclusive (their intersection must be none) and their union must
     * contain all elements from actualEvents
     * @param earlyEvents
     * @param lateEvents
     * @param anytimeEvents
     * @param actualEvents
     */
    private static void assertChangeEvents(Collection<PortChangeEvent> earlyEvents,
                                      Collection<PortChangeEvent> lateEvents,
                                      Collection<PortChangeEvent> anytimeEvents,
                                      Collection<PortChangeEvent> actualEvents) {
        String inputDesc = String.format("earlyEvents=%s, lateEvents=%s, " +
                "anytimeEvents=%s, actualEvents=%s",
                earlyEvents, lateEvents, anytimeEvents, actualEvents);
        // Make copies of expected lists, so we can modify them
        Collection<PortChangeEvent> early =
                new ArrayList<PortChangeEvent>(earlyEvents);
        Collection<PortChangeEvent> late =
                new ArrayList<PortChangeEvent>(lateEvents);
        Collection<PortChangeEvent> any =
                new ArrayList<PortChangeEvent>(anytimeEvents);

        // Sanity check: no overlap between early, late, and anytime events
        for (PortChangeEvent ev: early) {
            assertFalse("Test setup error. Early and late overlap",
                        late.contains(ev));
            assertFalse("Test setup error. Early and anytime overlap",
                        any.contains(ev));
        }
        for (PortChangeEvent ev: late) {
            assertFalse("Test setup error. Late and early overlap",
                        early.contains(ev));
            assertFalse("Test setup error. Late and any overlap",
                        any.contains(ev));
        }
        for (PortChangeEvent ev: any) {
            assertFalse("Test setup error. Anytime and early overlap",
                        early.contains(ev));
            assertFalse("Test setup error. Anytime and late overlap",
                        late.contains(ev));
        }

        for (PortChangeEvent a: actualEvents) {
            if (early.remove(a)) {
                continue;
            }
            if (any.remove(a)) {
                continue;
            }
            if (late.remove(a)) {
                if (!early.isEmpty()) {
                    fail(a + " is in late list, but haven't seen all required " +
                         "early events. " + inputDesc);
                } else {
                    continue;
                }
            }
            fail(a + " was not expected. " + inputDesc);
        }
        if (!early.isEmpty())
            fail("Elements left in early: " + early + ". " + inputDesc);
        if (!late.isEmpty())
            fail("Elements left in late: " + late + ". " + inputDesc);
        if (!any.isEmpty())
            fail("Elements left in any: " + any + ". " + inputDesc);
    }

    /**
     * Test setPort() with changing name / number mappings
     * We don't test comparePorts() here. We assume setPorts() and
     * comparePorts() use the same underlying implementation
     */
    @Test
    public void testSetPortNameNumberMappingChange() {

        List<OFPortDesc> ports = new ArrayList<OFPortDesc>();
        Collection<PortChangeEvent> early = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> late = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> anytime = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> actualChanges = null;

        ports.add(portFoo1);
        ports.add(p1a);
        sw.setPorts(ports);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Add portFoo2: name collision
        ports.clear();
        ports.add(portFoo2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        early.add(portFoo1Del);
        late.add(portFoo2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Add portBar2: number collision
        ports.clear();
        ports.add(portBar2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        early.add(portFoo2Del);
        late.add(portBar2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Set to portFoo1, portBar2. No collisions in this step
        ports.clear();
        ports.add(portFoo1);
        ports.add(portBar2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        anytime.add(portFoo1Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Add portFoo2: name and number collision
        ports.clear();
        ports.add(portFoo2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        early.add(portFoo1Del);
        early.add(portBar2Del);
        late.add(portFoo2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Set to portFoo2, portBar1. No collisions in this step
        ports.clear();
        ports.add(portFoo2);
        ports.add(portBar1);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        anytime.add(portBar1Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Add portFoo1, portBar2 name and number collision
        // Also change p1a -> p1b: expect modify for it
        // Also add p3: expect add for it
        PortChangeEvent p1bUp = new PortChangeEvent(p1b, PortChangeType.UP);
        PortChangeEvent p3Add = new PortChangeEvent(p3, PortChangeType.ADD);
        ports.clear();
        ports.add(portFoo1);
        ports.add(portBar2);
        ports.add(p1b);
        ports.add(p3);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.setPorts(ports);
        early.add(portFoo2Del);
        early.add(portBar1Del);
        late.add(portFoo1Add);
        late.add(portBar2Add);
        anytime.add(p1bUp);
        anytime.add(p3Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
    }


    @Test
    public void testPortStatusNameNumberMappingChange() {
        List<OFPortDesc> ports = new ArrayList<OFPortDesc>();
        Collection<PortChangeEvent> early = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> late = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> anytime = new ArrayList<PortChangeEvent>();
        Collection<PortChangeEvent> actualChanges = null;

        // init: add portFoo1, p1a
        ports.add(portFoo1);
        ports.add(p1a);
        sw.setPorts(ports);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        OFPortStatus ps = factory10.buildPortStatus()
                            .setReason(OFPortReason.MODIFY)
                            .setDesc(portFoo2)
                            .build();

        // portFoo1 -> portFoo2 via MODIFY : name collision
        ports.clear();
        ports.add(portFoo2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        early.add(portFoo1Del);
        late.add(portFoo2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // portFoo2 -> portBar2 via ADD number collision
        ps = ps.createBuilder().setReason(OFPortReason.ADD)
                               .setDesc(portBar2)
                               .build();

        ports.clear();
        ports.add(portBar2);
        ports.add(p1a);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        early.add(portFoo2Del);
        late.add(portBar2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Set to portFoo1, portBar2
        ports.clear();
        ports.add(portFoo1);
        ports.add(portBar2);
        sw.setPorts(ports);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // portFoo1 + portBar2 -> portFoo2: name and number collision
        ps = ps.createBuilder().setReason(OFPortReason.MODIFY)
                .setDesc(portFoo2)
                .build();

        ports.clear();
        ports.add(portFoo2);
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        early.add(portFoo1Del);
        early.add(portBar2Del);
        late.add(portFoo2Add);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        //----------------------
        // Test DELETEs

        // del portFoo1: name exists (portFoo2), but number doesn't.
        ps = ps.createBuilder().setReason(OFPortReason.DELETE)
                .setDesc(portFoo1)
                .build();

        ports.clear();
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        anytime.add(portFoo2Del);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // Set to portFoo1
        ports.clear();
        ports.add(portFoo1);
        sw.setPorts(ports);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // del portBar1: number exists (portFoo1), but name doesn't.
        ps = ps.createBuilder().setReason(OFPortReason.DELETE)
                .setDesc(portBar1)
                .build();

        ports.clear();
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        anytime.add(portFoo1Del);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());


        // Set to portFoo1, portBar2
        ports.clear();
        ports.add(portFoo1);
        ports.add(portBar2);
        sw.setPorts(ports);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());

        // del portFoo2: name and number exists
        ps = ps.createBuilder().setReason(OFPortReason.DELETE)
                .setDesc(portFoo2)
                .build();

        ports.clear();
        early.clear();
        late.clear();
        anytime.clear();
        actualChanges = sw.processOFPortStatus(ps);
        anytime.add(portFoo1Del);
        anytime.add(portBar2Del);
        assertChangeEvents(early, late, anytime, actualChanges);
        assertCollectionEqualsNoOrder(ports, sw.getPorts());
    }

    @Test
    public void testSubHandshake() {
        //Nicira role messages are vendor extentions should do the job.
        OFMessage m = factory10.niciraControllerRoleRequest(OFNiciraControllerRole.ROLE_MASTER);
        // BasicFactory.getInstance().getMessage(OFType.VENDOR);
        // test execptions before handshake is started
        try {
            sw.processDriverHandshakeMessage(m);
            fail("expected exception not thrown");
        } catch (SwitchDriverSubHandshakeNotStarted e) { /* expected */ }
        try {
            sw.isDriverHandshakeComplete();
            fail("expected exception not thrown");
        } catch (SwitchDriverSubHandshakeNotStarted e) { /* expected */ }

        // start the handshake -- it should immediately complete
        try {
            sw.startDriverHandshake();
        } catch (IOException e1) {
            fail("Unexpected IOException thrown.");
        }
        assertTrue("Handshake should be complete",
                   sw.isDriverHandshakeComplete());

        // test exceptions after handshake is completed
        try {
            sw.processDriverHandshakeMessage(m);
            fail("expected exception not thrown");
        } catch (SwitchDriverSubHandshakeCompleted e) { /* expected */ }
        try {
            sw.startDriverHandshake();
            fail("Expected exception not thrown");
        } catch (SwitchDriverSubHandshakeAlreadyStarted e) {
            /* expected */
        } catch (IOException e) {
            fail("Unexpected IOException thrown.");
        }
    }

}
