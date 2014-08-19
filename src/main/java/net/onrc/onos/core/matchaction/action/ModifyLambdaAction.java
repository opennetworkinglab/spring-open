package net.onrc.onos.core.matchaction.action;

import java.util.Objects;

/**
 * An action object to modify lambda.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, Flow or Intent class.
 */
public class ModifyLambdaAction implements Action {
    private final int lambda;

    /**
     * Constructor.
     *
     * @param lambda lambda after modification
     */
    public ModifyLambdaAction(int lambda) {
        this.lambda = lambda;
    }

    /**
     * Gets the lambda.
     *
     * @return The lambda.
     */
    public int getLambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lambda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModifyLambdaAction other = (ModifyLambdaAction) obj;
        return lambda == other.lambda;
    }

}
