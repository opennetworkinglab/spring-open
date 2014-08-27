package net.onrc.onos.core.newintent;

import net.onrc.onos.api.newintent.ConnectivityIntent;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentCompiler;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.core.matchaction.action.Action;
import net.onrc.onos.core.matchaction.action.Actions;
import net.onrc.onos.core.matchaction.action.OutputAction;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.SwitchPort;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base IntentCompiler implementation.
 * @param <T> the type of intent
 */
public abstract class AbstractIntentCompiler<T extends Intent> implements IntentCompiler<T> {
    private final IdGenerator<IntentId> idGenerator;

    /**
     * Constructs an instance with the specified Intent ID generator.
     * <p>
     * Intent compiler generates intents from an input intent.
     * To make sure to use unique IDs for generated intents, intent
     * ID generator is given as the argument of a constructor in normal
     * cases.
     * </p>
     * @param idGenerator intent ID generator
     */
    protected AbstractIntentCompiler(IdGenerator<IntentId> idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    protected IntentId getNextId() {
        return idGenerator.getNewId();
    }

    protected List<Action> packActions(ConnectivityIntent intent, SwitchPort egress) {
        List<Action> actions = new ArrayList<>();
        Action intentAction = intent.getAction();
        if (!intentAction.equals(Actions.nullAction())) {
            actions.add(intentAction);
        }

        OutputAction output = new OutputAction(egress.getPortNumber());
        actions.add(output);
        return actions;
    }
}
