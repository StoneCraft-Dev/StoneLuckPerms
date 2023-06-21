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

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Locale;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.context.ForgeContextManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserCapabilityImpl implements UserCapability {

    /**
     * The capability instance.
     */
    public static final Capability<UserCapability> CAPABILITY = getCapability();
    private boolean initialised = false;
    private User user;
    private QueryOptionsCache<ServerPlayerEntity> queryOptionsCache;
    private String language;
    private Locale locale;

    public UserCapabilityImpl() {}

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayerEntity}.
     *
     * @param player the player
     * @return the capability
     */
    public static @NotNull UserCapabilityImpl get(@NotNull final PlayerEntity player) {
        return (UserCapabilityImpl) player.getCapability(CAPABILITY).orElseThrow(
                () -> new IllegalStateException("Capability missing for " + player.getUUID()));
    }

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayerEntity}.
     *
     * @param player the player
     * @return the capability, or null
     */
    public static @Nullable UserCapabilityImpl getNullable(
            @NotNull final ServerPlayerEntity player) {
        return (UserCapabilityImpl) player.getCapability(CAPABILITY).resolve().orElse(null);
    }

    private static Capability<UserCapability> getCapability() {
        CapabilityManager.INSTANCE.register(UserCapability.class,
                new Capability.IStorage<UserCapability>() {
                    @Override
                    public INBT writeNBT(final Capability<UserCapability> capability,
                            final UserCapability instance, final Direction side) {
                        return null;
                    }

                    @Override
                    public void readNBT(final Capability<UserCapability> capability,
                            final UserCapability instance, final Direction side, final INBT nbt) {}
                }, UserCapabilityImpl::new);

        try {
            final Field field = CapabilityManager.class.getDeclaredField("providers");

            field.setAccessible(true);

            return (Capability<UserCapability>) ((IdentityHashMap<String, Capability<?>>) field.get(
                    CapabilityManager.INSTANCE)).get(UserCapability.class.getName().intern());
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not get capability!");
        }
    }

    public void initialise(final UserCapabilityImpl previous) {
        this.user = previous.user;
        this.queryOptionsCache = previous.queryOptionsCache;
        this.language = previous.language;
        this.locale = previous.locale;
        this.initialised = true;
    }

    public void initialise(final User user, final ServerPlayerEntity player,
            final ForgeContextManager contextManager) {
        this.user = user;
        this.queryOptionsCache = new QueryOptionsCache<>(player, contextManager);
        this.initialised = true;
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

    public QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache() {
        this.assertInitialised();
        return this.queryOptionsCache;
    }

    public Locale getLocale(final ServerPlayerEntity player) {
        if (this.language == null || !this.language.equals(player.getLanguage())) {
            this.language = player.getLanguage();
            this.locale = TranslationManager.parseLocale(this.language);
        }

        return this.locale;
    }
}