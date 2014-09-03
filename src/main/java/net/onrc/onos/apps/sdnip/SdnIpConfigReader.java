package net.onrc.onos.apps.sdnip;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.main.config.IConfigInfoService;
import net.onrc.onos.core.util.SwitchPort;

import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * SDN-IP Config Reader provides IConfigInfoService
 * by reading from an SDN-IP configuration file.
 * It must be enabled on the nodes within the cluster
 * not running SDN-IP.
 * <p/>
 * TODO: As a long term solution, a module providing
 * general network configuration to ONOS nodes should be used.
 */
public class SdnIpConfigReader implements IFloodlightModule, IConfigInfoService {

    private static final Logger log = LoggerFactory.getLogger(SdnIpConfigReader.class);

    private static final String DEFAULT_CONFIG_FILENAME = "config.json";
    private String currentConfigFilename = DEFAULT_CONFIG_FILENAME;
    private Map<String, Interface> interfaces;
    private Map<InetAddress, BgpPeer> bgpPeers;
    private MACAddress bgpdMacAddress;
    private short vlan;
    private InvertedRadixTree<Interface> interfaceRoutes;
    private Set<SwitchPort> externalNetworkSwitchPorts;

    /**
     * Reads the info contained in the configuration file.
     *
     * @param configFilename The name of configuration file for SDN-IP application.
     */
    private void readConfiguration(String configFilename) {
        File gatewaysFile = new File(configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            Configuration config = mapper.readValue(gatewaysFile, Configuration.class);
            interfaces = new HashMap<>();
            for (Interface intf : config.getInterfaces()) {
                interfaces.put(intf.getName(), intf);
                externalNetworkSwitchPorts.add(new SwitchPort(intf.getDpid(),
                        intf.getPort()));
            }
            bgpPeers = new HashMap<>();
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.getIpAddress(), peer);
            }
            bgpdMacAddress = config.getBgpdMacAddress();
            vlan = config.getVlan();
        } catch (JsonParseException | JsonMappingException e) {
            log.error("Error in JSON file", e);
            throw new ConfigurationRuntimeException("Error in JSON file", e);
        } catch (IOException e) {
            log.error("Error reading JSON file", e);
            throw new ConfigurationRuntimeException("Error in JSON file", e);
        }

        // Populate the interface InvertedRadixTree
        for (Interface intf : interfaces.values()) {
            Prefix prefix = new Prefix(intf.getIpAddress().getAddress(),
                    intf.getPrefixLength());
            interfaceRoutes.put(prefix.toBinaryString(), intf);
        }
    }

    /**
     * To find the Interface which has longest matchable IP prefix (sub-network
     *  prefix) to next hop IP address.
     *
     * @param address the IP address of next hop router
     * @return Interface the Interface which has longest matchable IP prefix
     */
    private Interface longestInterfacePrefixMatch(InetAddress address) {
        Prefix prefixToSearchFor = new Prefix(address.getAddress(),
                Prefix.MAX_PREFIX_LENGTH);
        Iterator<Interface> it =
                interfaceRoutes.getValuesForKeysPrefixing(
                        prefixToSearchFor.toBinaryString()).iterator();
        Interface intf = null;
        // Find the last prefix, which will be the longest prefix
        while (it.hasNext()) {
            intf = it.next();
        }

        return intf;
    }

    @Override
    public boolean isInterfaceAddress(InetAddress address) {
        Interface intf = longestInterfacePrefixMatch(address);
        return (intf != null && intf.getIpAddress().equals(address));
    }

    @Override
    public boolean inConnectedNetwork(InetAddress address) {
        Interface intf = longestInterfacePrefixMatch(address);
        return (intf != null && !intf.getIpAddress().equals(address));
    }

    @Override
    public boolean fromExternalNetwork(long inDpid, short inPort) {
        for (Interface intf : interfaces.values()) {
            if (intf.getDpid() == inDpid && intf.getPort() == inPort) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Interface getOutgoingInterface(InetAddress dstIpAddress) {
        return longestInterfacePrefixMatch(dstIpAddress);
    }

    @Override
    public boolean hasLayer3Configuration() {
        return !interfaces.isEmpty();
    }

    @Override
    public MACAddress getRouterMacAddress() {
        return bgpdMacAddress;
    }

    @Override
    public short getVlan() {
        return vlan;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(IConfigInfoService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
        m.put(IConfigInfoService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        interfaceRoutes = new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        externalNetworkSwitchPorts = new HashSet<SwitchPort>();
        // Reading config values
        String configFilenameParameter = context.getConfigParams(this).get("configfile");
        if (configFilenameParameter != null) {
            currentConfigFilename = configFilenameParameter;
        }
        log.debug("Config file set to {}", currentConfigFilename);

        readConfiguration(currentConfigFilename);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
    }

    @Override
    public Set<SwitchPort> getExternalSwitchPorts() {
        return Collections.unmodifiableSet(externalNetworkSwitchPorts);
    }
}
