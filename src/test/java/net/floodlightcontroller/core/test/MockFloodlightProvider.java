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

package net.floodlightcontroller.core.test;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.IUpdate;
import net.floodlightcontroller.core.internal.Controller.Counters;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.ListenerDispatcher;
import net.onrc.onos.core.configmanager.INetworkConfigService;
import net.onrc.onos.core.packet.Ethernet;
import net.onrc.onos.core.util.OnosInstanceId;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class MockFloodlightProvider implements IFloodlightModule,
        IFloodlightProviderService {
    protected final static Logger log = LoggerFactory
            .getLogger(MockFloodlightProvider.class);
    protected ConcurrentMap<OFType, ListenerDispatcher<OFType, IOFMessageListener>> listeners;
    protected List<IOFSwitchListener> switchListeners;
    protected Map<Long, IOFSwitch> switches;

    // TODO: need to add connected switches?
    protected ConcurrentHashMap<Long, IOFSwitch> activeMasterSwitches;
    protected ConcurrentHashMap<Long, IOFSwitch> activeEqualSwitches;

    /**
     *
     */
    public MockFloodlightProvider() {
        listeners = new ConcurrentHashMap<OFType, ListenerDispatcher<OFType,
                IOFMessageListener>>();
        switches = new ConcurrentHashMap<Long, IOFSwitch>();
        switchListeners = new CopyOnWriteArrayList<IOFSwitchListener>();
    }

    @Override
    public synchronized void addOFMessageListener(OFType type,
            IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                listeners.get(type);
        if (ldd == null) {
            ldd = new ListenerDispatcher<OFType, IOFMessageListener>();
            listeners.put(type, ldd);
        }
        ldd.addListener(type, listener);
    }

    @Override
    public synchronized void removeOFMessageListener(OFType type,
            IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                listeners.get(type);
        if (ldd != null) {
            ldd.removeListener(listener);
        }
    }

    /**
     * @return the listeners
     */
    @Override
    public Map<OFType, List<IOFMessageListener>> getListeners() {
        Map<OFType, List<IOFMessageListener>> lers =
                new HashMap<OFType, List<IOFMessageListener>>();
        for (Entry<OFType, ListenerDispatcher<OFType, IOFMessageListener>> e : listeners
                .entrySet()) {
            lers.put(e.getKey(), e.getValue().getOrderedListeners());
        }
        return Collections.unmodifiableMap(lers);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    @Override
    public Map<Long, IOFSwitch> getSwitches() {
        return this.switches;
    }

    public void setSwitches(Map<Long, IOFSwitch> switches) {
        this.switches = switches;
    }

    @Override
    public void addOFSwitchListener(IOFSwitchListener listener) {
        switchListeners.add(listener);
    }

    @Override
    public void removeOFSwitchListener(IOFSwitchListener listener) {
        switchListeners.remove(listener);
    }

    public void dispatchMessage(IOFSwitch sw, OFMessage msg) {
        dispatchMessage(sw, msg, new FloodlightContext());
    }

    public void dispatchMessage(IOFSwitch sw, OFMessage msg, FloodlightContext bc) {
        List<IOFMessageListener> theListeners = listeners.get(msg.getType())
                .getOrderedListeners();
        if (theListeners != null) {
            Command result = Command.CONTINUE;
            Iterator<IOFMessageListener> it = theListeners.iterator();
            if (OFType.PACKET_IN.equals(msg.getType())) {
                OFPacketIn pi = (OFPacketIn) msg;
                Ethernet eth = new Ethernet();
                eth.deserialize(pi.getData(), 0, pi.getData().length);
                IFloodlightProviderService.bcStore.put(bc,
                        IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                        eth);
            }
            while (it.hasNext() && !Command.STOP.equals(result)) {
                result = it.next().receive(sw, msg, bc);
            }
        }
    }

    // TODO: should be modify?
    /*    @Override
        public void handleOutgoingMessage(IOFSwitch sw, OFMessage m, FloodlightContext bc) {
            List<IOFMessageListener> msgListeners = null;
            if (listeners.containsKey(m.getType())) {
                msgListeners = listeners.get(m.getType()).getOrderedListeners();
            }

            if (msgListeners != null) {
                for (IOFMessageListener listener : msgListeners) {
                    if (listener instanceof IOFSwitchFilter) {
                        if (!((IOFSwitchFilter) listener).isInterested(sw)) {
                            continue;
                        }
                    }
                    if (Command.STOP.equals(listener.receive(sw, m, bc))) {
                        break;
                    }
                }
            }
        }

        public void handleOutgoingMessages(IOFSwitch sw, List<OFMessage> msglist, FloodlightContext bc) {
            for (OFMessage m : msglist) {
                handleOutgoingMessage(sw, m, bc);
            }
        }
    */
    /**
     * @return the switchListeners
     */
    public List<IOFSwitchListener> getSwitchListeners() {
        return switchListeners;
    }

    // TODO: check if needed
    /*
    @Override
    public void terminate() {
    }

    @Override
    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg) {
        dispatchMessage(sw, msg);
        return true;
    }

    @Override
    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg,
            FloodlightContext bContext) {
        dispatchMessage(sw, msg, bContext);
        return true;
    }

     */

    @Override
    public void run() {
        logListeners();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>(1);
        services.add(IFloodlightProviderService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>,
                IFloodlightService>();
        m.put(IFloodlightProviderService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public OnosInstanceId getOnosInstanceId() {
        return new OnosInstanceId("localhost");
    }

    @Override
    public Map<String, String> getControllerNodeIPs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getSystemStartTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    private void logListeners() {
        for (Map.Entry<OFType, ListenerDispatcher<OFType, IOFMessageListener>> entry : listeners
                .entrySet()) {

            OFType type = entry.getKey();
            ListenerDispatcher<OFType, IOFMessageListener> ldd =
                    entry.getValue();

            StringBuffer sb = new StringBuffer();
            sb.append("OFListeners for ");
            sb.append(type);
            sb.append(": ");
            for (IOFMessageListener l : ldd.getOrderedListeners()) {
                sb.append(l.getName());
                sb.append(",");
            }
            log.debug(sb.toString());
        }
    }

    @Override
    public void setAlwaysClearFlowsOnSwAdd(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void publishUpdate(IUpdate update) {
        // TODO Auto-generated method stub

    }

    @Override
    public Counters getCounters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAlwaysClearFlowsOnSwActivate(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Long> getMemory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    @Override
    public Set<Long> getAllSwitchDpids() {
        return this.switches.keySet();
    }

    @Override
    public IOFSwitch getSwitch(long dpid) {
        return this.switches.get(dpid);
    }

    @Override
    public void addSwitchEvent(long switchDPID, String reason, boolean flushNow) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<Long> getAllMasterSwitchDpids() {
        return this.activeMasterSwitches.keySet();
    }

    @Override
    public Set<Long> getAllEqualSwitchDpids() {
        return this.activeEqualSwitches.keySet();
    }

    @Override
    public IOFSwitch getMasterSwitch(long dpid) {
        return this.activeMasterSwitches.get(dpid);
    }

    @Override
    public IOFSwitch getEqualSwitch(long dpid) {
        return this.activeEqualSwitches.get(dpid);
    }

    @Override
    public INetworkConfigService getNetworkConfigService() {
        // TODO Auto-generated method stub
        return null;
    }

}
