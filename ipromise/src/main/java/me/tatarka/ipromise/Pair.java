package me.tatarka.ipromise;

import me.tatarka.ipromise.compat.Objects;

/**
 * A pair of items. Properly implements {@link Object#equals(Object)} and {@link Object#hashCode()}
 * based on what it contains.
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @author Evan Tatarka
 */
public class Pair<A, B> {
    public final A first;
    public final B second;

    /**
     * Constructs a pair of items.
     *
     * @param first  the first item
     * @param second the second item
     * @param <A>    the type of the first item
     * @param <B>    the type of the second item
     * @return the new {@code Pair}
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    /**
     * Constructs a pair of items.
     *
     * @param first  the first item
     * @param second the second item
     */
    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Pair)) return false;
        Pair p = (Pair) obj;
        return Objects.equals(first, p.first) && Objects.equals(second, p.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
