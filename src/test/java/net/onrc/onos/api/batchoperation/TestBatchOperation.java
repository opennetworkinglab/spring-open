package net.onrc.onos.api.batchoperation;

public class TestBatchOperation extends
        BatchOperation<BatchOperationEntry<TestBatchOperation.Operator, ?>> {

    public enum Operator {
        STRING,
        INTEGER,
    }

    public static class StringTarget implements IBatchOperationTarget {
        private String string;

        public StringTarget(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }
    }

    public static class IntegerTarget implements IBatchOperationTarget {
        private Integer integer;

        public IntegerTarget(Integer integer) {
            this.integer = integer;
        }

        public Integer getInteger() {
            return integer;
        }
    }

    public TestBatchOperation addStringOperation(String string) {
        return (super.addOperation(new BatchOperationEntry<Operator, StringTarget>(
                Operator.STRING, new StringTarget(string))) == null)
                ? null : this;
    }

    public TestBatchOperation addIntegerOperation(Integer integer) {
        return (super.addOperation(new BatchOperationEntry<Operator, IntegerTarget>(
                Operator.INTEGER, new IntegerTarget(integer))) == null)
                ? null : this;
    }
}
