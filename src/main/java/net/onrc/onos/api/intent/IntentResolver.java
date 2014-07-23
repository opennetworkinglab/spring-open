package net.onrc.onos.api.intent;

import java.util.List;

/**
 * An interface to translate an intent to lower level intents.
 *
 * IntentResolvers are registered to {@link IIntentRuntimeService} to handle translations of intents
 * (we call it intent resolution).
 *
 * @param <T> the type of intent this resolver translates
 */
public interface IntentResolver<T extends Intent> {
    /**
     * Returns lower level intents, into which the given intent is translated.
     *
     * This method is invoked by the Intent Framework when the framework find an unresolved intent.
     *
     * @param intent the intent to be translated.
     * @return lower level intents, into which the given intent is translated.
     * @throws IntentResolutionException if this method can't resolve the intent.
     */
    public List<Intent> resolve(T intent) throws IntentResolutionException;
}
