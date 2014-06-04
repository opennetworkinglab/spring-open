package net.onrc.onos.core.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.onrc.onos.core.registry.web.RegistryWebRoutable;

import org.apache.commons.lang.NotImplementedException;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a registry that doesn't rely on any external registry
 * service. This is designed to be used only in single-node setups (e.g. for
 * development). All registry data is stored in local memory.
 */
public class StandaloneRegistry implements IFloodlightModule,
        IControllerRegistryService {
    private static final Logger log = LoggerFactory.getLogger(StandaloneRegistry.class);

    private IRestApiService restApi;

    private String registeredControllerId;
    private Map<String, ControlChangeCallback> switchCallbacks;

    private long blockTop;
    private static final long BLOCK_SIZE = 0x1000000L;

    //
    // Unique ID generation state
    //
    private static AtomicLong nextUniqueId = new AtomicLong(0);

    @Override
    public void requestControl(long dpid, ControlChangeCallback cb)
            throws RegistryException {
        if (registeredControllerId == null) {
            throw new IllegalStateException(
                    "Must register a controller before calling requestControl");
        }

        switchCallbacks.put(HexString.toHexString(dpid), cb);

        log.debug("Control granted for {}", HexString.toHexString(dpid));

        // Immediately grant request for control
        if (cb != null) {
            cb.controlChanged(dpid, true);
        }
    }

    @Override
    public void releaseControl(long dpid) {
        ControlChangeCallback cb = switchCallbacks.remove(HexString.toHexString(dpid));

        log.debug("Control released for {}", HexString.toHexString(dpid));

        if (cb != null) {
            cb.controlChanged(dpid, false);
        }
    }

    @Override
    public boolean hasControl(long dpid) {
        return switchCallbacks.containsKey(HexString.toHexString(dpid));
    }

    @Override
    public boolean isClusterLeader() {
        return true;
    }

    @Override
    public String getControllerId() {
        return registeredControllerId;
    }

    @Override
    public void registerController(String controllerId)
            throws RegistryException {
        if (registeredControllerId != null) {
            throw new RegistryException(
                    "Controller already registered with id " + registeredControllerId);
        }
        registeredControllerId = controllerId;
    }

    @Override
    public Collection<String> getAllControllers() throws RegistryException {
        //List<String> l = new ArrayList<String>();
        //l.add(registeredControllerId);
        //return l;
        return Collections.singletonList(registeredControllerId);
    }

    @Override
    public String getControllerForSwitch(long dpid) throws RegistryException {
        return (switchCallbacks.get(HexString.toHexString(dpid)) == null)
                ? null : registeredControllerId;
    }

    @Override
    public Map<String, List<ControllerRegistryEntry>> getAllSwitches() {
        Map<String, List<ControllerRegistryEntry>> switches =
                new HashMap<String, List<ControllerRegistryEntry>>();

        for (String strSwitch : switchCallbacks.keySet()) {
            log.debug("Switch _{}", strSwitch);
            List<ControllerRegistryEntry> list =
                    new ArrayList<ControllerRegistryEntry>();
            list.add(new ControllerRegistryEntry(registeredControllerId, 0));

            switches.put(strSwitch, list);
        }

        return switches;
    }

    @Override
    public Collection<Long> getSwitchesControlledByController(
            String controllerId) {
        throw new NotImplementedException("Not yet implemented");
    }

    /**
     * Returns a block of IDs which are unique and unused.
     * Range of IDs is fixed size and is assigned incrementally as this method
     * called.
     *
     * @return an IdBlock containing a set of unique IDs
     */
    @Override
    public IdBlock allocateUniqueIdBlock() {
        synchronized (this)  {
            long blockHead = blockTop;
            long blockTail = blockTop + BLOCK_SIZE;

            IdBlock block = new IdBlock(blockHead, blockTail - 1, BLOCK_SIZE);
            blockTop = blockTail;

            return block;
        }
    }

    /**
     * Get a globally unique ID.
     *
     * @return a globally unique ID.
     */
    @Override
    public long getNextUniqueId() {
        return nextUniqueId.incrementAndGet();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IControllerRegistryService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        m.put(IControllerRegistryService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IRestApiService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        restApi = context.getServiceImpl(IRestApiService.class);

        switchCallbacks = new HashMap<String, ControlChangeCallback>();
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        restApi.addRestletRoutable(new RegistryWebRoutable());
    }

    @Override
    public IdBlock allocateUniqueIdBlock(long range) {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
