package net.onrc.onos.core.datastore;

import java.net.InetAddress;

import net.onrc.onos.core.datastore.IKVTable.IKVEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVArpCache {

    private static final Logger log = LoggerFactory.getLogger(KVArpCache.class);

    private static final String GLOBAL_ARPCACHE_TABLE_NAME = "arp_cache";
    private final IKVTable table;

    public long getVersionNonexistant() {
        return table.getVersionNonexistant();
    }

    public KVArpCache() {
        table = DataStoreClient.getClient().getTable(GLOBAL_ARPCACHE_TABLE_NAME);
        log.debug("create table {}", table.getTableId());
    }

    public void dropArpCache() {
        DataStoreClient.getClient().dropTable(table);
        log.debug("drop table {}", table.getTableId());
    }

    public long create(InetAddress ip, byte[] mac) throws ObjectExistsException {
        return table.create(ip.getAddress(), mac);
    }

    public long forceCreate(InetAddress ip, byte[] mac) {
        return table.forceCreate(ip.getAddress(), mac);
    }

    public IKVEntry read(InetAddress ip) throws ObjectDoesntExistException {
        return table.read(ip.getAddress());
    }

    public long update(InetAddress ip, byte[] mac) throws ObjectDoesntExistException {
        return table.update(ip.getAddress(), mac);
    }

    public long forceDelete(InetAddress ip) {
        return table.forceDelete(ip.getAddress());
    }

    public Iterable<IKVEntry> getAllEntries() {
        return table.getAllEntries();
    }
}
