package net.onrc.onos.api.batchoperation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test for AddOperation class.
 */
public class AddOperationTest {

    /**
     * Creates the AddOperatoin instance and checks if the getTarget() method
     * returns the object which was specified with construcutor's parameter and
     * the getOperator() method returns BatchOperator.ADD.
     */
    @Test
    public void testConstructor() {
        StringOperationTargetId id = new StringOperationTargetId("test");
        StringOperationTarget target = new StringOperationTarget(id);
        AddOperation<StringOperationTarget> op =
                new AddOperation<StringOperationTarget>(target);

        StringOperationTarget testTarget = op.getTarget();
        assertEquals(target, testTarget);
        assertEquals(id, testTarget.getId());
        assertEquals("test", testTarget.getId().toString());

        assertEquals(BatchOperator.ADD, op.getOperator());
    }
}
