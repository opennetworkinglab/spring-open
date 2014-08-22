package net.onrc.onos.core.newintent;

import com.google.common.collect.ImmutableList;
import net.onrc.onos.api.newintent.InstallableIntent;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class representing a result of intent compilation.
 */
public class IntentCompilationResult {
    private final List<InstallableIntent> result;

    /**
     * Constructs an instance containing the specified list of installable intents.
     *
     * @param result installable intents
     */
    public IntentCompilationResult(List<InstallableIntent> result) {
        checkNotNull(result);
        checkArgument(result.size() > 0,
                "result of intent compilation should be " +
                "at least one installable intent, but %s", result.size());

        this.result = new ArrayList<>(checkNotNull(result));
    }

    /**
     * Constructor for serializer.
     */
    protected IntentCompilationResult() {
        this.result = null;
    }

    /**
     * Returns list of installable intents that this object contains.
     *
     * @return installable intents
     */
    public List<InstallableIntent> getResult() {
        return ImmutableList.copyOf(result);
    }
}
