package me.tatarka.ipromise;

import java.util.HashSet;
import java.util.Set;

import me.tatarka.ipromise.compat.Objects;

public final class Filters {
    private Filters() {}

    public static <T> Filter<T> unique() {
        return new Filter<T>() {
            private Set<T> seenItems = new HashSet<T>();

            @Override
            public boolean filter(T item) {
                return seenItems.add(item);
            }
        };
    }

    public static <T> Filter<T> dedup() {
        return new Filter<T>() {
            private T lastItem;

            @Override
            public boolean filter(T item) {
                boolean isDuplicate = Objects.equals(lastItem, item);
                lastItem = item;
                return !isDuplicate;
            }
        };
    }

    public static <T> Filter<T> rateLimit(final int rate) {
        return new Filter<T>() {
            private long lastTime = -1;

            @Override
            public boolean filter(T item) {
                if (lastTime < 0) {
                    lastTime = System.nanoTime();
                    return true;
                } else {
                    long now = System.nanoTime();
                    long elapsed = now - lastTime;
                    if (elapsed >= rate)  {
                        lastTime = now;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    public static <T> Filter<T> equal(final T value) {
        return new Filter<T>() {
            @Override
            public boolean filter(T item) {
                return Objects.equals(value, item);
            }
        };
    }

    public static <T> Filter<T> not(final Filter<T> filter) {
        return new Filter<T>() {
            @Override
            public boolean filter(T item) {
                return !filter.filter(item);
            }
        };
    }
}
