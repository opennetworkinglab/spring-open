package net.onrc.onos.apps.sdnip;

import java.util.Iterator;

/**
 * A PATRICIA tree is data structure for storing data where entries aggregate
 * based on shared prefixes. They provide lookups in O(k) where k is the
 * maximum length of the strings in the tree. They work well for storing IP
 * addresses.
 * <p/>
 * SDN-IP uses a patricia tree to store routes learnt from BGPd. BGPd sends
 * route updates in the form:
 * {@code <prefix, next_hop>},
 * e.g. {@code <192.168.1.0/24, 10.0.0.1>}
 * <p/>
 * These updates are stored in the patricia tree, which acts as a map from
 * {@code prefix} to {@code next_hop}. {@code next_hop} values can be looked up
 * by prefix.
 *
 * @param <V> The class of the data to stored in the patricia tree
 *
 * @see <a href="http://en.wikipedia.org/wiki/Patricia_tree">Patricia tree</a>
 */
public interface IPatriciaTree<V> {
    /**
     * Puts a new mapping into the patricia tree.
     *
     * @param prefix the Prefix which is the key for this entry
     * @param value the value that maps to the Prefix
     * @return the old value that was mapped to the Prefix, or null if there
     * was no such mapping
     */
    public V put(Prefix prefix, V value);

    /**
     * Searches the tree for a prefix that exactly matches the argument. If an
     * exact match for the prefix is found in the tree, the value it maps to is
     * returned. Otherwise, null is returned.
     *
     * @param prefix the prefix to look up in the tree
     * @return the value if the prefix was found, otherwise null
     */
    public V lookup(Prefix prefix);

    /**
     * Searches the tree for the closest containing prefix of the supplied
     * argument. If an exact match is found, that will be returned. Otherwise,
     * the value of the most specific prefix that contains the argument prefix
     * will be returned. If no such prefix is found, null is returned.
     *
     * @param prefix the prefix to find the closest containing match for in the
     * tree
     * @return the value of the match if one was found, otherwise null
     */
    public V match(Prefix prefix);

    /**
     * Removes a prefix to value mapping from the tree. The prefix argument is
     * first looked up in the same way as the {@link #lookup(Prefix)} method.
     * If an exact match to the prefix is found in the tree, its value is
     * is checked to see if it matches the supplied argument value. The prefix
     * and value will be removed only if both the prefix and value arguments
     * match a mapping in the tree.
     *
     * @param prefix the prefix to remove from the tree
     * @param value the value that must be mapped to the prefix for it to be
     * removed
     * @return true if a mapping was removed, otherwise false
     */
    public boolean remove(Prefix prefix, V value);

    /**
     * Gets an iterator over all mappings in the tree.
     *
     * @return an iterator that will iterate over all entries in the tree
     */
    public Iterator<Entry<V>> iterator();

    /**
     * Represents an entry in the patricia tree. The {@code Entry} is a mapping
     * from {@link Prefix} to a value object of type {@code V}.
     *
     * @param <V> the class of objects stored in the tree
     */
    interface Entry<V> {
        /**
         * Gets the {@link Prefix} object for this entry.
         *
         * @return the prefix for this entry
         */
        public Prefix getPrefix();

        /**
         * Gets the value of this entry.
         *
         * @return the value object of this entry
         */
        public V getValue();
    }
}
