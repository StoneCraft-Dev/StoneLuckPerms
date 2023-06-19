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

package me.lucko.luckperms.forge;

import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.forge.util.ForgeEventBusFacade;
import net.luckperms.api.platform.Platform;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;

/**
 * Bootstrap plugin for LuckPerms running on Forge.
 */
public final class LPForgeBootstrap
        implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {
    public static final String ID = "luckperms";

    /**
     * The plugin loader
     */
    private final Supplier<ModContainer> loader;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final SchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final ClassPathAppender classPathAppender;

    /**
     * A facade for the forge event bus, compatible with LP's jar-in-jar packaging
     */
    private final ForgeEventBusFacade forgeEventBus;

    /**
     * The plugin instance
     */
    private final LPForgePlugin plugin;
    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);
    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;
    /**
     * The Minecraft server instance
     */
    private MinecraftServer server;

    public LPForgeBootstrap(final Supplier<ModContainer> loader) {
        this.loader = loader;
        this.logger = new Log4jPluginLogger(LogManager.getLogger(LPForgeBootstrap.ID));
        this.schedulerAdapter = new ForgeSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(this.getClass().getClassLoader());
        this.forgeEventBus = new ForgeEventBusFacade();
        this.plugin = new LPForgePlugin(this);
    }

    // provide adapters

    @Override
    public Object getLoader() {
        return this.loader;
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    public void registerListeners(final Object target) {
        this.forgeEventBus.register(target);
    }

    // lifecycle

    @Override
    public void onLoad() { // called by the loader on FMLCommonSetupEvent
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        this.forgeEventBus.register(this);
        this.plugin.registerEarlyListeners();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerAboutToStart(final FMLServerAboutToStartEvent event) {
        this.server = event.getServer();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStopping(final FMLServerStoppingEvent event) {
        this.plugin.disable();
        this.forgeEventBus.unregisterAll();
        this.server = null;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    // MinecraftServer singleton getter

    public Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(this.server);
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.FORGE;
    }

    @Override
    public String getServerBrand() {
        return ModList.get().getModContainerById("forge").map(ModContainer::getModInfo)
                .map(IModInfo::getDisplayName).orElse("null");
    }

    @Override
    public String getServerVersion() {
        final String forgeVersion =
                ModList.get().getModContainerById("forge").map(ModContainer::getModInfo)
                        .map(IModInfo::getVersion).map(ArtifactVersion::toString).orElse("null");

        return this.getServer().map(MinecraftServer::getServerVersion).orElse("null") + "-"
                + forgeVersion;
    }

    @Override
    public Path getDataDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(LPForgeBootstrap.ID).toAbsolutePath();
    }

    @Override
    public Optional<ServerPlayerEntity> getPlayer(final UUID uniqueId) {
        return this.getServer().map(MinecraftServer::getPlayerList)
                .map(playerList -> playerList.getPlayer(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(final String username) {
        return this.getServer().map(MinecraftServer::getProfileCache)
                .map(profileCache -> profileCache.get(username)).map(GameProfile::getId);
    }

    @Override
    public Optional<String> lookupUsername(final UUID uniqueId) {
        return this.getServer().map(MinecraftServer::getProfileCache)
                .map(profileCache -> profileCache.get(uniqueId)).map(GameProfile::getName);
    }

    @Override
    public int getPlayerCount() {
        return this.getServer().map(MinecraftServer::getPlayerCount).orElse(0);
    }

    @Override
    public Collection<String> getPlayerList() {
        return this.getServer().map(MinecraftServer::getPlayerList).map(PlayerList::getPlayers)
                .map(players -> {
                    final List<String> list = new ArrayList<>(players.size());
                    for (final ServerPlayerEntity player : players) {
                        list.add(player.getGameProfile().getName());
                    }
                    return list;
                }).orElse(Collections.emptyList());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return this.getServer().map(MinecraftServer::getPlayerList).map(PlayerList::getPlayers)
                .map(players -> {
                    final List<UUID> list = new ArrayList<>(players.size());
                    for (final ServerPlayerEntity player : players) {
                        list.add(player.getGameProfile().getId());
                    }
                    return list;
                }).orElse(Collections.emptyList());
    }

    @Override
    public boolean isPlayerOnline(final UUID uniqueId) {
        return this.getServer().map(MinecraftServer::getPlayerList)
                .map(playerList -> playerList.getPlayer(uniqueId)).isPresent();
    }

}
