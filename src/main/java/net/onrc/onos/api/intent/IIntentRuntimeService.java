package net.onrc.onos.api.intent;

import java.util.Collection;
import java.util.EventListener;

import net.onrc.onos.api.batchoperation.BatchOperation;
import net.onrc.onos.api.flowmanager.ConflictDetectionPolicy;
import net.onrc.onos.api.flowmanager.IFlow;

/**
 * An interface class for the Intent-Runtime Service. The role of the
 * Intent-Runtime Service is to manage a set of IFlow objects based on the
 * specified Intent objects.
 * <p>
 * It compiles accepted Intents to IFlow objects by allocating resources and
 * calculating paths based on the constrains described in the Intents, and
 * executes installation/uninstallation of the IFlow objects using FlowManager
 * Service.
 */
public interface IIntentRuntimeService {
    /**
     * Adds specified intent.
     *
     * @param intent Intent to be added.
     * @return true if succeeded, false otherwise.
     */
    boolean addIntent(Intent intent);

    /**
     * Removes specified intent.
     *
     * @param id ID of the intent to be removed.
     * @return true if succeeded, false otherwise.
     */
    boolean removeIntent(IntentId id);

    /**
     * Overwrites existing intent by new specified intent.
     *
     * @param id ID of the existing intent to be overwritten.
     * @param intent The new intent to be added.
     * @return true if succeeded, false otherwise.
     */
    boolean updateIntent(IntentId id, Intent intent);

    /**
     * Gets specific intent.
     *
     * @param id ID of the intent should be retrieved
     * @return Intent if it exists, null otherwise.
     */
    Intent getIntent(IntentId id);

    /**
     * Gets all intents.
     *
     * @return collection of intents.
     */
    Collection<Intent> getIntents();

    /**
     * Executes batch operation of intents.
     *
     * @param ops BatchOperations to be executed.
     * @return true if succeeded, false otherwise.
     */
    boolean executeBatch(BatchOperation<Intent> ops);

    /**
     * Adds an IntentResolver associated with a given intent type.
     *
     * @param type the class instance of the intent type.
     * @param resolver the resolver of the given intent type.
     * @param <T> the type of the intent.
     */
    public <T extends Intent> void addResolver(Class<T> type, IntentResolver<T> resolver);

    /**
     * Removes the IntentResolver associated with the intent type.
     *
     * @param type the class instance of the intent type.
     * @param <T> the type of the intent.
     */
    public <T extends Intent> void removeResolver(Class<T> type);

    /**
     * Adds an IntentInstaller associated with a given intent type.
     *
     * If there is an Intent instance of the specified Intent type in the runtime,
     * the specified IntentInstaller doesn't replace the existing installer.
     * Otherwise, the existing installer is replaced with the specified installer.
     *
     * @param type the class instance of the intent type.
     * @param installer the installer of the given intent type.
     * @param <T> the type of the intent.
     * @return false when there is an Intent instance of the specified intent type
     * in the runtime. Otherwise, true.
     */
    public <T extends Intent> boolean addInstaller(Class<T> type, IntentInstaller<T> installer);

    /**
     * Removes the IntentInstaller associated with a given intent type.
     *
     * If there is an Intent instance of the specified Intent type in the runtime,
     * the specified IntentInstaller is not removed. Otherwise, the existing
     * IntentInstaller is removed from the runtime.
     *
     * @param type the class instance of the intent type.
     * @param <T> the type of the intent.
     * @return false when there is an Intent instance of the specified intent type
     * in the runtime. Otherwise, true.
     */
    public <T extends Intent> boolean removeInstaller(Class<T> type);

    /**
     * Gets IFlow objects managed by the specified intent.
     *
     * @param intentId ID of the target Intent.
     * @return Collection of IFlow objects if exists, null otherwise.
     */
    Collection<IFlow> getFlows(String intentId);

    /**
     * Gets Intent object which manages the specified IFlow object.
     *
     * @param flowId ID of the target IFlow object.
     * @return Intent which manages the specified IFlow object, null otherwise.
     */
    Intent getIntentByFlow(String flowId);

    /**
     * Sets a conflict detection policy.
     *
     * @param policy ConflictDetectionPolicy object to be set.
     */
    void setConflictDetectionPolicy(ConflictDetectionPolicy policy);

    /**
     * Gets the conflict detection policy.
     *
     * @return ConflictDetectionPolicy object being applied currently.
     */
    ConflictDetectionPolicy getConflictDetectionPolicy();

    /**
     * Adds event listener to this service.
     *
     * @param listener EventListener to be added.
     */
    void addEventListener(EventListener listener);

    /**
     * Removes event listener from this service.
     *
     * @param listener EventListener to be removed.
     */
    void removeEventListener(EventListener listener);
}
