package net.onrc.onos.core.newintent;

import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import net.onrc.onos.api.newintent.InstallableIntent;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentCompiler;
import net.onrc.onos.api.newintent.IntentEvent;
import net.onrc.onos.api.newintent.IntentEventListener;
import net.onrc.onos.api.newintent.IntentException;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.IntentInstaller;
import net.onrc.onos.api.newintent.IntentOperations;
import net.onrc.onos.api.newintent.IntentState;
import net.onrc.onos.core.datagrid.ISharedCollectionsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.onrc.onos.api.newintent.IntentState.COMPILED;
import static net.onrc.onos.api.newintent.IntentState.FAILED;
import static net.onrc.onos.api.newintent.IntentState.INSTALLED;
import static net.onrc.onos.api.newintent.IntentState.SUBMITTED;
import static net.onrc.onos.api.newintent.IntentState.WITHDRAWING;
import static net.onrc.onos.api.newintent.IntentState.WITHDRAWN;

/**
 * An implementation of Intent Manager.
 */
public class IntentManagerRuntime implements IntentManager {
    // Collections for intent, installable intent, and intent state are globally shared
    private final IntentMap<IntentEvent> intentEvents;
    private final IntentMap<IntentCompilationResult> installableIntents;

    // Collections for compiler, installer, and listener are ONOS instance local
    private final ConcurrentMap<Class<? extends Intent>,
            IntentCompiler<? extends Intent>> compilers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends InstallableIntent>,
            IntentInstaller<? extends InstallableIntent>> installers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<IntentEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a Intent Manager runtime with the specified shared collections service.
     *
     * @param collectionsService shared collections service
     */
    public IntentManagerRuntime(ISharedCollectionsService collectionsService) {
        checkNotNull(collectionsService);

        this.intentEvents = new IntentMap<>("intentState", IntentEvent.class, collectionsService);
        this.installableIntents =
                new IntentMap<>("installableIntents", IntentCompilationResult.class, collectionsService);

        this.intentEvents.addListener(new InternalEntryListener(new InternalIntentEventListener()));
    }

    @Override
    public void submit(Intent intent) {
        registerSubclassCompilerIfNeeded(intent);
        setState(intent, SUBMITTED);
    }

    @Override
    public void withdraw(Intent intent) {
        setState(intent, WITHDRAWING);
    }

    // FIXME: implement this method
    @Override
    public void execute(IntentOperations operations) {
        throw new UnsupportedOperationException("execute() is not implemented yet");
    }

    @Override
    public Set<Intent> getIntents() {
        Collection<IntentEvent> events = intentEvents.values();
        Set<Intent> intents = new HashSet<>(events.size());
        for (IntentEvent event: events) {
            intents.add(event.getIntent());
        }
        return intents;
    }

    @Override
    public Intent getIntent(IntentId id) {
        IntentEvent event = intentEvents.get(id);
        if (event == null) {
            return null;
        }
        return event.getIntent();
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        IntentEvent event = intentEvents.get(id);
        if (event == null) {
            return null;
        }
        return event.getState();
    }

    @Override
    public void addListener(IntentEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IntentEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler) {
        compilers.put(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilers.remove(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return ImmutableMap.copyOf(compilers);
    }

    @Override
    public <T extends InstallableIntent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installers.put(cls, installer);
    }

    @Override
    public <T extends InstallableIntent> void unregisterInstaller(Class<T> cls) {
        installers.remove(cls);
    }

    @Override
    public Map<Class<? extends InstallableIntent>, IntentInstaller<? extends InstallableIntent>> getInstallers() {
        return ImmutableMap.copyOf(installers);
    }

    /**
     * Sets the state of the specified intent to the new state.
     *
     * @param intent intent whose state is to be changed
     * @param newState new state
     */
    private void setState(Intent intent, IntentState newState) {
        IntentState oldState = getIntentState(intent.getId());
        IntentEvent event = new IntentEvent(intent, newState, oldState, System.currentTimeMillis());
        intentEvents.put(intent.getId(), event);
    }

    /**
     * Invokes all of registered intent event listener.
     *
     * @param event event supplied to a listener as an argument
     */
    private void invokeListeners(IntentEvent event) {
        for (IntentEventListener listener: listeners) {
            listener.event(event);
        }
    }

    /**
     * Returns the corresponding intent compiler to the specified intent.
     *
     * @param intent intent
     * @param <T> the type of intent
     * @return intent compiler corresponding to the specified intent
     */
    private <T extends Intent> IntentCompiler<T> getCompiler(T intent) {
        @SuppressWarnings("unchecked")
        IntentCompiler<T> compiler = (IntentCompiler<T>) compilers.get(intent.getClass());
        if (compiler == null) {
            throw new IntentException("no compiler for class " + intent.getClass());
        }
        return compiler;
    }

    /**
     * Returns the corresponding intent installer to the specified installable intent.
     * @param intent intent
     * @param <T> the type of installable intent
     * @return intent installer corresponding to the specified installable intent
     */
    private <T extends InstallableIntent> IntentInstaller<T> getInstaller(T intent) {
        @SuppressWarnings("unchecked")
        IntentInstaller<T> installer = (IntentInstaller<T>) installers.get(intent.getClass());
        if (installer == null) {
            throw new IntentException("no installer for class " + intent.getClass());
        }
        return installer;
    }

    /**
     * Compiles an intent.
     *
     * @param intent intent
     */
    private void compileIntent(Intent intent) {
        // FIXME: To make SDN-IP workable ASAP, only single level compilation is implemented
        // TODO: implement compilation traversing tree structure
        List<InstallableIntent> installable = new ArrayList<>();
        for (Intent compiled : getCompiler(intent).compile(intent)) {
            installable.add((InstallableIntent) compiled);
        }
        installableIntents.put(intent.getId(), new IntentCompilationResult(installable));
        setState(intent, COMPILED);
    }

    /**
     * Installs an intent.
     *
     * @param intent intent
     */
    private void installIntent(Intent intent) {
        IntentCompilationResult compiled = installableIntents.get(intent.getId());
        for (InstallableIntent installable: compiled.getResult()) {
            registerSubclassInstallerIfNeeded(installable);
            getInstaller(installable).install(installable);
        }

        setState(intent, INSTALLED);
    }

    /**
     * Uninstalls an intent.
     *
     * @param intent intent
     */
    private void uninstallIntent(Intent intent) {
        IntentCompilationResult compiled = installableIntents.get(intent.getId());
        for (InstallableIntent installable: compiled.getResult()) {
            getInstaller(installable).remove(installable);
        }

        installableIntents.remove(intent.getId());
        setState(intent, WITHDRAWN);
    }

    /**
     * Registers an intent compiler of the specified intent if an intent compiler
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent compiler for a parent type is found, this method
     * registers the found intent compiler.
     *
     * @param intent intent
     */
    @SuppressWarnings("unchecked")
    private void registerSubclassCompilerIfNeeded(Intent intent) {
        if (!compilers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    IntentCompiler<?> compiler = compilers.get(cls);
                    if (compiler != null) {
                        compilers.put(intent.getClass(), compiler);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    /**
     * Registers an intent installer of the specified intent if an intent installer
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent installer for a parent type is found, this method
     * registers the found intent installer.
     *
     * @param intent intent
     */
    @SuppressWarnings("unchecked")
    private void registerSubclassInstallerIfNeeded(InstallableIntent intent) {
        if (!installers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the InstallableIntent class descendants
                if (InstallableIntent.class.isAssignableFrom(cls)) {
                    IntentInstaller<?> installer = installers.get(cls);
                    if (installer != null) {
                        installers.put(intent.getClass(), installer);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    /**
     * Destroys underlying {@link IntentMap IntentMaps}.
     * This method is only for testing purpose.
     */
    void destroy() {
        intentEvents.destroy();
        installableIntents.destroy();
    }

    /**
     * An entry listener used internally.
     *
     * This listener is a kind of bridge of listener mechanism
     * between {@link IntentMap} and {@link IntentEventListener}.
     */
    private static class InternalEntryListener implements EntryListener<IntentId, IntentEvent> {
        private final IntentEventListener listener;

        public InternalEntryListener(IntentEventListener listener) {
            this.listener = listener;
        }

        @Override
        public void entryAdded(EntryEvent<IntentId, IntentEvent> event) {
            listener.event(event.getValue());
        }

        @Override
        public void entryRemoved(EntryEvent<IntentId, IntentEvent> event) {
            listener.event(event.getValue());
        }

        @Override
        public void entryUpdated(EntryEvent<IntentId, IntentEvent> event) {
            listener.event(event.getValue());
        }

        @Override
        public void entryEvicted(EntryEvent<IntentId, IntentEvent> event) {
            // no-op
        }
    }

    /**
     * An intent event listener used internally.
     *
     * event() method handles state transition of submitted intents.
     */
    private class InternalIntentEventListener implements IntentEventListener {
        @Override
        public void event(IntentEvent event) {
            invokeListeners(event);
            Intent intent = event.getIntent();

            try {
                switch (event.getState()) {
                    case SUBMITTED:
                        compileIntent(intent);
                        break;
                    case COMPILED:
                        installIntent(intent);
                        break;
                    case INSTALLED:
                        break;
                    case WITHDRAWING:
                        uninstallIntent(intent);
                        break;
                    case WITHDRAWN:
                        break;
                    case FAILED:
                        break;
                    default:
                        throw new IllegalStateException(
                                "the state of IntentEvent is illegal: " + event.getState());
                }
            } catch (IntentException e) {
                setState(intent, FAILED);
            }
        }
    }
}
