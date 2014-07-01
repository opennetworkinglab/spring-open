package net.onrc.onos.core.topology;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.Validate;

/**
 * Base class for Topology Elements.
 * <p/>
 * Self-contained element, it is expected to be used as if it is an immutable
 * object.
 *
 * @param <T> Sub-class' type.
 *      (Required to define a method returning itself's type)
 */
public class TopologyElement<T extends TopologyElement<T>>
        implements StringAttributes, UpdateStringAttributes {

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
     * Create an unfrozen copy of given Object.
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
        Validate.notNull(value, "attribute value cannot be null");

        return this.stringAttributes.putIfAbsent(attr, value) == null;
    }

    @Override
    public boolean replaceStringAttribute(String attr, String oldValue, String value) {
        if (isFrozen) {
            throw new IllegalStateException("Tried to modify frozen object: " + this);
        }
        Validate.notNull(value, "attribute value cannot be null");

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
}
