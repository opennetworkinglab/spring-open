package net.onrc.onos.intent;

import java.util.ArrayList;

public class IntentOperationList extends ArrayList<IntentOperation> {
	private static final long serialVersionUID = -3894081461861052610L;
	
	public boolean add(IntentOperation.Operator op, Intent intent) {
		return add(new IntentOperation(op, intent));
	}
}