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
import java.util.Set;
import me.lucko.luckperms.forge.LPForgeBootstrap;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Check if this is even used.

public class ForgePermissionHandler implements IPermissionHandler {
    public static final ResourceLocation IDENTIFIER =
            new ResourceLocation(LPForgeBootstrap.ID, "permission_handler");

    private final LPForgePlugin plugin;
    private final Set<String> permissionNodes = new HashSet<>();

    public ForgePermissionHandler(final LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerNode(final String node, final DefaultPermissionLevel level,
            final String desc) {
        this.plugin.getPermissionRegistry().insert(node);
    }

    @Override
    public @NotNull Set<String> getRegisteredNodes() {
        return this.permissionNodes;
    }

    @Override
    public boolean hasPermission(final GameProfile profile, final String node,
            @Nullable final IContext context) {
        System.out.println("HAS PERMISSION???");
        return false;
    }

    @Override
    public @NotNull String getNodeDescription(final String node) {
        return "";
    }
}
