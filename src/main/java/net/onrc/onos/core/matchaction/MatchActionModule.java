package net.onrc.onos.core.matchaction;

import java.util.EventListener;
import java.util.Set;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;

/**
 * Manages Match-Action entries.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class MatchActionModule implements MatchActionService {

    @Override
    public boolean addMatchAction(MatchAction matchAction) {
        return false;
    }

    @Override
    public Set<MatchAction> getMatchActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean executeOperations(final MatchActionOperations operations) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setConflictDetectionPolicy(ConflictDetectionPolicy policy) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConflictDetectionPolicy getConflictDetectionPolicy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEventListener(EventListener listener) {
        // TODO Auto-generated method stub

    }
}
