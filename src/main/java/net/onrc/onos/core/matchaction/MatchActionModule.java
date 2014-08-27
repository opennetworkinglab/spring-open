package net.onrc.onos.core.matchaction;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.core.util.IdGenerator;

/**
 * Manages Match-Action entries.
 * <p>
 * TODO: Make all methods thread-safe
 */
public class MatchActionModule implements MatchActionFloodlightService {

    private final HashSet<MatchAction> currentOperations = new HashSet<>();

    private boolean processMatchActionEntries(
            final List<MatchActionOperationEntry> entries) {
        int successfulOperations = 0;
        for (final MatchActionOperationEntry entry : entries) {
            if (currentOperations.add(entry.getTarget())) {
                successfulOperations++;
            }
        }
        return entries.size() == successfulOperations;
    }

    @Override
    public boolean addMatchAction(MatchAction matchAction) {
        return false;
    }

    @Override
    public Set<MatchAction> getMatchActions() {
        return Collections.unmodifiableSet(currentOperations);
    }

    @Override
    public boolean executeOperations(final MatchActionOperations operations) {
        return processMatchActionEntries(operations.getOperations());
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
    public IdGenerator<MatchActionId> getMatchActionIdGenerator() {
        // TODO Auto-generated method stub
        // use MatchActionIdGeneratorWithIdBlockAllocator.
        return null;
    }

    @Override
    public IdGenerator<MatchActionOperationsId> getMatchActionOperationsIdGenerator() {
        // TODO Auto-generated method stub
        // use MatchActionOperationsIdGeneratorWithIdBlockAllocator.
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
