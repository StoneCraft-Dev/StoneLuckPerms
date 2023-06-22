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

package me.lucko.luckperms.forge.service;

import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.LPForgeBootstrap;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.capabilities.UserCapabilityImpl;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForgePermissionHandler implements IPermissionHandler {
    public static final ResourceLocation IDENTIFIER =
            new ResourceLocation(LPForgeBootstrap.ID, "permission_handler");

    private final LPForgePlugin plugin;
    private final Set<ForgePermissionNode> permissionNodes = new HashSet<>();

    public ForgePermissionHandler(final LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerNode(final @NotNull String node,
            final @NotNull DefaultPermissionLevel level, final @NotNull String desc) {
        this.plugin.getPermissionRegistry().insert(node);
        this.permissionNodes.add(new ForgePermissionNode(node, level, desc));
    }

    @Override
    public @NotNull Set<String> getRegisteredNodes() {
        return this.permissionNodes.stream().map(ForgePermissionNode::getNode)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasPermission(final @NotNull GameProfile profile, final @NotNull String node,
            @Nullable final IContext context) {
        final Optional<ServerPlayerEntity> player =
                this.plugin.getBootstrap().getPlayer(profile.getId());
        final Boolean result = player.isPresent() ? this.getPermission(player.get(), node)
                : this.getOfflinePermission(profile.getId(), node);

        return result != null && result;
    }

    @Override
    public @NotNull String getNodeDescription(final @NotNull String node) {
        return this.permissionNodes.stream().map(ForgePermissionNode::getNode)
                .filter(n -> n.equals(node)).findFirst().orElse("");
    }

    public ResourceLocation getIdentifier() {
        return IDENTIFIER;
    }

    public Set<ForgePermissionNode> getRegisteredPermissionNodes() {
        return this.permissionNodes;
    }

    public Boolean getPermission(final ServerPlayerEntity player, final String node) {
        final UserCapabilityImpl capability = UserCapabilityImpl.getNullable(player);

        if (capability != null) {
            final User user = capability.getUser();
            final QueryOptions queryOptions = capability.getQueryOptionsCache().getQueryOptions();
            final Boolean value = this.getPermissionValue(user, queryOptions, node);

            if (value != null) {
                return this.getPermissionValue(user, queryOptions, node);
            }
        }

        return null;
    }

    public Boolean getOfflinePermission(final UUID player, final String node) {
        final User user = this.plugin.getUserManager().getIfLoaded(player);

        if (user != null) {
            final QueryOptions queryOptions = user.getQueryOptions();

            return this.getPermissionValue(user, queryOptions, node);
        }

        return null;
    }

    private Boolean getPermissionValue(final User user, final QueryOptions queryOptions,
            final String key) {
        // permission check
        final PermissionCache cache = user.getCachedData().getPermissionData(queryOptions);
        final Tristate value =
                cache.checkPermission(key, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();

        return value == Tristate.UNDEFINED ? null : value.asBoolean();
    }
}
