package net.onrc.onos.core.configmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.core.configmanager.NetworkConfig.LinkConfig;
import net.onrc.onos.core.configmanager.NetworkConfig.SwitchConfig;
import net.onrc.onos.core.util.Dpid;
import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.PortNumber;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetworkConfigManager manages all network configuration for switches, links
 * and any other state that needs to be configured for correct network
 * operation.
 *
 */
public class NetworkConfigManager implements IFloodlightModule,
        INetworkConfigService {
    protected static final Logger log = LoggerFactory
            .getLogger(NetworkConfigManager.class);
    public static final String DEFAULT_NETWORK_CONFIG_FILE =
            "conf/example-network.conf";
    /**
     * JSON Config file needs to use one of the following types for defining the
     * kind of switch or link it wishes to configure.
     */
    public static final String SEGMENT_ROUTER = "Router_SR";
    public static final String ROADM = "Roadm";
    public static final String OF10SWITCH = "Switch_OF10";

    public static final String PKT_LINK = "pktLink";
    public static final String WDM_LINK = "wdmLink";
    public static final String PKT_OPT_LINK = "pktOptLink";

    NetworkConfig networkConfig;
    private ConcurrentMap<Long, SwitchConfig> configuredSwitches;
    private ConcurrentMap<LinkTuple, LinkConfig> configuredLinks;
    private Map<String, Long> nameToDpid;

    // **************
    // INetworkConfigService
    // **************

    public SwitchConfigStatus checkSwitchConfig(Dpid dpid) {
        SwitchConfig swc = configuredSwitches.get(dpid.value());
        if (networkConfig.getRestrictSwitches()) {
            // default deny behavior
            if (swc == null) {
                // switch is not configured - we deny this switch
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch not configured, in network denying switches by default.");
            }
            if (swc.isAllowed()) {
                // switch is allowed in config, return configured attributes
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT_ADD, swc);
            } else {
                // switch has been configured off (administratively down)
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch configured down (allowed=false).");
            }
        } else {
            // default allow behavior
            if (swc == null) {
                // no config to add
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT, null);
            }
            if (swc.isAllowed()) {
                // switch is allowed in config, return configured attributes
                return new SwitchConfigStatus(NetworkConfigState.ACCEPT_ADD, swc);
            } else {
                // switch has been configured off (administratively down)
                return new SwitchConfigStatus(NetworkConfigState.DENY, null,
                        "Switch configured down (allowed=false).");
            }
        }

    }

    public LinkConfigStatus checkLinkConfig(LinkTuple linkTuple) {
        LinkConfig lkc = getConfiguredLink(linkTuple);
        // links are always disallowed if any one of the nodes that make up the
        // link are disallowed
        Dpid linkNode1 = linkTuple.getSrc().getDpid();
        SwitchConfigStatus scs1 = checkSwitchConfig(linkNode1);
        if (scs1.getConfigState() == NetworkConfigState.DENY) {
            return new LinkConfigStatus(NetworkConfigState.DENY, null,
                    "Link-node: " + linkNode1 + " denied by config: " + scs1.getMsg());
        }
        Dpid linkNode2 = linkTuple.getDst().getDpid();
        SwitchConfigStatus scs2 = checkSwitchConfig(linkNode2);
        if (scs2.getConfigState() == NetworkConfigState.DENY) {
            return new LinkConfigStatus(NetworkConfigState.DENY, null,
                    "Link-node: " + linkNode2 + " denied by config: " + scs2.getMsg());
        }
        if (networkConfig.getRestrictLinks()) {
            // default deny behavior
            if (lkc == null) {
                // link is not configured - we deny this link
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link not configured, in network denying links by default.");
            }
            if (lkc.isAllowed()) {
                // link is allowed in config, return configured attributes
                return new LinkConfigStatus(NetworkConfigState.ACCEPT_ADD, lkc);
            } else {
                // link has been configured off (administratively down)
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link configured down (allowed=false).");
            }
        } else {
            // default allow behavior
            if (lkc == null) {
                // no config to add
                return new LinkConfigStatus(NetworkConfigState.ACCEPT, null);
            }
            if (lkc.isAllowed()) {
                // link is allowed in config, return configured attributes
                return new LinkConfigStatus(NetworkConfigState.ACCEPT_ADD, lkc);
            } else {
                // link has been configured off (administratively down)
                return new LinkConfigStatus(NetworkConfigState.DENY, null,
                        "Link configured down (allowed=false).");
            }
        }

    }

    @Override
    public List<SwitchConfig> getConfiguredAllowedSwitches() {
        List<SwitchConfig> allowed = new ArrayList<SwitchConfig>();
        for (SwitchConfig swc : configuredSwitches.values()) {
            if (swc.isAllowed()) {
                allowed.add(swc);
            }
        }
        return allowed;
    }

    @Override
    public List<LinkConfig> getConfiguredAllowedLinks() {
        List<LinkConfig> allowed = new ArrayList<LinkConfig>();
        for (LinkConfig lkc : configuredLinks.values()) {
            if (lkc.isAllowed()) {
                allowed.add(lkc);
            }
        }
        return allowed;
    }

    @Override
    public List<List<String>> getOpticalReachabiltyConfig() {
        if (networkConfig.getOpticalReachability() != null) {
            return networkConfig.getOpticalReachability();
        }
        return Collections.emptyList();
    }

    @Override
    public Dpid getDpidForName(String name) {
        if (nameToDpid.get(name) != null) {
            return new Dpid(nameToDpid.get(name));
        }
        return null;
    }

    // **************
    // Private methods
    // **************

    private void loadNetworkConfig(FloodlightModuleContext context) {
        ObjectMapper mapper = new ObjectMapper();
        networkConfig = new NetworkConfig();

        Map<String, String> configParams = context.getConfigParams(this);
        String file = configParams.get("networkConfigFile");
        if (file == null) {
            // use default file which no-ops
            log.info("Using default file for network configuration");
            file = DEFAULT_NETWORK_CONFIG_FILE;
        } else {
            log.info("Loading network configuration from " + file);
        }

        try {
            networkConfig = mapper.readValue(new File(file), NetworkConfig.class);
        } catch (JsonParseException e) {
            String err = String.format("JsonParseException while loading network "
                    + "config from file: %s: %s", file, e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        } catch (JsonMappingException e) {
            String err = String.format(
                    "JsonMappingException while loading network config "
                            + "from file: %s: %s", file, e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        } catch (IOException e) {
            String err = String.format("IOException while loading network config "
                    + "from file: %s %s", file, e.getMessage());
            throw new NetworkConfigException.ErrorConfig(err);
        }

        log.info("Network config specifies: {} switches and {} links",
                (networkConfig.getRestrictSwitches())
                        ? networkConfig.getSwitchConfig().size() : "default allow",
                        (networkConfig.getRestrictLinks())
                        ? networkConfig.getLinkConfig().size() : "default allow");
    }

    private void parseNetworkConfig() {
        List<SwitchConfig> swConfList = networkConfig.getSwitchConfig();
        List<LinkConfig> lkConfList = networkConfig.getLinkConfig();
        validateSwitchConfig(swConfList);
        createTypeSpecificSwitchConfig(swConfList);
        validateLinkConfig(lkConfList);
        createTypeSpecificLinkConfig(lkConfList);
        // TODO validate reachability matrix 'names' for configured dpids
    }

    private void createTypeSpecificSwitchConfig(List<SwitchConfig> swConfList) {
        for (SwitchConfig swc : swConfList) {
            nameToDpid.put(swc.getName(), swc.getDpid());
            String swtype = swc.getType();
            switch (swtype) {
            case SEGMENT_ROUTER:
                SwitchConfig sr = new SegmentRouterConfig(swc);
                configuredSwitches.put(sr.getDpid(), sr);
                break;
            case ROADM:
                SwitchConfig rd = new RoadmConfig(swc);
                configuredSwitches.put(rd.getDpid(), rd);
                break;
            case OF10SWITCH:
                SwitchConfig of10 = new SwitchOF10Config(swc);
                configuredSwitches.put(of10.getDpid(), of10);
                break;
            default:
                throw new NetworkConfigException.UnknownSwitchType(swtype,
                        swc.getName());
            }
        }
    }

    private void createTypeSpecificLinkConfig(List<LinkConfig> lkConfList) {
        for (LinkConfig lkc : lkConfList) {
            String lktype = lkc.getType();
            switch (lktype) {
            case PKT_LINK:
                PktLinkConfig pk = new PktLinkConfig(lkc);
                for (LinkTuple lt : pk.getLinkTupleList()) {
                    configuredLinks.put(lt, pk);
                }
                break;
            case WDM_LINK:
                WdmLinkConfig wd = new WdmLinkConfig(lkc);
                for (LinkTuple lt : wd.getLinkTupleList()) {
                    configuredLinks.put(lt, wd);
                }
                break;
            case PKT_OPT_LINK:
                PktOptLinkConfig po = new PktOptLinkConfig(lkc);
                for (LinkTuple lt : po.getLinkTupleList()) {
                    configuredLinks.put(lt, po);
                }
                break;
            default:
                throw new NetworkConfigException.UnknownLinkType(lktype,
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
        }
    }

    private void validateSwitchConfig(List<SwitchConfig> swConfList) {
        Set<Long> swDpids = new HashSet<Long>();
        Set<String> swNames = new HashSet<String>();
        for (SwitchConfig swc : swConfList) {
            if (swc.getNodeDpid() == null || swc.getDpid() == 0) {
                throw new NetworkConfigException.DpidNotSpecified(swc.getName());
            }
            // ensure both String and Long values of dpid are set
            if (swc.getDpid() != HexString.toLong(swc.getNodeDpid())) {
                throw new NetworkConfigException.SwitchDpidNotConverted(
                        swc.getName());
            }
            if (swc.getName() == null) {
                throw new NetworkConfigException.NameNotSpecified(swc.getDpid());
            }
            if (swc.getType() == null) {
                throw new NetworkConfigException.SwitchTypeNotSpecified(
                        swc.getDpid());
            }
            if (!swDpids.add(swc.getDpid())) {
                throw new NetworkConfigException.DuplicateDpid(swc.getDpid());
            }
            if (!swNames.add(swc.getName())) {
                throw new NetworkConfigException.DuplicateName(swc.getName());
            }
            // TODO Add more validations
        }
    }

    private void validateLinkConfig(List<LinkConfig> lkConfList) {
        for (LinkConfig lkc : lkConfList) {
            if (lkc.getNodeDpid1() == null || lkc.getNodeDpid2() == null) {
                throw new NetworkConfigException.LinkDpidNotSpecified(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            // ensure both String and Long values are set
            if (lkc.getDpid1() != HexString.toLong(lkc.getNodeDpid1()) ||
                    lkc.getDpid2() != HexString.toLong(lkc.getNodeDpid2())) {
                throw new NetworkConfigException.LinkDpidNotConverted(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            if (lkc.getType() == null) {
                throw new NetworkConfigException.LinkTypeNotSpecified(
                        lkc.getNodeDpid1(), lkc.getNodeDpid2());
            }
            if (configuredSwitches.get(lkc.getDpid1()) == null) {
                throw new NetworkConfigException.LinkForUnknownSwitchConfig(
                        lkc.getNodeDpid1());
            }
            if (configuredSwitches.get(lkc.getDpid2()) == null) {
                throw new NetworkConfigException.LinkForUnknownSwitchConfig(
                        lkc.getNodeDpid2());
            }
            // TODO add more validations
        }

    }

    private LinkConfig getConfiguredLink(LinkTuple linkTuple) {
        LinkConfig lkc = null;
        // first try the unidirectional link with the ports assigned
        lkc = configuredLinks.get(linkTuple);
        if (lkc == null) {
            // try without ports, as configuration may be for all links
            // between the two switches
            LinkTuple portlessLink = new LinkTuple(linkTuple.getSrc().getDpid(),
                    PortNumber.uint32(0), linkTuple.getDst().getDpid(),
                    PortNumber.uint32(0));

            lkc = configuredLinks.get(portlessLink);
        }
        return lkc;
    }


    // **************
    // IFloodlightModule
    // **************

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(INetworkConfigService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<>();
        m.put(INetworkConfigService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        loadNetworkConfig(context);
        configuredSwitches = new ConcurrentHashMap<Long, SwitchConfig>();
        configuredLinks = new ConcurrentHashMap<LinkTuple, LinkConfig>();
        nameToDpid = new HashMap<String, Long>();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        parseNetworkConfig();
    }

}
