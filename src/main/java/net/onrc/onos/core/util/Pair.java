package net.onrc.onos.core.util;

import java.util.Objects;

/**
 * A generic class representing a pair of two values.
 */
public class Pair<F, S> {
    public F first;        // The first value in the pair
    public S second;        // The second value in the pair

    /**
     * Constructor for a pair of two values.
     *
     * @param first  the first value in the pair.
     * @param second the second value in the pair.
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Pair)) {
            return false;
        }

        Pair<?, ?> that = (Pair<?, ?>) o;
        return Objects.equals(this.first, that.first)
                && Objects.equals(this.second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.first, this.second);
    }
}
