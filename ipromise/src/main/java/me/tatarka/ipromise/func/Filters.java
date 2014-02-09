package me.tatarka.ipromise.func;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import me.tatarka.ipromise.compat.Objects;

/**
 * Some useful filters to use with {@link me.tatarka.ipromise.Promise#then(Filter)}.
 *
 * @author Evan Tatarka
 * @see me.tatarka.ipromise.func.Filter
 */
public final class Filters {
    private Filters() {
    }

    /**
     * Filters out any duplicates. For example, {@code [1, 2, 1, 3, 2]} will become {@link [1, 2,
     * 3]}. This has to store all values it comes across to check duplicates, so use cautiously.
     *
     * @param <T> the item type
     * @return the {@link me.tatarka.ipromise.func.Filter}
     */
    public static <T> Filter<T> unique() {
        return new Filter<T>() {
            private Set<T> seenItems = new HashSet<T>();

            @Override
            public boolean filter(T item) {
                return seenItems.add(item);
            }
        };
    }

    /**
     * Filters out consecutive duplicates. For example, {@code [1, 1, 2, 2, 3]} will become {@link
     * [1, 2, 3]}.
     *
     * @param <T> the item type
     * @return the {@link me.tatarka.ipromise.func.Filter}
     */
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

    /**
     * Filters out items so that you will never get two consecutive ones within the given timespan.
     * This is useful, if you wan't to sample a source at a lower rate.
     *
     * @param timespan the timespan
     * @param unit     the unit of the timespan
     * @param <T>      the item type
     * @return the {@link me.tatarka.ipromise.func.Filter}
     */
    public static <T> Filter<T> rateLimit(final long timespan, final TimeUnit unit) {
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
                    if (elapsed >= unit.toNanos(timespan)) {
                        lastTime = now;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    /**
     * Filters out items not equal to the given value. This is useful if you only care about a
     * subset of events.
     *
     * @param value the value to check equality
     * @param <T>   the item type
     * @return the {@link me.tatarka.ipromise.func.Filter}
     */
    public static <T> Filter<T> equal(final T value) {
        return new Filter<T>() {
            @Override
            public boolean filter(T item) {
                return Objects.equals(value, item);
            }
        };
    }

    /**
     * Inverts a filter so that if the given filter accepts an item it will be rejected and
     * vice-versa.
     *
     * @param filter the filter to invert
     * @param <T>    the item type
     * @return the {@link me.tatarka.ipromise.func.Filter}
     */
    public static <T> Filter<T> not(final Filter<T> filter) {
        return new Filter<T>() {
            @Override
            public boolean filter(T item) {
                return !filter.filter(item);
            }
        };
    }
}
