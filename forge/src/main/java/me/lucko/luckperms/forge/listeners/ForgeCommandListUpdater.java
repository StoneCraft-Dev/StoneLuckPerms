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

import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.lucko.luckperms.common.api.implementation.ApiGroup;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

/**
 * Calls
 * {@link net.minecraft.server.management.PlayerList#sendPlayerPermissionLevel(net.minecraft.entity.player.ServerPlayerEntity)} when a players permissions change.
 */
public class ForgeCommandListUpdater implements LuckPermsEventListener {
    private final LPForgePlugin plugin;
    private final LoadingCache<UUID, SendBuffer> sendingBuffers =
            CaffeineFactory.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS)
                    .build(SendBuffer::new);

    public ForgeCommandListUpdater(final LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(final EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(GroupDataRecalculateEvent.class, this::onGroupDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(final UserDataRecalculateEvent e) {
        this.requestUpdate(e.getUser().getUniqueId());
    }

    private void onGroupDataRecalculate(final GroupDataRecalculateEvent e) {
        this.plugin.getUserManager().getAll().values().forEach(user -> {
            if (user.resolveInheritanceTree(user.getQueryOptions())
                    .contains(ApiGroup.cast(e.getGroup()))) {
                this.requestUpdate(user.getUniqueId());
            }
        });
    }

    private void onContextUpdate(final ContextUpdateEvent e) {
        e.getSubject(ServerPlayerEntity.class).ifPresent(p -> this.requestUpdate(p.getUUID()));
    }

    private void requestUpdate(final UUID uniqueId) {
        if (!this.plugin.getBootstrap().isPlayerOnline(uniqueId)) {
            return;
        }

        // Buffer the request to send a commands update.
        final SendBuffer sendBuffer = this.sendingBuffers.get(uniqueId);
        if (sendBuffer != null) {
            sendBuffer.request();
        }
    }

    // Called when the buffer times out.
    private void sendUpdate(final UUID uniqueId) {
        this.plugin.getBootstrap().getScheduler().sync().execute(() -> {
            this.plugin.getBootstrap().getPlayer(uniqueId).ifPresent(player -> {
                final MinecraftServer server = player.getServer();
                if (server != null) {
                    server.getPlayerList().sendPlayerPermissionLevel(player);
                }
            });
        });
    }

    private final class SendBuffer extends BufferedRequest<Void> {
        private final UUID uniqueId;

        SendBuffer(final UUID uniqueId) {
            super(500, TimeUnit.MILLISECONDS,
                    ForgeCommandListUpdater.this.plugin.getBootstrap().getScheduler());
            this.uniqueId = uniqueId;
        }

        @Override
        protected Void perform() {
            ForgeCommandListUpdater.this.sendUpdate(this.uniqueId);
            return null;
        }
    }

}
