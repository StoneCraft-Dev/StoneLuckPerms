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

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Arrays;
import java.util.Locale;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.minecraft.server.management.UserListOps;
import net.minecraftforge.event.CommandEvent;

public class ForgePlatformListener {
    private final LPForgePlugin plugin;

    public ForgePlatformListener(final LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onCommand(final CommandEvent event) {
        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            final String name = event.command.getCommandName().toLowerCase(Locale.ROOT);

            if (name.equals("op") || name.equals("deop")) {
                Message.OP_DISABLED.send(this.plugin.getSenderFactory().wrap(event.sender));
                event.setCanceled(true);
            }
        }
    }

    public void onServerStarted() {
        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {

            this.plugin.getBootstrap().getServer().ifPresent(server -> {
                final UserListOps ops = server.getConfigurationManager().getOppedPlayers();

                // TODO: Check
                Arrays.stream(ops.getKeys()).forEach(ops::removeEntry);
            });
        }
    }
}
