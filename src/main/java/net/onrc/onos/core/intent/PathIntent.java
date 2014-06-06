package net.onrc.onos.core.intent;

import java.util.Objects;

/**
 * @author Toshio Koide (t-koide@onlab.us)
 */
public class PathIntent extends Intent {
    protected Path path;
    protected double bandwidth;
    protected Intent parentIntent;

    public static String createFirstId(String parentId) {
        return String.format("%s___0", parentId);
    }

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
     * @param path
     * @param bandwidth    bandwidth which should be allocated for the path.
     *                     If 0, no intent for bandwidth allocation (best effort).
     * @param parentIntent parent intent. If null, this is root intent.
     * @param id
     */
    public PathIntent(String id, Path path, double bandwidth, Intent parentIntent) {
        super(id);
        this.path = path;
        this.bandwidth = bandwidth;
        this.parentIntent = parentIntent;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public Path getPath() {
        return path;
    }

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

    @Override
    public int hashCode() {
        // TODO: Is this the intended behavior?
        return (super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: Is this the intended behavior?
        return (super.equals(obj));
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s", getId(), getState(), getPath());
    }
}
