package net.onrc.onos.api.newintent;

import java.util.List;

/**
 * Abstraction of an extensible intent service enabled for unit tests.
 */
public interface TestableIntentService extends IntentManager {

    List<IntentException> getExceptions();

}
