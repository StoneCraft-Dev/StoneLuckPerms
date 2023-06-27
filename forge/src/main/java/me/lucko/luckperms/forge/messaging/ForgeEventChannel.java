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

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.EnumMap;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;

public class ForgeEventChannel {

    private final EnumMap<Side, FMLEmbeddedChannel> channels;
    private final EventBus eventBus;
    private final Consumer<byte[]> consumer;

    public ForgeEventChannel(final String name, final Consumer<byte[]> consumer) {
        this.channels =
                NetworkRegistry.INSTANCE.newChannel(name, new ForgeNetworkEventFiringHandler(this));
        this.eventBus = new EventBus();
        this.consumer = consumer;
    }

    /**
     * Register an event listener with this channel and bus. See {@link SubscribeEvent}
     *
     * @param object
     */
    public void register(final Object object) {
        this.eventBus.register(object);
    }

    /**
     * Unregister an event listener from the bus.
     *
     * @param object
     */
    public void unregister(final Object object) {
        this.eventBus.unregister(object);
    }

    void fireRead(final FMLProxyPacket msg, final ChannelHandlerContext ctx) {
        final byte[] buf = new byte[msg.payload().readableBytes()];

        msg.payload().readBytes(buf);
        this.consumer.accept(buf);
    }

    public void fireUserEvent(final Object evt, final ChannelHandlerContext ctx) {
        final FMLNetworkEvent.CustomNetworkEvent event =
                new FMLNetworkEvent.CustomNetworkEvent(evt);
        this.eventBus.post(event);
    }

    /**
     * Send a packet to all on the server
     *
     * @param pkt
     */
    public void sendToAll(final FMLProxyPacket pkt) {
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.ALL);
        this.channels.get(Side.SERVER).writeAndFlush(pkt)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Send to a specific player
     *
     * @param pkt
     * @param player
     */
    public void sendTo(final FMLProxyPacket pkt, final EntityPlayerMP player) {
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.PLAYER);
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        this.channels.get(Side.SERVER).writeAndFlush(pkt)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Send to all around a point
     *
     * @param pkt
     * @param point
     */
    public void sendToAllAround(final FMLProxyPacket pkt, final NetworkRegistry.TargetPoint point) {
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        this.channels.get(Side.SERVER).writeAndFlush(pkt)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Send to all in a dimension
     *
     * @param pkt
     * @param dimensionId
     */
    public void sendToDimension(final FMLProxyPacket pkt, final int dimensionId) {
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        this.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
                .set(dimensionId);
        this.channels.get(Side.SERVER).writeAndFlush(pkt)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Send to the server
     *
     * @param pkt
     */
    public void sendToServer(final FMLProxyPacket pkt) {
        this.channels.get(Side.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        this.channels.get(Side.CLIENT).writeAndFlush(pkt)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
