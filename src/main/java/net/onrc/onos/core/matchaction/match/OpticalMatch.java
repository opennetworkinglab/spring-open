package net.onrc.onos.core.matchaction.match;

/**
 * A match object (traffic specifier) for optical nodes, flow-paths and intents.
 * <p>
 * This class does not have a switch ID and a port number. They are handled by
 * MatchAction, Flow or Intent class.
 */
public class OpticalMatch implements Match {

    // Match fields
    protected Integer srcLambda;

    /**
     * Constructor.
     */
    public OpticalMatch() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param srcLambda The source lambda. Null means the wild-card for the
     *        lambda.
     */
    public OpticalMatch(Integer srcLambda) {
        this.srcLambda = srcLambda;
    }

    /**
     * Gets the source lambda.
     *
     * @return The source lambda, or null if it was wild-card.
     */
    public Integer getSrcLambda() {
        return srcLambda;
    }
}
