package net.onrc.onos.apps.proxyarp;


/**
 * Inter-instance notification that an ARP Cache status has been changed.
 */
public class ArpCacheNotification {

    private byte[] targetAddress;
    private byte[] targetMacAddress;

    private ArpCacheNotification() {
        targetAddress = null;
        targetMacAddress = null;
    }

    /**
     * Class constructor.
     *
     * @param targetAddress    IP address
     * @param targetMacAddress MAC address
     */
    public ArpCacheNotification(byte[] targetAddress,
                                byte[] targetMacAddress) {
        this.targetAddress = (byte[]) targetAddress.clone();
        this.targetMacAddress = (byte[]) targetMacAddress.clone();
    }

    /**
     * Returns the IP address.
     *
     * @return the IP address
     */
    public byte[] getTargetAddress() {
        return (byte[]) targetAddress.clone();
    }

    /**
     * Returns the MAC address.
     *
     * @return the MAC address
     */
    public byte[] getTargetMacAddress() {
        return (byte[]) targetMacAddress.clone();
    }

}
