package me.lucko.luckperms.forge.util;

import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Equivalent to {@link Predicate}, except with nonnull contract.
 *
 * @see Predicate
 */
@FunctionalInterface
public interface NonNullPredicate<T> {
    boolean test(@NotNull T t);
}
