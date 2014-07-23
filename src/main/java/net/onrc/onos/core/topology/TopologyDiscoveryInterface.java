package net.onrc.onos.core.topology;

import java.util.Collection;

/**
 * Interface used by the Topology Discovery module to write topology-related
 * events.
 */
public interface TopologyDiscoveryInterface {
    /**
     * Switch discovered event.
     *
     * @param switchEvent the switch event.
     * @param portEvents  the corresponding port events for the switch.
     */
    public void putSwitchDiscoveryEvent(SwitchEvent switchEvent,
                                        Collection<PortEvent> portEvents);

    /**
     * Switch removed event.
     *
     * @param switchEvent the switch event.
     */
    public void removeSwitchDiscoveryEvent(SwitchEvent switchEvent);

    /**
     * Port discovered event.
     *
     * @param portEvent the port event.
     */
    public void putPortDiscoveryEvent(PortEvent portEvent);

    /**
     * Port removed event.
     *
     * @param portEvent the port event.
     */
    public void removePortDiscoveryEvent(PortEvent portEvent);

    /**
     * Link discovered event.
     *
     * @param linkEvent the link event.
     */
    public void putLinkDiscoveryEvent(LinkEvent linkEvent);

    /**
     * Link removed event.
     *
     * @param linkEvent the link event.
     */
    public void removeLinkDiscoveryEvent(LinkEvent linkEvent);

    /**
     * Host discovered event.
     *
     * @param hostEvent the host event.
     */
    public void putHostDiscoveryEvent(HostEvent hostEvent);

    /**
     * Host removed event.
     *
     * @param hostEvent the host event.
     */
    public void removeHostDiscoveryEvent(HostEvent hostEvent);

    /**
     * Switch Mastership updated event.
     *
     * @param mastershipEvent the mastership event.
     */
    public void putSwitchMastershipEvent(MastershipEvent mastershipEvent);

    /**
     * Switch Mastership removed event.
     *
     * @param mastershipEvent the mastership event.
     */
    public void removeSwitchMastershipEvent(MastershipEvent mastershipEvent);
}
