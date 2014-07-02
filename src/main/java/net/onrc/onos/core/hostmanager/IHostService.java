package net.onrc.onos.core.hostmanager;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.util.MACAddress;

/**
 * {@link HostManager} doesn't yet provide any API to fellow modules,
 * however making it export a dummy service means we can specify it as
 * a dependency of Forwarding.
 */
public interface IHostService extends IFloodlightService {

    public void addHostListener(IHostListener listener);

    public void removeHostListener(IHostListener listener);

    public void deleteHost(Host host);

    public void deleteHostByMac(MACAddress mac);

    public void addHost(Long mac, Host host);
}
