package net.onrc.onos.core.matchaction.action;

/**
 * An action object to modify lambda.
 * <p>
 * This class does not have a switch ID. The switch ID is handled by
 * MatchAction, IFlow or Intent class.
 */
public class ModifyLambdaAction implements Action {
    protected int lambda;

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

}
