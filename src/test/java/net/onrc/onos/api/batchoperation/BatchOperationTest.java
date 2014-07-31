package net.onrc.onos.api.batchoperation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for BatchOperation class.
 */
public class BatchOperationTest {

    /**
     * Tests constructor of the BatchOperation. It checks the size of the
     * operations equals to 0.
     */
    @Test
    public void testConstructor() {
        // Initialization
        BatchOperation<StringOperationTarget> op = new BatchOperation<StringOperationTarget>();

        // Checks if there is no operation in the object
        assertEquals(0, op.size());
        assertEquals(new LinkedList<BatchOperationEntry<StringOperationTarget>>(),
                op.getOperations());
        Iterator<BatchOperationEntry<StringOperationTarget>> i = op.iterator();
        assertFalse(i.hasNext());
    }

    /**
     * Tests adding the add-operation to the BatchOperation. It adds an
     * add-operation to the BatchOperation and checks the list of operations
     * maintained by the BatchOperation.
     */
    @Test
    public void testAddAddOp() {
        // Initialization
        BatchOperation<StringOperationTarget> op = new BatchOperation<StringOperationTarget>();
        assertEquals(0, op.size());

        // Adds one add-operation
        StringOperationTarget target1 = new StringOperationTarget("target1");
        op.addAddOperation(target1);

        // Checks the size
        assertEquals(1, op.size());

        // Checks the iterator
        Iterator<BatchOperationEntry<StringOperationTarget>> i = op.iterator();
        assertTrue(i.hasNext());
        BatchOperationEntry<StringOperationTarget> entry = i.next();
        assertEquals(BatchOperator.ADD, entry.getOperator());
        assertTrue(entry instanceof AddOperation);
        assertEquals(target1, ((AddOperation<StringOperationTarget>) entry).getTarget());
        assertFalse(i.hasNext());

        // Checks the list
        List<BatchOperationEntry<StringOperationTarget>> list = op.getOperations();
        assertEquals(1, list.size());
        entry = list.get(0);
        assertEquals(BatchOperator.ADD, entry.getOperator());
        assertTrue(entry instanceof AddOperation);
        assertEquals(target1, ((AddOperation<StringOperationTarget>) entry).getTarget());
    }

    /**
     * Tests adding the remove-operation to the BatchOperation. It adds a
     * remove-operation to the BatchOperation and checks the list of operations
     * maintained by the BatchOperation.
     */
    @Test
    public void testAddRemoveOp() {
        // Initialization
        BatchOperation<StringOperationTarget> op = new BatchOperation<StringOperationTarget>();
        assertEquals(0, op.size());

        // Adds one remove-operation
        StringOperationTargetId targetId1 = new StringOperationTargetId("target1");
        op.addRemoveOperation(targetId1);

        // Checks the size
        assertEquals(1, op.size());

        // Checks the iterator
        Iterator<BatchOperationEntry<StringOperationTarget>> i = op.iterator();
        assertTrue(i.hasNext());
        BatchOperationEntry<StringOperationTarget> entry = i.next();
        assertEquals(BatchOperator.REMOVE, entry.getOperator());
        assertTrue(entry instanceof RemoveOperation);
        assertEquals(targetId1,
                ((RemoveOperation<StringOperationTarget>) entry).getTargetId());
        assertFalse(i.hasNext());

        // Checks the list
        List<BatchOperationEntry<StringOperationTarget>> list = op.getOperations();
        assertEquals(1, list.size());
        entry = list.get(0);
        assertEquals(BatchOperator.REMOVE, entry.getOperator());
        assertTrue(entry instanceof RemoveOperation);
        assertEquals(targetId1,
                ((RemoveOperation<StringOperationTarget>) entry).getTargetId());
    }

    /**
     * Tests adding the two types of operations, add/remove-operations, to the
     * BatchOperation. It adds add/remove-operation to the BatchOperation and
     * checks the list of operations maintained by the BatchOperation.
     */
    @Test
    public void testAddMultiOp() {
        // Initialization
        BatchOperation<StringOperationTarget> op = new BatchOperation<StringOperationTarget>();
        assertEquals(0, op.size());

        // Adds one add-operation
        StringOperationTarget target1 = new StringOperationTarget("target1");
        op.addAddOperation(target1);

        // Adds one remove-operation
        StringOperationTargetId targetId2 = new StringOperationTargetId("target2");
        op.addRemoveOperation(targetId2);

        // Checks the size
        assertEquals(2, op.size());

        // Checks the iterator
        Iterator<BatchOperationEntry<StringOperationTarget>> i = op.iterator();
        assertTrue(i.hasNext());
        BatchOperationEntry<StringOperationTarget> entry = i.next();
        assertEquals(BatchOperator.ADD, entry.getOperator());
        assertTrue(entry instanceof AddOperation);
        assertEquals(target1, ((AddOperation<StringOperationTarget>) entry).getTarget());

        assertTrue(i.hasNext());
        entry = i.next();
        assertEquals(BatchOperator.REMOVE, entry.getOperator());
        assertTrue(entry instanceof RemoveOperation);
        assertEquals(targetId2,
                ((RemoveOperation<StringOperationTarget>) entry).getTargetId());

        assertFalse(i.hasNext());

        // Checks the list
        List<BatchOperationEntry<StringOperationTarget>> list = op.getOperations();
        assertEquals(2, list.size());
        entry = list.get(0);
        assertEquals(BatchOperator.ADD, entry.getOperator());
        assertTrue(entry instanceof AddOperation);
        assertEquals(target1, ((AddOperation<StringOperationTarget>) entry).getTarget());

        entry = list.get(1);
        assertEquals(BatchOperator.REMOVE, entry.getOperator());
        assertTrue(entry instanceof RemoveOperation);
        assertEquals(targetId2,
                ((RemoveOperation<StringOperationTarget>) entry).getTargetId());
    }
}
