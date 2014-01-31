package me.tatarka.ipromise;

import me.tatarka.ipromise.compat.Objects;

public class Pair<A, B> {
    public final A first;
    public final B second;

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<A, B>(first, second);
    }

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
