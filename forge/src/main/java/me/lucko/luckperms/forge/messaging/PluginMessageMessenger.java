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

package me.lucko.luckperms.forge.messaging;

import com.google.common.collect.Iterables;
import io.netty.buffer.Unpooled;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;

public class PluginMessageMessenger extends AbstractPluginMessageMessenger implements Messenger {
    private static final ResourceLocation CHANNEL =
            new ResourceLocation(AbstractPluginMessageMessenger.CHANNEL);

    private final LPForgePlugin plugin;
    private EventNetworkChannel channel;

    public PluginMessageMessenger(final LPForgePlugin plugin,
            final IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        this.channel = NetworkRegistry.newEventChannel(CHANNEL, () -> "1", predicate -> true,
                predicate -> true);
        this.channel.addListener(event -> {
            final byte[] buf = new byte[event.getPayload().readableBytes()];
            event.getPayload().readBytes(buf);

            this.handleIncomingMessage(buf);
            event.getSource().get().setPacketHandled(true);
        });
    }

    @Override
    protected void sendOutgoingMessage(final byte[] buf) {
        final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
        final SchedulerTask task = this.plugin.getBootstrap().getScheduler().asyncRepeating(() -> {
            final ServerPlayerEntity player =
                    this.plugin.getBootstrap().getServer().map(MinecraftServer::getPlayerList)
                            .map(PlayerList::getPlayers)
                            .map(players -> Iterables.getFirst(players, null)).orElse(null);

            if (player == null) {
                return;
            }

            final PacketBuffer byteBuf = new PacketBuffer(Unpooled.buffer());
            byteBuf.writeBytes(buf);
            final IPacket<?> packet = new SCustomPayloadPlayPacket(CHANNEL, byteBuf);

            player.connection.send(packet);

            final SchedulerTask t = taskRef.getAndSet(null);
            if (t != null) {
                t.cancel();
            }
        }, 10, TimeUnit.SECONDS);
        taskRef.set(task);
    }

}
