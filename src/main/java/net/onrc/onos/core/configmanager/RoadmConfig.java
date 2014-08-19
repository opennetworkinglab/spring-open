package net.onrc.onos.core.configmanager;

import java.util.concurrent.ConcurrentHashMap;

import net.onrc.onos.core.configmanager.NetworkConfig.SwitchConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadmConfig extends SwitchConfig {
    protected static final Logger log = LoggerFactory
            .getLogger(RoadmConfig.class);
    private int numRegen;

    public RoadmConfig(SwitchConfig swc) {
        this.setName(swc.getName());
        this.setDpid(swc.getDpid());
        this.setType(swc.getType());
        this.setLatitude(swc.getLatitude());
        this.setLongitude(swc.getLongitude());
        this.setParams(swc.getParams());
        this.setAllowed(swc.isAllowed());
        publishAttributes = new ConcurrentHashMap<String, String>();
        parseParams();
        validateParams();
        setPublishAttributes();
    }

    public int getNumRegen() {
        return numRegen;
    }

    public void setNumRegen(int numRegen) {
        this.numRegen = numRegen;
    }

    private void parseParams() {
        // TODO
    }

    private void validateParams() {
        // TODO
    }

    private void setPublishAttributes() {
        // TODO
    }
}
