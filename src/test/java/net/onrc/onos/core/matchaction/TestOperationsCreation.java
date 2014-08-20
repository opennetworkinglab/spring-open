package net.onrc.onos.core.matchaction;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


/**
 * Unit tests for creation of MatchActionOperations.
 */
public class TestOperationsCreation {

    /**
     * Checks creation of Match Action Operations.
     */
    @Test
    public void testOperationsCreation() {
        //  Create the MatchActionOperations
        final MatchActionOperationsId operationsId =
            new MatchActionOperationsId(1L);
        final MatchActionOperations operations =
                new MatchActionOperations(operationsId);

        //  Create one MatchActionEntry and add it to the Operations

        final MatchActionId matchActionId1 = new MatchActionId(1L);
        final MatchAction action1 = new MatchAction(matchActionId1, null, null, null);

        final MatchActionOperationEntry entry1 =
                new MatchActionOperationEntry(MatchActionOperations.Operator.ADD, action1);

        operations.addOperation(entry1);

        //  Query the Operations entry list and check that the returned list is correct
        final List<MatchActionOperationEntry> opList = operations.getOperations();
        assertThat(opList, is(notNullValue()));
        assertThat(opList, hasSize(1));
        assertThat(opList.size(), is(equalTo(operations.size())));

        //  Check that the MatchAction was persisted correctly
        final MatchActionOperationEntry loadedEntry1 = opList.get(0);
        assertThat(loadedEntry1, is(notNullValue()));

        final MatchAction loadedAction1 = loadedEntry1.getTarget();
        assertThat(loadedAction1.getId().toString(),
                   is(equalTo(matchActionId1.toString())));

        final MatchActionOperations.Operator loadedOperator1 = loadedEntry1.getOperator();
        assertThat(loadedOperator1, is(equalTo(MatchActionOperations.Operator.ADD)));
    }
}
