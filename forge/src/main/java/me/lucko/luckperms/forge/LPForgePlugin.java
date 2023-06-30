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

import cpw.mods.fml.common.ModContainer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.DummyConsoleSender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.forge.calculator.ForgeCalculatorFactory;
import me.lucko.luckperms.forge.capabilities.UserCapabilityListener;
import me.lucko.luckperms.forge.context.ForgeContextManager;
import me.lucko.luckperms.forge.context.ForgePlayerCalculator;
import me.lucko.luckperms.forge.listeners.ForgeAutoOpListener;
import me.lucko.luckperms.forge.listeners.ForgeConnectionListener;
import me.lucko.luckperms.forge.listeners.ForgePlatformListener;
import me.lucko.luckperms.forge.messaging.ForgeMessagingFactory;
import me.lucko.luckperms.forge.util.ForgeEventBusFacade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * LuckPerms implementation for Forge.
 */
public class LPForgePlugin extends AbstractLuckPermsPlugin {

    private final LPForgeBootstrap bootstrap;
    private ForgeSenderFactory senderFactory;
    private ForgeConnectionListener connectionListener;
    private ForgePlatformListener platformListener;
    private ForgeCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private ForgeContextManager contextManager;

    public LPForgePlugin(final LPForgeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPForgeBootstrap getBootstrap() {
        return this.bootstrap;
    }

    protected void registerEarlyListeners() {
        this.connectionListener = new ForgeConnectionListener(this);
        this.bootstrap.registerListeners(this.connectionListener,
                ForgeEventBusFacade.EventBusType.BOTH);

        this.platformListener = new ForgePlatformListener(this);
        this.bootstrap.registerListeners(this.platformListener,
                ForgeEventBusFacade.EventBusType.MC);

        final UserCapabilityListener userCapabilityListener = new UserCapabilityListener();
        this.bootstrap.registerListeners(userCapabilityListener,
                ForgeEventBusFacade.EventBusType.MC);

        this.commandManager = new ForgeCommandExecutor(this);
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new ForgeSenderFactory(this);
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        return Collections.emptySet();
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new ForgeConfigAdapter(this, this.resolveConfig("luckperms.conf"));
    }

    @Override
    protected void registerPlatformListeners() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new ForgeMessagingFactory(this);
    }

    @Override
    protected void registerCommands() {
        // Too late for Forge, registered in #registerEarlyListeners
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new ForgeCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new ForgeContextManager(this);

        final ForgePlayerCalculator playerCalculator = new ForgePlayerCalculator(this,
                this.getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.bootstrap.registerListeners(playerCalculator, ForgeEventBusFacade.EventBusType.BOTH);
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
    }

    @Override
    protected AbstractEventBus<ModContainer> provideEventBus(final LuckPermsApiProvider provider) {
        return new ForgeEventBus(this, provider);
    }

    @Override
    protected void registerApiOnPlatform(final LuckPerms api) {
    }

    @Override
    protected void performFinalSetup() {
        // register autoop listener
        if (this.getConfiguration().get(ConfigKeys.AUTO_OP)) {
            this.getApiProvider().getEventBus().subscribe(new ForgeAutoOpListener(this));
        }
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(final User user) {
        return this.bootstrap.getPlayer(user.getUniqueId())
                .map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(Stream.of(this.getConsoleSender()), this.bootstrap.getServer()
                .map(minecraftServer -> (List<EntityPlayerMP>) minecraftServer.getConfigurationManager().playerEntityList)
                .map(players -> players.stream().map(player -> this.senderFactory.wrap(player)))
                .orElseGet(Stream::empty));
    }

    @Override
    public Sender getConsoleSender() {
        return this.bootstrap.getServer().map(server -> this.senderFactory.wrap(server))
                .orElseGet(() -> new DummyConsoleSender(this) {
                    @Override
                    public void sendMessage(final Component message) {
                        LPForgePlugin.this.bootstrap.getPluginLogger()
                                .info(PlainTextComponentSerializer.plainText()
                                        .serialize(TranslationManager.render(message)));
                    }
                });
    }

    public ForgeSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public ForgeConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    public ForgePlatformListener getPlatformListener() {
        return this.platformListener;
    }

    @Override
    public ForgeCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public ForgeContextManager getContextManager() {
        return this.contextManager;
    }

}
