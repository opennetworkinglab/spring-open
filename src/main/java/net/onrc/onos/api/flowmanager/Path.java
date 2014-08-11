package net.onrc.onos.api.flowmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.onrc.onos.core.util.Dpid;

/**
 * Path representation for the flow manager.
 */
public class Path extends FlowLinks {
    /**
     * Default constructor to create an empty path.
     */
    public Path() {
        super();
    }

    /**
     * Constructor to create the object from the list of FlowLinks.
     *
     * @param links the list of FlowLinks
     * @throws IllegalArgumentException if the links does not form a single
     *         connected directed path topology
     */
    public Path(List<FlowLink> links) {
        super();
        checkNotNull(links);
        for (FlowLink link : links) {
            if (!addLink(link)) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Adds FlowLink to the Path object.
     *
     * @param link the FlowLink object to be added
     * @return true if succeeded, false otherwise
     */
    public boolean addLink(FlowLink link) {
        // TODO check connectivity
        checkNotNull(link);
        return add(link);
    }

    /**
     * Gets a list of switch DPIDs of the path.
     *
     * @return a list of Dpid objects
     */
    public List<Dpid> getDpids() {
        if (size() < 1) {
            return null;
        }

        List<Dpid> dpids = new ArrayList<Dpid>(size() + 1);
        dpids.add(getSrcDpid());
        for (FlowLink link : this) {
            dpids.add(link.getDstDpid());
        }
        return dpids;
    }

    /**
     * Gets the DPID of the first switch.
     *
     * @return a Dpid object of the first switch.
     */
    public Dpid getSrcDpid() {
        if (size() < 1) {
            return null;
        }
        return get(0).getSrcDpid();
    }

    /**
     * Gets the DPID of the last switch.
     *
     * @return a Dpid object of the last switch.
     */
    public Dpid getDstDpid() {
        if (size() < 1) {
            return null;
        }
        return get(size() - 1).getDstDpid();
    }

    /**
     * Returns a string representation of the path.
     *
     * @return a string representation of the path.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<FlowLink> i = this.iterator();
        while (i.hasNext()) {
            builder.append(i.next().toString());
            if (i.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
