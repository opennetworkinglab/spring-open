package net.onrc.onos.core.topology;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

import net.onrc.onos.core.util.Dpid;

/**
 * Base class for Topology Elements.
 * <p/>
 * Self-contained element, it is expected to be used as if it is an immutable
 * object.
 *
 * @param <T> Sub-class' type.
 *      (Required to define a method returning itself's type)
 */
public abstract class TopologyElement<T extends TopologyElement<T>>
        implements ITopologyElement, StringAttributes, UpdateStringAttributes {

    // TODO: Where should the attribute names be defined?
    /**
     * Attribute name for type.
     */
    public static final String TYPE = "type";
    /**
     * Attribute "type" value representing that the object belongs to Packet
     * layer.
     */
    public static final String TYPE_PACKET_LAYER = "packet";
    /**
     * Attribute "type" value representing that the object belongs to Optical
     * layer.
     */
    public static final String TYPE_OPTICAL_LAYER = "optical";
    /**
     * Attribute "type" value representing that the object belongs to all
     * layers.
     */
    public static final String TYPE_ALL_LAYERS = "AllLayers";

    public static final String ELEMENT_CONFIG_STATE = "ConfigState";

    public static final String ELEMENT_ADMIN_STATUS = "AdminStatus";

    /**
     * Attribute name for device type.
     */
    public static final String ELEMENT_TYPE = "ElementType";

    private boolean isFrozen = false;

    private ConcurrentMap<String, String> stringAttributes;



    /**
     * Default constructor for serializer.
     */
    protected TopologyElement() {
        this.isFrozen = false;
        this.stringAttributes = new ConcurrentHashMap<>();
    }

    /**
     * Creates an unfrozen copy of given Object.
     * <p/>
     * Sub-classes should do a deep-copies if necessary.
     *
     * @param original to make copy of.
     */
    public TopologyElement(TopologyElement<T> original) {
        this.isFrozen = false;
        this.stringAttributes = new ConcurrentHashMap<>(original.stringAttributes);

    }


    /**
     * Tests if this instance is frozen.
     *
     * @return true if frozen.
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Freezes this instance to avoid further modifications.
     *
     * @return this
     */
    @SuppressWarnings("unchecked")
    public T freeze() {
        isFrozen = true;
        return (T) this;
    }

    @Override
    public int hashCode() {
        return stringAttributes.hashCode();
    }

    /*
     *  (non-Javadoc)
     * Equality based only on string attributes.
     *
     * Subclasses should call super.equals().
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        TopologyElement<T> other = (TopologyElement<T>) obj;
        return Objects.equals(stringAttributes, other.stringAttributes);
    }

    @Override
    public String getStringAttribute(String attr) {
        return this.stringAttributes.get(attr);
    }

    @Override
    public String getStringAttribute(String attr, String def) {
        final String v = getStringAttribute(attr);
        if (v == null) {
            return def;
        } else {
            return v;
        }
    }

    @Override
    public Map<String, String> getAllStringAttributes() {
        return Collections.unmodifiableMap(this.stringAttributes);
    }

    @Override
    public boolean createStringAttribute(String attr, String value) {
        if (isFrozen) {
            throw new IllegalStateException("Tried to modify frozen object: " + this);
        }
        checkNotNull(value, "attribute value cannot be null");

        return this.stringAttributes.putIfAbsent(attr, value) == null;
    }

    @Override
    public boolean replaceStringAttribute(String attr, String oldValue, String value) {
        if (isFrozen) {
            throw new IllegalStateException("Tried to modify frozen object: " + this);
        }
        checkNotNull(value, "attribute value cannot be null");

        return this.stringAttributes.replace(attr, oldValue, value);
    }

    @Override
    public boolean deleteStringAttribute(String attr, String expectedValue) {
        if (isFrozen) {
            throw new IllegalStateException("Tried to modify frozen object: " + this);
        }

        return this.stringAttributes.remove(attr, expectedValue);
    }

    @Override
    public void deleteStringAttribute(String attr) {
        if (isFrozen) {
            throw new IllegalStateException("Tried to modify frozen object: " + this);
        }

        this.stringAttributes.remove(attr);
    }

    @Override
    public String getType() {
        return getStringAttribute(TYPE, TYPE_PACKET_LAYER);
    }

    /**
     * Returns the config state of topology element.
     *
     * @return ConfigState
     */
    @Override
    public ConfigState getConfigState() {
        return ConfigState.valueOf(getStringAttribute(ELEMENT_CONFIG_STATE,
                                                      ConfigState.NOT_CONFIGURED.toString()));
    }

    /**
     * Returns the status of topology element.
     *
     * @return AdminStatus
     */
    @Override
    public AdminStatus getStatus() {
        return AdminStatus.valueOf(getStringAttribute(ELEMENT_ADMIN_STATUS));
    }

    /**
     * Gets the topology event origin DPID.
     *
     * @return the topology event origin DPID.
     */
    abstract Dpid getOriginDpid();

    /**
     * Gets the topology event ID as a ByteBuffer.
     *
     * @return the topology event ID as a ByteBuffer.
     */
    abstract ByteBuffer getIDasByteBuffer();
}
