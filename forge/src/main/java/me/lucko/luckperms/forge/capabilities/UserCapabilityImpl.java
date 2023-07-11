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

package me.lucko.luckperms.forge.capabilities;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.context.ForgeContextManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserCapabilityImpl implements UserCapability {

    public static final Map<UUID, UserCapabilityImpl> CAPABILITIES = new HashMap<>();
    private boolean initialised = false;
    private User user;
    private QueryOptionsCache<EntityPlayerMP> queryOptionsCache;
    private String language;
    private Locale locale;

    public UserCapabilityImpl() {}

    /**
     * Gets a {@link UserCapability} for a given {@link EntityPlayer}.
     * If the capability does not exist, it is created.
     *
     * @param player the player
     * @return the capability
     */
    public static @NotNull UserCapabilityImpl get(@NotNull final EntityPlayer player) {
        CAPABILITIES.putIfAbsent(player.getUniqueID(), new UserCapabilityImpl());

        return CAPABILITIES.get(player.getUniqueID());
    }

    /**
     * Gets a {@link UserCapability} for a given {@link EntityPlayer}.
     *
     * @param player the player
     * @return the capability, or null
     */
    public static @Nullable UserCapabilityImpl getNullable(@NotNull final EntityPlayer player) {
        return CAPABILITIES.get(player.getUniqueID());
    }

    public void initialise(final User user, final EntityPlayerMP player,
            final ForgeContextManager contextManager) {
        this.user = user;
        this.queryOptionsCache = new QueryOptionsCache<>(player, contextManager);
        this.initialised = true;
    }

    public void resetQueryOptionsCache(final EntityPlayerMP player,
            final ForgeContextManager contextManager) {
        this.queryOptionsCache = new QueryOptionsCache<>(player, contextManager);
    }

    private void assertInitialised() {
        if (!this.initialised) {
            throw new IllegalStateException("Capability has not been initialised");
        }
    }

    @Override
    public Tristate checkPermission(final String permission) {
        this.assertInitialised();

        if (permission == null) {
            throw new NullPointerException("permission");
        }

        return this.checkPermission(permission, this.queryOptionsCache.getQueryOptions());
    }

    @Override
    public Tristate checkPermission(final String permission, final QueryOptions queryOptions) {
        this.assertInitialised();

        if (permission == null) {
            throw new NullPointerException("permission");
        }

        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final PermissionCache cache = this.user.getCachedData().getPermissionData(queryOptions);
        return cache.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    public User getUser() {
        this.assertInitialised();
        return this.user;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return this.getQueryOptionsCache().getQueryOptions();
    }

    public QueryOptionsCache<EntityPlayerMP> getQueryOptionsCache() {
        this.assertInitialised();
        return this.queryOptionsCache;
    }

    public Locale getLocale(final EntityPlayerMP player) {
        return this.locale;
    }
}