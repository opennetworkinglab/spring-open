package net.onrc.onos.core.intent;

import java.util.LinkedList;

/**
 * This class is simply a list of IntentOperations. It exists so
 * that IntentOperations can be serialized for notifications and persistence.
 */
public class IntentOperationList extends LinkedList<IntentOperation> {
    private static final long serialVersionUID = -3894081461861052610L;

    /**
     * Add an operator and Intent to the list.
     *
     * @param op operator for the Intent
     * @param intent the Intent
     * @return true
     */
    public boolean add(IntentOperation.Operator op, Intent intent) {
        return add(new IntentOperation(op, intent));
    }
}
