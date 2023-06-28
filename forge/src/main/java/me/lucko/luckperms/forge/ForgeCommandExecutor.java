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

import java.util.List;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;

public class ForgeCommandExecutor extends CommandManager {

    private final LPForgePlugin plugin;

    public ForgeCommandExecutor(final LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void registerCommands() {
        System.out.println("REGISTERING....");
        System.out.println("Is server present?: " + this.plugin.getBootstrap().getServer().isPresent());

        for (final String alias : new String[] {"luckperms", "lp", "perm", "perms", "permission",
                "permissions"}) {
            this.plugin.getBootstrap().getServer().ifPresent(
                    minecraftServer -> {
                        System.out.println("Registering '" + alias + "'.");
                        ((ServerCommandManager) minecraftServer.getCommandManager()).registerCommand(
                            new CommandBase() {
                                @Override
                                public String getCommandName() {
                                    return alias;
                                }

                                @Override
                                public boolean canCommandSenderUseCommand(
                                        final ICommandSender sender) {
                                    return true;
                                }

                                @Override
                                public String getCommandUsage(final ICommandSender sender) {
                                    return '/' + this.getCommandName() + "help";
                                }

                                @Override
                                public void processCommand(final ICommandSender sender,
                                        final String[] args) {
                                    final Sender wrapped =
                                            ForgeCommandExecutor.this.plugin.getSenderFactory()
                                                    .wrap(sender);
                                    final List<String> arguments =
                                            ArgumentTokenizer.EXECUTE.tokenizeInput(args);
                                    ForgeCommandExecutor.this.executeCommand(wrapped,
                                            this.getCommandName(), arguments);
                                }

                                @Override
                                public List<String> addTabCompletionOptions(
                                        final ICommandSender sender, final String[] args) {
                                    final Sender wrapped =
                                            ForgeCommandExecutor.this.plugin.getSenderFactory()
                                                    .wrap(sender);
                                    final List<String> arguments =
                                            ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
                                    return ForgeCommandExecutor.this.tabCompleteCommand(wrapped,
                                            arguments);
                                }
                            });});
        }
    }
}
