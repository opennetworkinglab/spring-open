package net.onrc.onos.api.newintent;

/**
 * Abstraction of entity capable of installing intents to the environment.
 */
public interface IntentInstaller<T extends InstallableIntent> {
    /**
     * Installs the specified intent to the environment.
     *
     * @param intent intent to be installed
     * @throws IntentException if issues are encountered while installing the intent
     */
    void install(T intent);

    /**
     * Removes the specified intent from the environment.
     *
     * @param intent intent to be removed
     * @throws IntentException if issues are encountered while removing the intent
     */
    void remove(T intent); // TODO: consider calling this uninstall for symmetry
}
