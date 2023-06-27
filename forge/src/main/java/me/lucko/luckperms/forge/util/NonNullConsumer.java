package me.lucko.luckperms.forge.util;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Equivalent to {@link Consumer}, except with nonnull contract.
 *
 * @see Consumer
 */
@FunctionalInterface
public interface NonNullConsumer<T> {
    void accept(@NotNull T t);
}
