package net.onrc.onos.core.configmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class corresponding to JSON described data model. Defines the network
 * configuration at startup.
 */
public class NetworkConfig {
    protected static final Logger log = LoggerFactory.getLogger(NetworkConfig.class);

    @SuppressWarnings("unused")
    private String comment;

    private Boolean restrictSwitches;
    private Boolean restrictLinks;
    private List<SwitchConfig> switches;
    private List<LinkConfig> links;
    private List<List<String>> opticalReachability;

    public NetworkConfig() {
        switches = new ArrayList<SwitchConfig>();
        links = new ArrayList<LinkConfig>();
        opticalReachability = new ArrayList<List<String>>();
    }

    public void setComment(String c) {
        comment = c;
    }

    public void setRestrictSwitches(boolean rs) {
        restrictSwitches = rs;
    }

    public Boolean getRestrictSwitches() {
        return restrictSwitches;
    }

    public void setRestrictLinks(boolean rl) {
        restrictLinks = rl;
    }

    public Boolean getRestrictLinks() {
        return restrictLinks;
    }

    // *********************
    // switches
    // *********************/

    public List<SwitchConfig> getSwitchConfig() {
        return switches;
    }

    public void setSwitchConfig(List<SwitchConfig> switches2) {
        this.switches = switches2;
    }

    public static class SwitchConfig {
        protected String name;
        protected long dpid;
        protected String nodeDpid;
        protected String type;
        protected double latitude;
        protected double longitude;
        protected boolean allowed;
        protected Map<String, JsonNode> params;
        protected Map<String, String> publishAttributes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getDpid() {
            return dpid;
        }

        public void setDpid(long dpid) {
            this.dpid = dpid;
            this.nodeDpid = HexString.toHexString(dpid);
        }

        public String getNodeDpid() {
            return nodeDpid;
        }

        public String getHexDpid() {
            return nodeDpid;
        }

        // mapper sets both long and string fields for dpid
        public void setNodeDpid(String nodeDpid) {
            this.nodeDpid = nodeDpid;
            this.dpid = HexString.toLong(nodeDpid);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }

        public Map<String, JsonNode> getParams() {
            return params;
        }

        public void setParams(Map<String, JsonNode> params) {
            this.params = params;
        }

        public Map<String, String> getPublishAttributes() {
            return publishAttributes;
        }

        public void setPublishAttributes(Map<String, String> publishAttributes) {
            this.publishAttributes = publishAttributes;
        }

    }

    // *********************
    // links
    // *********************/

    public void setLinkConfig(List<LinkConfig> links2) {
        this.links = links2;
    }

    public List<LinkConfig> getLinkConfig() {
        return links;
    }

    public static class LinkConfig {
        protected String type;
        protected Boolean allowed;
        protected long dpid1;
        protected long dpid2;
        protected String nodeDpid1;
        protected String nodeDpid2;
        protected Map<String, JsonNode> params;
        protected Map<String, String> publishAttributes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(Boolean allowed) {
            this.allowed = allowed;
        }

        public String getNodeDpid1() {
            return nodeDpid1;
        }

        // mapper sets both long and string fields for dpid
        public void setNodeDpid1(String nodeDpid1) {
            this.nodeDpid1 = nodeDpid1;
            this.dpid1 = HexString.toLong(nodeDpid1);
        }

        public String getNodeDpid2() {
            return nodeDpid2;
        }

        // mapper sets both long and string fields for dpid
        public void setNodeDpid2(String nodeDpid2) {
            this.nodeDpid2 = nodeDpid2;
            this.dpid2 = HexString.toLong(nodeDpid2);
        }

        public long getDpid1() {
            return dpid1;
        }

        public void setDpid1(long dpid1) {
            this.dpid1 = dpid1;
            this.nodeDpid1 = HexString.toHexString(dpid1);
        }

        public long getDpid2() {
            return dpid2;
        }

        public void setDpid2(long dpid2) {
            this.dpid2 = dpid2;
            this.nodeDpid2 = HexString.toHexString(dpid2);
        }

        public Map<String, JsonNode> getParams() {
            return params;
        }

        public void setParams(Map<String, JsonNode> params) {
            this.params = params;
        }

        public Map<String, String> getPublishAttributes() {
            return publishAttributes;
        }

        public void setPublishAttributes(Map<String, String> publishAttributes) {
            this.publishAttributes = publishAttributes;
        }
    }

    // *********************
    // optical reach matrix
    // *********************/

    public void setOpticalReachabilty(List<List<String>> opticalReach) {
        this.opticalReachability = opticalReach;
    }
    public List<List<String>> getOpticalReachability() {
        return opticalReachability;
    }

}

