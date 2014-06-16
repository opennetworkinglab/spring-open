package net.onrc.onos.core.intent;

import java.util.Objects;

/**
 * The PathIntent is a "low-level" intent that is the manifestation
 * of a "high-level" intent. It contains the path through the network
 * and a pointer to the "high-level" intent.
 */
public class PathIntent extends Intent {
    protected Path path;
    protected double bandwidth;
    protected Intent parentIntent;

    /**
     * Generate the PathIntent ID for the first path for a
     * parent Intent. It is derived from the parent Intent ID.
     *
     * @param parentId parent Intent ID
     * @return new PathIntent ID
     */
    public static String createFirstId(String parentId) {
        return String.format("%s___0", parentId);
    }

    /**
     * Generate the PathIntent ID for subsequent paths used to
     * satisfy a parent Intent. It is derived from the previous ID.
     *
     * @param currentId current PathIntent's ID
     * @return new PathIntent ID
     */
    public static String createNextId(String currentId) {
        String[] parts = currentId.split("___");
        return String.format("%s___%d", parts[0], Long.parseLong(parts[1]) + 1);
    }

    /**
     * Default constructor for Kryo deserialization.
     */
    protected PathIntent() {
    }

    /**
     * Constructor.
     *
     * @param id           the PathIntent ID
     * @param path         the path for this Intent
     * @param bandwidth    bandwidth which should be allocated for the path.
     *                     If 0, no intent for bandwidth allocation (best effort).
     * @param parentIntent parent intent. If null, this is root intent.
     */
    public PathIntent(String id, Path path, double bandwidth, Intent parentIntent) {
        super(id);
        this.path = path;
        this.bandwidth = bandwidth;
        this.parentIntent = parentIntent;
    }

    /**
     * Get the bandwidth specified for this Intent.
     * TODO: specify unit
     *
     * @return this Intent's bandwidth
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * Get the Path for this Intent.
     *
     * @return this Intent's Path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get the parent Intent.
     *
     * @return the parent Intent
     */
    public Intent getParentIntent() {
        return parentIntent;
    }

    /**
     * Checks the specified PathIntent have the same fields of
     * path, bandwidth and parentIntent's id with this PathIntent.
     *
     * @param target target PathIntent instance
     * @return true if the specified intent has the same fields, otherwise false
     */
    public boolean hasSameFields(PathIntent target) {
        if (target == null) {
            return false;
        }
        if (!Objects.equals(getPath(), target.getPath())) {
            return false;
        }
        if (getBandwidth() != target.getBandwidth()) {
            return false;
        }
        return Objects.equals(getParentIntent(), target.getParentIntent());
    }

    /**
     * Generates a hash code using the Intent ID.
     *
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Compares two intent object by type (class) and Intent ID.
     *
     * @param obj other Intent
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Returns a String representation of this Intent.
     *
     * @return "Intent ID, State, Path"
     */
    @Override
    public String toString() {
        return String.format("%s, %s, %s", getId(), getState(), getPath());
    }
}
