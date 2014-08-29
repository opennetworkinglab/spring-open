package net.onrc.onos.api.flowmanager;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;

import net.onrc.onos.api.batchoperation.BatchOperationEntry;
import net.onrc.onos.api.flowmanager.FlowBatchOperation.Operator;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsId;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.util.IdGenerator;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link FlowBatchOperation}.
 */
public class FlowBatchOperationTest {
    private Flow flow1;
    private Flow flow2;
    private Flow flow3;

    /**
     * A subclass of {@link Flow} for testing purpose.
     */
    final class TestFlow extends Flow {
        public TestFlow(FlowId id) {
            super(id);
        }

        @Override
        public Match getMatch() {
            return null;
        }

        @Override
        public List<MatchActionOperations> compile(Operator op,
                IdGenerator<MatchActionId> maIdGenerator,
                IdGenerator<MatchActionOperationsId> maoIdGenerator) {
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        flow1 = new TestFlow(new FlowId(123L));
        flow2 = new TestFlow(new FlowId(456L));
        flow3 = new TestFlow(new FlowId(789L));
    }

    /**
     * Tests {@link FlowBatchOperation#FlowBatchOperation()} constructor.
     */
    @Test
    public void testConstructor() {
        FlowBatchOperation op1 = new FlowBatchOperation();
        assertNotNull(op1);
        assertEquals(0, op1.size());
    }

    /**
     * Tests {@link FlowBatchOperation#FlowBatchOperation(java.util.List)}
     * constructor.
     */
    @Test
    public void testConstructorWithList() {
        List<BatchOperationEntry<Operator, ?>> batchOperations;
        batchOperations = new LinkedList<>();
        batchOperations.add(new BatchOperationEntry<Operator, Flow>(
                Operator.ADD, flow1));
        batchOperations.add(new BatchOperationEntry<Operator, Flow>(
                Operator.ADD, flow2));
        batchOperations.add(new BatchOperationEntry<Operator, Flow>(
                Operator.ADD, flow3));
        batchOperations.add(new BatchOperationEntry<Operator, FlowId>(
                Operator.REMOVE, new FlowId(1L)));
        batchOperations.add(new BatchOperationEntry<Operator, FlowId>(
                Operator.REMOVE, new FlowId(2L)));
        batchOperations.add(new BatchOperationEntry<Operator, FlowId>(
                Operator.REMOVE, new FlowId(3L)));

        FlowBatchOperation op1 = new FlowBatchOperation(batchOperations);

        assertNotNull(op1);
        assertEquals(6, op1.size());
        assertThat(op1.getOperations(), hasSize(6));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow1)));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow2)));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow3)));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(Operator.REMOVE,
                        new FlowId(1L))));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(Operator.REMOVE,
                        new FlowId(2L))));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(Operator.REMOVE,
                        new FlowId(3L))));
    }

    /**
     * Tests {@link FlowBatchOperation#addAddFlowOperation(Flow)} method.
     */
    @Test
    public void testAddAddFlowOperation() {
        FlowBatchOperation op1 = new FlowBatchOperation();

        FlowBatchOperation op2 = op1.addAddFlowOperation(flow1);

        assertEquals(op1, op2);
        assertEquals(1, op1.size());
        assertThat(op1.getOperations(), hasSize(1));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow1)));

        op1.addAddFlowOperation(flow2).addAddFlowOperation(flow3);

        assertEquals(3, op1.size());
        assertThat(op1.getOperations(), hasSize(3));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow1)));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow2)));
        assertThat(op1.getOperations(), hasItem(
                new BatchOperationEntry<Operator, Flow>(Operator.ADD, flow3)));
    }

    /**
     * Tests {@link FlowBatchOperation#addRemoveFlowOperation(Flow)} method.
     */
    @Test
    public void testAddRemoveFlowOperation() {
        FlowBatchOperation op1 = new FlowBatchOperation();

        FlowBatchOperation op2 = op1.addRemoveFlowOperation(new FlowId(123L));

        assertEquals(op1, op2);
        assertEquals(1, op1.size());
        assertThat(op1.getOperations(), hasSize(1));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(
                        Operator.REMOVE, new FlowId(123L))));

        op1.addRemoveFlowOperation(new FlowId(456L))
                .addRemoveFlowOperation(new FlowId(789L));

        assertEquals(3, op1.size());
        assertThat(op1.getOperations(), hasSize(3));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(
                        Operator.REMOVE, new FlowId(123L))));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(
                        Operator.REMOVE, new FlowId(456L))));
        assertThat(op1.getOperations(),
                hasItem(new BatchOperationEntry<Operator, FlowId>(
                        Operator.REMOVE, new FlowId(789L))));
    }
}
