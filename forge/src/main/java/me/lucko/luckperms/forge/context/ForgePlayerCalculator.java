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

package me.lucko.luckperms.forge.context;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.loader.PlayerChangeGameModeEvent;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ForgePlayerCalculator implements ContextCalculator<EntityPlayerMP> {

    private static final Field PROVIDERS_FIELD;

    static {
        try {
            PROVIDERS_FIELD = DimensionManager.class.getDeclaredField("providers");
            PROVIDERS_FIELD.setAccessible(true);
        } catch (final NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final LPForgePlugin plugin;

    private final boolean gamemode;
    private final boolean world;
    private final boolean dimensionType;

    public ForgePlayerCalculator(final LPForgePlugin plugin, final Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
        this.dimensionType = !disabled.contains(DefaultContextKeys.DIMENSION_TYPE_KEY);
    }

    @Override
    public void calculate(@NonNull final EntityPlayerMP target,
            @NonNull final ContextConsumer consumer) {
        final World level = target.worldObj;
        if (this.dimensionType) {
            consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY,
                    level.provider.getDimensionName());
        }

        final WorldInfo levelData = level.getWorldInfo();
        if (this.world) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES)
                    .rewriteAndSubmit(levelData.getWorldName(), consumer);
        }

        final WorldSettings.GameType gameMode = target.theItemInWorldManager.getGameType();
        if (this.gamemode && gameMode != WorldSettings.GameType.NOT_SET) {
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, gameMode.getName());
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        final ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        if (this.gamemode) {
            for (final WorldSettings.GameType gameType : WorldSettings.GameType.values()) {
                if (gameType == WorldSettings.GameType.NOT_SET) {
                    continue;
                }

                builder.add(DefaultContextKeys.GAMEMODE_KEY, gameType.getName());
            }
        }

        final MinecraftServer server = this.plugin.getBootstrap().getServer().orElse(null);
        if (this.dimensionType && server != null) {
            try {
                @SuppressWarnings("unchecked") final Hashtable<Integer, Class<?
                        extends WorldProvider>>
                        providers =
                        (Hashtable<Integer, Class<? extends WorldProvider>>) PROVIDERS_FIELD.get(
                                null);
                final Set<String> dimensionTypes = new HashSet<>();

                for (final Class<? extends WorldProvider> clazz : providers.values()) {
                    try {
                        dimensionTypes.add(clazz.newInstance().getDimensionName());
                    } catch (final InstantiationException ignored) {}
                }

                dimensionTypes.forEach(s -> builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, s));
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (this.world && server != null) {
            for (final WorldServer level : server.worldServers) {
                final WorldInfo levelData = level.getWorldInfo();
                if (Context.isValidValue(levelData.getWorldName())) {
                    builder.add(DefaultContextKeys.WORLD_KEY, levelData.getWorldName());
                }
            }
        }

        return builder.build();
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(
            final cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(this.world || this.dimensionType)) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerChangeGameMode(final PlayerChangeGameModeEvent event) {
        if (!this.gamemode || event.getNewGameMode() == WorldSettings.GameType.NOT_SET) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate((EntityPlayerMP) event.entityPlayer);
    }

}
