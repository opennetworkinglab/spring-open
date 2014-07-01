package net.onrc.onos.core.topology;

// TODO Need better name
/**
 * Update String Attributes.
 */
public interface UpdateStringAttributes extends StringAttributes {

    /**
     * Creates the string attribute.
     *
     * @param attr attribute name
     * @param value new value to replace with
     * @return true if success, false if the attribute already exist
     */
    public boolean createStringAttribute(final String attr,
                                         final String value);

    /**
     * Replaces the existing string attribute.
     *
     * @param attr attribute name
     * @param oldValue old value to replace
     * @param value new value to replace with
     * @return true if success
     */
    public boolean replaceStringAttribute(final String attr,
                                     final String oldValue, final String value);

    /**
     * Deletes existing string attribute.
     *
     * @param attr attribute name
     * @param expectedValue value expected to be deleted
     * @return true if success, false if an attribute already exist
     */
    public boolean deleteStringAttribute(final String attr,
                                         final String expectedValue);

    /**
     * Deletes string attribute.
     *
     * @param attr attribute name
     */
    public void deleteStringAttribute(final String attr);

}
