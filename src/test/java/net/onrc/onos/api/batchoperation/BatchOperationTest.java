package net.onrc.onos.api.batchoperation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.onrc.onos.api.batchoperation.TestBatchOperation.IntegerTarget;
import net.onrc.onos.api.batchoperation.TestBatchOperation.Operator;
import net.onrc.onos.api.batchoperation.TestBatchOperation.StringTarget;

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
        TestBatchOperation ops = new TestBatchOperation();

        // Checks if there is no operation in the object
        assertEquals(0, ops.size());
        assertEquals(0, ops.getOperations().size());
    }

    /**
     * Tests adding the operations to the BatchOperation.
     */
    @Test
    public void testAddOp() {
        // Initialization
        TestBatchOperation ops = new TestBatchOperation();
        assertEquals(0, ops.size());

        // Adds one string-operation
        ops.addStringOperation("target1");
        assertEquals(1, ops.size());

        // Adds one integer-operation
        ops.addIntegerOperation(123);
        assertEquals(2, ops.size());

        // Checks entries with the list
        List<BatchOperationEntry<Operator, ?>> list = ops.getOperations();
        assertEquals(2, list.size());

        BatchOperationEntry<Operator, ?> entry = list.get(0);
        assertEquals(Operator.STRING, entry.getOperator());
        BatchOperationTarget target = entry.getTarget();
        assertTrue(target instanceof StringTarget);
        assertEquals("target1", ((StringTarget) target).getString());

        entry = list.get(1);
        assertEquals(Operator.INTEGER, entry.getOperator());
        target = entry.getTarget();
        assertTrue(target instanceof IntegerTarget);
        assertEquals(Integer.valueOf(123), ((IntegerTarget) target).getInteger());
    }
}
