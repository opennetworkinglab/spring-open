package net.onrc.onos.core.matchaction;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.easymock.EasyMock.createNiceMock;

/**
 * Unit tests for the MatchActionModule.
 */
public class MatchActionModuleTest {

    private IDatagridService datagridService;
    private FloodlightModuleContext modContext;

    /**
     * Sets up the mocks used by the test.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUpMocks() {
        final IEventChannel<String, MatchActionOperations> installSetChannel =
                createMock(IEventChannel.class);
        final IEventChannel<String, SwitchResultList> installSetReplyChannel =
                createMock(IEventChannel.class);

        datagridService = createNiceMock(IDatagridService.class);
        modContext = createMock(FloodlightModuleContext.class);

        expect(modContext.getServiceImpl(IDatagridService.class))
                .andReturn(datagridService).once();

        expect(datagridService.createChannel("onos.matchaction.installSetChannel",
                String.class,
                MatchActionOperations.class))
                .andReturn(installSetChannel).once();

        expect(datagridService.addListener(
                eq("onos.matchaction.installSetChannel"),
                anyObject(IEventChannelListener.class),
                eq(String.class),
                eq(MatchActionOperations.class)))
                .andReturn(installSetChannel).once();

        expect(datagridService.createChannel("onos.matchaction.installSetReplyChannel",
                String.class,
                SwitchResultList.class))
                .andReturn(installSetReplyChannel).once();

        expect(datagridService.addListener(
                eq("onos.matchaction.installSetReplyChannel"),
                anyObject(IEventChannelListener.class),
                eq(String.class),
                eq(SwitchResultList.class)))
                .andReturn(installSetReplyChannel).once();

        replay(datagridService);
    }

    /**
     * Tests that MatchAction objects added by the executeOperations()
     * method are properly returned by the getMatchActions() method.
     */
    //@Test
    public void testMatchActionModuleGlobalEntriesSet() {

        final int iterations = 5;
        final MatchActionComponent matchActionComponent =
            new MatchActionComponent(datagridService, null, null, null);
        final ArrayList<MatchAction> generatedMatchActions = new ArrayList<>();

        // Add some test MatchAction objects. 25 will be added, in 5 blocks
        // of 5.
        for (int operationsIteration = 1;
             operationsIteration <= iterations;
             operationsIteration++) {
            final MatchActionOperationsId id =
                    new MatchActionOperationsId(1L);
            assertThat(id, is(notNullValue()));
            final MatchActionOperations operations =
                    new MatchActionOperations(id);
            assertThat(operations, is(notNullValue()));

            for (int entriesIteration = 1;
                 entriesIteration <= iterations;
                 entriesIteration++) {

                final MatchActionId entryId =
                        new MatchActionId(
                        (operationsIteration * 10) +
                        entriesIteration);
                final MatchAction matchAction =
                        new MatchAction(entryId, null, null, null);
                final MatchActionOperationEntry entry =
                        new MatchActionOperationEntry(MatchActionOperations.Operator.ADD,
                                                      matchAction);
                operations.addOperation(entry);
                generatedMatchActions.add(matchAction);
            }

            // Add the MatchActions generated by this iteration
            final boolean result = matchActionComponent.executeOperations(operations);
            assertThat(result, is(true));
        }

        // Get the list of generated MatchAction objects and make sure its
        // length is correct.
        final int generatedCount = generatedMatchActions.size();
        final Set<MatchAction> matchActions = matchActionComponent.getMatchActions();
        assertThat(matchActions, hasSize(generatedCount));

        // Make sure that all the created items are in the list
        final MatchAction[] generatedArray =
                generatedMatchActions.toArray(new MatchAction[generatedCount]);
        assertThat(matchActions, containsInAnyOrder(generatedArray));

        //  Make sure that the returned list cannot be modified
        Throwable errorThrown = null;
        try {
            matchActions.add(new MatchAction(new MatchActionId(1L), null, null, null));
        } catch (UnsupportedOperationException e) {
            errorThrown = e;
        }
        assertThat(errorThrown, is(notNullValue()));
    }

    /**
     * Tests that adding a duplicate MatchAction via executeOperations()
     * returns an error.
     */
    //@Test
    public void testAddingDuplicateMatchAction() {

        // Create two MatchAction objects using the same ID
        final MatchAction matchAction =
                new MatchAction(new MatchActionId(111L), null, null, null);
        final MatchAction duplicateMatchAction =
                new MatchAction(new MatchActionId(111L), null, null, null);

        // create Operation Entries for the two MatchAction objects
        final MatchActionOperationEntry entry =
                new MatchActionOperationEntry(MatchActionOperations.Operator.ADD,
                        matchAction);
        final MatchActionOperationEntry duplicateEntry =
                new MatchActionOperationEntry(MatchActionOperations.Operator.ADD,
                        duplicateMatchAction);

        // Create an Operations object to execute the first MatchAction
        final MatchActionOperationsId id =
                new MatchActionOperationsId(11L);
        assertThat(id, is(notNullValue()));
        final MatchActionOperations operations =
                new MatchActionOperations(id);
        operations.addOperation(entry);

        // Create a module to use to execute the Operations.
        final MatchActionComponent matchActionComponent = new MatchActionComponent(null, null, null, null);

        // Execute the first set of Operations.  This
        // should succeed.
        final boolean result = matchActionComponent.executeOperations(operations);
        assertThat(result, is(true));

        // Now add the duplicate entry.  This should fail.
        final MatchActionOperationsId idForDuplicate =
                new MatchActionOperationsId(22L);
        assertThat(idForDuplicate, is(notNullValue()));
        final MatchActionOperations operationsForDuplicate =
                new MatchActionOperations(idForDuplicate);
        operationsForDuplicate.addOperation(duplicateEntry);

        final boolean resultForDuplicate =
                matchActionComponent.executeOperations(operationsForDuplicate);
        assertThat(resultForDuplicate, is(false));

        // Now add the original entry again.  This should fail.
        final boolean resultForAddAgain = matchActionComponent.executeOperations(operations);
        assertThat(resultForAddAgain, is(false));
    }
}
