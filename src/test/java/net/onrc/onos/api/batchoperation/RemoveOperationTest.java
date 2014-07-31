package net.onrc.onos.api.batchoperation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test for RemoveOperation class.
 */
public class RemoveOperationTest {

    /**
     * Creates the RemoveOperation instance and checks if the getTargetId()
     * method returns the object which was specified with construcutor's
     * parameter and the getOperator() method returns BatchOperator.REMOVE.
     */
    @Test
    public void testConstructor() {
        StringOperationTargetId id = new StringOperationTargetId("test");
        RemoveOperation<StringOperationTarget> op =
                new RemoveOperation<StringOperationTarget>(id);

        BatchOperationTargetId testTargetId = op.getTargetId();
        assertEquals(id, testTargetId);
        assertEquals("test", testTargetId.toString());

        assertEquals(BatchOperator.REMOVE, op.getOperator());
    }
}
