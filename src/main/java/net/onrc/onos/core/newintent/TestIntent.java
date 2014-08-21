package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.AbstractIntent;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.IntentId;

/**
 * A intent used in the unit test.
 *
 * FIXME: we don't want to expose this class publicly, but the current Kryo
 * serialization mechanism does not allow this class to be private and placed
 * on testing directory.
 */
public class TestIntent extends AbstractIntent implements InstallableIntent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestIntent(IntentId id) {
        super(id);
    }

    /**
     * Constructor for serializer.
     */
    protected TestIntent() {
        super();
    }
}
