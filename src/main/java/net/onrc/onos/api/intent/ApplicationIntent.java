package net.onrc.onos.api.intent;

/**
 * An object used by the application to specify an intent.
 */
public class ApplicationIntent {
    private String intentId;            // The Intent ID
    private String intentType;          // The Intent type

    // If true, don't update the path when topology changes
    private boolean isStaticPath;

    private String srcSwitchDpid;       // Flow Path Source Switch DPID
    private int srcSwitchPort;          // Flow Path Source Switch Port
    private String dstSwitchDpid;       // Flow Path Destination Switch DPID
    private int dstSwitchPort;          // Flow Path Destination Switch Port
    private double bandwidth;        // Bandwidth for Constrained Shortest Path

    // Matching Fields
    private String matchSrcMac;         // Matching source MAC address
    private String matchDstMac;         // Matching destination MAC address

    /**
     * Gets the Intent ID.
     *
     * @return the Intent ID.
     */
    public String getIntentId() {
        return this.intentId;
    }

    /**
     * Sets the Intent ID.
     *
     * @param intentId the Intent ID to set.
     */
    public void setIntentId(String intentId) {
        this.intentId = intentId;
    }

    /**
     * Gets the Intent type.
     *
     * Currently, the following strings are recognized:
     *   - "SHORTEST_PATH"
     *   - "CONSTRAINED_SHORTEST_PATH"
     *
     * @return the Intent type.
     */
    public String getIntentType() {
        return this.intentType;
    }

    /**
     * Sets the Intent type.
     *
     * Currently, the following strings are recognized:
     *   - "SHORTEST_PATH"
     *   - "CONSTRAINED_SHORTEST_PATH"
     *
     * @param intentType the Intent type to set.
     */
    public void setIntentType(String intentType) {
        this.intentType = intentType;
    }

    /**
     * Gets the "staticPath" flag for the intent.
     *
     * A path for an intent is defined as "static" if it shouldn't be updated
     * when the topology changes.
     *
     * @return true if the intent path is static, otherwise false.
     */
    public boolean isStaticPath() {
        return this.isStaticPath;
    }

    /**
     * Sets the "staticPath" flag for the intent.
     *
     * A path for an intent is defined as "static" if it shouldn't be updated
     * when the topology changes.
     *
     * @param staticPath true if the intent path is static, otherwise false.
     */
    public void setStaticPath(boolean staticPath) {
        this.isStaticPath = staticPath;
    }

    /**
     * Gets the Source Switch DPID.
     *
     * @return the Source Switch DPID.
     */
    public String getSrcSwitchDpid() {
        return this.srcSwitchDpid;
    }

    /**
     * Sets the Source Switch DPID.
     *
     * @param srcSwitchDpid the Source Switch DPID to set.
     */
    public void setSrcSwitchDpid(String srcSwitchDpid) {
        this.srcSwitchDpid = srcSwitchDpid;
    }

    /**
     * Gets the Source Switch Port.
     *
     * @return the Source Switch Port.
     */
    public int getSrcSwitchPort() {
        return this.srcSwitchPort;
    }

    /**
     * Sets the Source Switch Port.
     *
     * @param srcSwitchPort the Source Switch Port to set.
     */
    public void setSrcSwitchPort(int srcSwitchPort) {
        this.srcSwitchPort = srcSwitchPort;
    }

    /**
     * Gets the Destination Switch DPID.
     *
     * @return the Destination Switch DPID.
     */
    public String getDstSwitchDpid() {
        return this.dstSwitchDpid;
    }

    /**
     * Sets the Destination Switch DPID.
     *
     * @param dstSwitchDpid the Destination Switch DPID to set.
     */
    public void setDstSwitchDpid(String dstSwitchDpid) {
        this.dstSwitchDpid = dstSwitchDpid;
    }

    /**
     * Gets the Destination Switch Port.
     *
     * @return the Destination Switch Port.
     */
    public int getDstSwitchPort() {
        return this.dstSwitchPort;
    }

    /**
     * Sets the Destination Switch Port.
     *
     * @param dstSwitchPort the Destination Switch Port to set.
     */
    public void setDstSwitchPort(int dstSwitchPort) {
        this.dstSwitchPort = dstSwitchPort;
    }

    /**
     * Gets the bandwidth for Constrained Shortest Path.
     *
     * @return the bandwidth for Constrained Shortest Path.
     */
    public double getBandwidth() {
        return this.bandwidth;
    }

    /**
     * Sets the bandwidth for Constrained Shortest Path.
     *
     * @param bandwidth the bandwidth for Constrained Shortest Path
     */
    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Gets the matching source MAC address.
     *
     * @return the matching source MAC address.
     */
    public String getMatchSrcMac() {
        return this.matchSrcMac;
    }

    /**
     * Sets the matching source MAC address.
     *
     * @param matchSrcMac the matching source MAC address to set.
     */
    public void setMatchSrcMac(String matchSrcMac) {
        this.matchSrcMac = matchSrcMac;
    }

    /**
     * Gets the matching destination MAC address.
     *
     * @return the matching destination MAC address.
     */
    public String getMatchDstMac() {
        return this.matchDstMac;
    }

    /**
     * Sets the matching destination MAC address.
     *
     * @param matchDstMac the matching destination MAC address to set.
     */
    public void setMatchDstMac(String matchDstMac) {
        this.matchDstMac = matchDstMac;
    }
}
