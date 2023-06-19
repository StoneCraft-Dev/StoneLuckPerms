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

import java.util.Map;
import me.lucko.luckperms.common.api.implementation.ApiUser;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.entity.player.ServerPlayerEntity;

public class ForgeAutoOpListener implements LuckPermsEventListener {
    private static final String NODE = "luckperms.autoop";

    private final LPForgePlugin plugin;

    public ForgeAutoOpListener(final LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(final EventBus bus) {
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onContextUpdate(final ContextUpdateEvent event) {
        event.getSubject(ServerPlayerEntity.class).ifPresent(player -> this.refreshAutoOp(player, true));
    }

    private void onUserDataRecalculate(final UserDataRecalculateEvent event) {
        final User user = ApiUser.cast(event.getUser());
        this.plugin.getBootstrap().getPlayer(user.getUniqueId())
                .ifPresent(player -> this.refreshAutoOp(player, false));
    }

    private void refreshAutoOp(final ServerPlayerEntity player, final boolean callerIsSync) {
        if (!callerIsSync && !this.plugin.getBootstrap().getServer().isPresent()) {
            return;
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUUID());

        final boolean value;
        if (user != null) {
            final QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
            final Map<String, Boolean> permData =
                    user.getCachedData().getPermissionData(queryOptions).getPermissionMap();
            value = permData.getOrDefault(NODE, false);
        } else {
            value = false;
        }

        if (callerIsSync) {
            this.setOp(player, value);
        } else {
            this.plugin.getBootstrap().getScheduler().executeSync(() -> this.setOp(player, value));
        }
    }

    private void setOp(final ServerPlayerEntity player, final boolean value) {
        this.plugin.getBootstrap().getServer().ifPresent(server -> {
            if (value) {
                server.getPlayerList().op(player.getGameProfile());
            } else {
                server.getPlayerList().deop(player.getGameProfile());
            }
        });
    }

}
