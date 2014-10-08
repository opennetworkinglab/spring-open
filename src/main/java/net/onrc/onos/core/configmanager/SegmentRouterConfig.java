package net.onrc.onos.core.configmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.onrc.onos.core.configmanager.NetworkConfig.SwitchConfig;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegmentRouterConfig extends SwitchConfig {
    protected static final Logger log = LoggerFactory
            .getLogger(SegmentRouterConfig.class);
    private String routerIp;
    private String routerMac;
    private int nodeSid;
    private boolean isEdgeRouter;
    private List<AdjacencySid> adjacencySids;
    private List<Subnet> subnets;

    public static final String ROUTER_IP = "routerIp";
    public static final String ROUTER_MAC = "routerMac";
    public static final String NODE_SID = "nodeSid";
    public static final String ADJACENCY_SIDS = "adjacencySids";
    public static final String SUBNETS = "subnets";
    public static final String ISEDGE = "isEdgeRouter";

    public SegmentRouterConfig(SwitchConfig swc) {
        this.setName(swc.getName());
        this.setDpid(swc.getDpid());
        this.setType(swc.getType());
        this.setLatitude(swc.getLatitude());
        this.setLongitude(swc.getLongitude());
        this.setParams(swc.getParams());
        this.setAllowed(swc.isAllowed());
        publishAttributes = new ConcurrentHashMap<String, String>();
        adjacencySids = new ArrayList<AdjacencySid>();
        subnets = new ArrayList<Subnet>();
        parseParams();
        validateParams();
        setPublishAttributes();
    }

    // ********************
    // Segment Router Configuration
    // ********************
    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    public String getRouterMac() {
        return routerMac;
    }

    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    public int getNodeSid() {
        return nodeSid;
    }

    public void setNodeSid(int nodeSid) {
        this.nodeSid = nodeSid;
    }

    public boolean isEdgeRouter() {
        return isEdgeRouter;
    }

    public void setIsEdgeRouter(boolean isEdge) {
        this.isEdgeRouter = isEdge;
    }

    public static class AdjacencySid {
        private int portNo;
        private int adjSid;

        public AdjacencySid(int portNo, int adjSid) {
            this.portNo = portNo;
            this.adjSid = adjSid;
        }

        public int getPortNo() {
            return portNo;
        }

        public void setPortNo(int portNo) {
            this.portNo = portNo;
        }

        public int getAdjSid() {
            return adjSid;
        }

        public void setAdjSid(int adjSid) {
            this.adjSid = adjSid;
        }
    }

    public List<AdjacencySid> getAdjacencySids() {
        return adjacencySids;
    }

    public void setAdjacencySids(List<AdjacencySid> adjacencySids) {
        this.adjacencySids = adjacencySids;
    }

    public static class Subnet {
        private int portNo;
        private String subnetIp;

        public Subnet(int portNo, String subnetIp) {
            this.portNo = portNo;
            this.subnetIp = subnetIp;
        }

        public int getPortNo() {
            return portNo;
        }

        public void setPortNo(int portNo) {
            this.portNo = portNo;
        }

        public String getSubnetIp() {
            return subnetIp;
        }

        public void setSubnetIp(String subnetIp) {
            this.subnetIp = subnetIp;
        }
    }

    public List<Subnet> getSubnets() {
        return subnets;
    }

    public void setSubnets(List<Subnet> subnets) {
        this.subnets = subnets;
    }

    // ********************
    // Helper methods
    // ********************

    private void parseParams() {
        if (params == null) {
            throw new NetworkConfigException.ParamsNotSpecified(name);
        }

        Set<Entry<String, JsonNode>> m = params.entrySet();
        for (Entry<String, JsonNode> e : m) {
            String key = e.getKey();
            JsonNode j = e.getValue();
            if (key.equals("routerIp")) {
                setRouterIp(j.asText());
            } else if (key.equals("routerMac")) {
                setRouterMac(j.asText());
            } else if (key.equals("nodeSid")) {
                setNodeSid(j.asInt());
            } else if (key.equals("isEdgeRouter")) {
                setIsEdgeRouter(j.asBoolean());
            }
            else if (key.equals("adjacencySids") || key.equals("subnets")) {
                getInnerParams(j, key);
            } else {
                throw new UnknownSegmentRouterConfig(key, dpid);
            }
        }
    }

    private void getInnerParams(JsonNode j, String innerParam) {
        Iterator<JsonNode> innerList = j.getElements();
        while (innerList.hasNext()) {
            Iterator<Entry<String, JsonNode>> f = innerList.next().getFields();
            int portNo = -1;
            int adjSid = -1;
            String subnetIp = null;
            while (f.hasNext()) {
                Entry<String, JsonNode> fe = f.next();
                if (fe.getKey().equals("portNo")) {
                    portNo = fe.getValue().asInt();
                } else if (fe.getKey().equals("adjSid")) {
                    adjSid = fe.getValue().asInt();
                } else if (fe.getKey().equals("subnetIp")) {
                    subnetIp = fe.getValue().asText();
                } else {
                    throw new UnknownSegmentRouterConfig(fe.getKey(), dpid);
                }
            }
            if (innerParam.equals("adjacencySids")) {
                AdjacencySid ads = new AdjacencySid(portNo, adjSid);
                adjacencySids.add(ads);
            } else {
                Subnet sip = new Subnet(portNo, subnetIp);
                subnets.add(sip);
            }
        }
    }

    private void validateParams() {
        if (routerIp == null) {
            throw new IpNotSpecified(dpid);
        }
        if (isEdgeRouter && subnets.isEmpty()) {
            throw new SubnetNotSpecifiedInEdgeRouter(dpid);
        }
        if (!isEdgeRouter && !subnets.isEmpty()) {
            throw new SubnetSpecifiedInBackboneRouter(dpid);
        }

        // TODO more validations
    }

    /**
     * Setting publishAttributes implies that this is the configuration that
     * will be added to Topology.Switch object before it is published on the
     * channel to other controller instances.
     */
    private void setPublishAttributes() {
        publishAttributes.put(ROUTER_IP, routerIp);
        publishAttributes.put(ROUTER_MAC, routerMac);
        publishAttributes.put(NODE_SID, String.valueOf(nodeSid));
        publishAttributes.put(ISEDGE, String.valueOf(isEdgeRouter));
        ObjectMapper mapper = new ObjectMapper();
        try {
            publishAttributes.put(ADJACENCY_SIDS,
                    mapper.writeValueAsString(adjacencySids));
            publishAttributes.put(SUBNETS,
                    mapper.writeValueAsString(subnets));
        } catch (JsonProcessingException e) {
            log.error("Error while writing SR config: {}", e.getCause());
        } catch (IOException e) {
            log.error("Error while writing SR config: {}", e.getCause());
        }
    }

    // ********************
    // Exceptions
    // ********************

    public static class IpNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -3001502553646331686L;

        public IpNotSpecified(long dpid) {
            super();
            log.error("Router IP address not specified for SR config dpid:{}",
                    HexString.toHexString(dpid));
        }
    }

    public static class UnknownSegmentRouterConfig extends RuntimeException {
        private static final long serialVersionUID = -5750132094884129179L;

        public UnknownSegmentRouterConfig(String key, long dpid) {
            super();
            log.error("Unknown Segment Router config {} in dpid: {}", key,
                    HexString.toHexString(dpid));
        }
    }

    public static class SubnetNotSpecifiedInEdgeRouter extends RuntimeException {
        private static final long serialVersionUID = -5855458472668581268L;

        public SubnetNotSpecifiedInEdgeRouter(long dpid) {
            super();
            log.error("Subnet was not specified for edge router in dpid: {}",
                    HexString.toHexString(dpid));
        }
    }

    public static class SubnetSpecifiedInBackboneRouter extends RuntimeException {
        private static final long serialVersionUID = -5855458472668581268L;

        public SubnetSpecifiedInBackboneRouter(long dpid) {
            super();
            log.error("Subnet was specified in backbone router in dpid: {}",
                    HexString.toHexString(dpid));
        }
    }



}
