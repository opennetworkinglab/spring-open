package net.onrc.onos.core.topology;

import java.util.Map;

/**
 * Interface for Elements with StringAttributes.
 */
public interface StringAttributes {
    /**
     * Gets the string attribute.
     *
     * @param attr attribute name
     * @return attribute value or null
     */
    public String getStringAttribute(final String attr);

    /**
     * Gets the string attribute.
     *
     * @param attr attribute name
     * @param def default value if {@code attr} did not exist
     * @return attribute value or null
     */
    public String getStringAttribute(final String attr, final String def);

    /**
     * Gets all the string attributes.
     *
     * @return Immutable Map containing all the String attributes.
     */
    public Map<String, String> getAllStringAttributes();
}
