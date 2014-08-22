package net.onrc.onos.core.matchaction;
import java.util.LinkedList;

/**
 * This class wraps a list of SwitchResults. It is required to be able to pass
 * SwitchResults via a Hazelcast channel.
 */
public class SwitchResultList extends LinkedList<SwitchResult> {

    static final long serialVersionUID = -4966789015808022563L;

    /**
     * Add a switch result to the list.
     *
     * @param result switch result to add to the list
     * @return true
     */
    public boolean add(SwitchResult result) {
        return super.add(result);
    }
}
