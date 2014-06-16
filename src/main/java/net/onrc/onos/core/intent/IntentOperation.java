package net.onrc.onos.core.intent;

/**
 * This class is merely an Intent and an associated operation.
 * <p>
 * It exists so that the pair can be serialized for notifications and persistence.
 */
public class IntentOperation {
    public enum Operator {
        /**
         * Add new intent specified by intent field.
         */
        ADD,

        /**
         * Remove existing intent specified by intent field.
         * The instance of intent field should be an instance of Intent class (not a child class)
         */
        REMOVE,

        /**
         * Do error handling.
         * The instance of intent field should be an instance of ErrorIntent
         */
        ERROR,
    }

    public Operator operator;
    public Intent intent;

    /**
     * Default Constructor.
     */
    protected IntentOperation() {
    }

    /**
     * Constructor.
     *
     * @param operator the operation to perform for this Intent
     * @param intent the Intent
     */
    public IntentOperation(Operator operator, Intent intent) {
        this.operator = operator;
        this.intent = intent;
    }

    /**
     * Returns a string representation of the operation and Intent.
     *
     * @return "operator, (Intent ID)"
     */
    @Override
    public String toString() {
        return operator.toString() + ", (" + intent.toString() + ")";
    }
}
