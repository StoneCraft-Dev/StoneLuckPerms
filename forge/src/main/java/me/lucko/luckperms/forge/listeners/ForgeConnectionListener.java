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

package me.lucko.luckperms.forge.listeners;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.forge.ForgeSenderFactory;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.capabilities.UserCapabilityImpl;
import me.lucko.luckperms.forge.util.PlayerNegotiationEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeConnectionListener extends AbstractConnectionListener {
    private final LPForgePlugin plugin;

    public ForgeConnectionListener(final LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onPlayerNegotiation(final PlayerNegotiationEvent event) {
        final String username = event.getProfile().getName();
        final UUID uniqueId = event.getProfile().isComplete() ? event.getProfile().getId()
                : PlayerEntity.createPlayerUUID(username);

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger()
                    .info("Processing pre-login (sync phase) for " + uniqueId + " - " + username);
        }

        event.enqueueWork(CompletableFuture.runAsync(() -> {
            this.onPlayerNegotiationAsync(event.getConnection(), uniqueId, username);
        }, this.plugin.getBootstrap().getScheduler().async()));
    }

    private void onPlayerNegotiationAsync(final NetworkManager connection, final UUID uniqueId,
            final String username) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger()
                    .info("Processing pre-login (async phase) for " + uniqueId + " - " + username);
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            final User user = this.loadUser(uniqueId, username);
            this.recordConnection(uniqueId);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, user);
        } catch (final Exception ex) {
            this.plugin.getLogger()
                    .severe("Exception occurred whilst loading data for " + uniqueId + " - "
                            + username, ex);

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                final Component component =
                        TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                connection.send(
                        new SDisconnectLoginPacket(ForgeSenderFactory.toNativeText(component)));
                connection.disconnect(ForgeSenderFactory.toNativeText(component));
            } else {
                // Schedule the message to be sent on the next tick.
                this.plugin.getBootstrap().getServer().orElseThrow(IllegalStateException::new)
                        .execute(() -> {
                            final Component component =
                                    TranslationManager.render(Message.LOADING_STATE_ERROR.build());
                            connection.send(
                                    new SChatPacket(ForgeSenderFactory.toNativeText(component),
                                            ChatType.SYSTEM, Util.NIL_UUID));
                        });
            }

            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLoadFromFile(final PlayerEvent.LoadFromFile event) {
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        final GameProfile profile = player.getGameProfile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + profile.getId() + " - "
                    + profile.getName());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(profile.getId());

        if (user == null) {
            if (!this.getUniqueConnections().contains(profile.getId())) {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName()
                        + " doesn't have data pre-loaded, they have never been processed during "
                        + "pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName()
                        + " doesn't currently have data pre-loaded, but they have been processed "
                        + "before in this session.");
            }

            final Component component =
                    TranslationManager.render(Message.LOADING_STATE_ERROR.build(),
                            player.getLanguage());
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
            } else {
                player.sendMessage(ForgeSenderFactory.toNativeText(component), Util.NIL_UUID);
            }
        }

        // initialise capability
        final UserCapabilityImpl userCapability = UserCapabilityImpl.get(player);
        userCapability.initialise(user, player, this.plugin.getContextManager());
        this.plugin.getContextManager().signalContextUpdate(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        this.handleDisconnect(player.getGameProfile().getId());
    }

}
