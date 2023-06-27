/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.forge.util;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import net.minecraftforge.common.MinecraftForge;

/**
 * A utility for registering Forge listeners for methods in a jar-in-jar.
 *
 * <p>This differs from {@link EventBus#register(Object)} as reflection is used for invoking the
 * registered listeners
 * instead of ASM, which is incompatible with {@link JarInJarClassLoader}</p>
 */
public class ForgeEventBusFacade {

    private final List<ListenerRegistration> listeners = new ArrayList<>();

    /**
     * Handles {@link EventBus#register(Object)}.
     */
    private static void addListener(final EventBus eventBus, final Object target) {
        eventBus.register(target);
    }

    /**
     * Register listeners for the target object.
     */
    public void register(final Object target, final EventBusType type) {
        Arrays.stream(type.eventBusses).forEach(eventBus -> {
            addListener(eventBus, target);
            this.listeners.add(new ListenerRegistration(eventBus, target));
        });
    }

    /**
     * Unregister previously registered listeners on the target object.
     *
     * @param target the target listener
     */
    public void unregister(final Object target) {
        this.listeners.removeIf(listener -> {
            if (listener.target == target) {
                listener.close();
                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Unregister all listeners created through this interface.
     */
    public void unregisterAll() {
        for (final ListenerRegistration listener : this.listeners) {
            listener.close();
        }
        this.listeners.clear();
    }

    public enum EventBusType {
        MC(MinecraftForge.EVENT_BUS), FML(FMLCommonHandler.instance().bus()),
        BOTH(MinecraftForge.EVENT_BUS, FMLCommonHandler.instance().bus());

        private final EventBus[] eventBusses;

        EventBusType(final EventBus... eventBus) {
            this.eventBusses = eventBus;
        }

        public EventBus[] getEventBuses() {
            return this.eventBusses;
        }
    }

    /**
     * A listener registration.
     */
    private static final class ListenerRegistration implements AutoCloseable {

        /**
         * The event bus that the invoker was registered to
         */
        private final EventBus eventBus;
        /**
         * The target listener class
         */
        private final Object target;

        private ListenerRegistration(final EventBus eventBus, final Object target) {
            this.eventBus = eventBus;
            this.target = target;
        }

        @Override
        public void close() {
            this.eventBus.unregister(this.target);
        }
    }
}
