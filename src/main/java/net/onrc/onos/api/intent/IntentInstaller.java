package net.onrc.onos.api.intent;

/**
 * An interface to handle installation and removal of a specific type of intent.
 *
 * @param <T> the type of intent this installer handles.
 */
public interface IntentInstaller<T extends Intent> {
    /**
     * Installs the given intent.
     *
     * @param intent the intent to be installed.
     * @return true if the installation succeeds. Otherwise, false.
     */
    public boolean install(T intent);

    /**
     * Removes the given intent.
     *
     * @param intent the intent to be removed.
     * @return true if the removal succeeds. Otherwise, false.
     */
    public boolean remove(T intent);
}
