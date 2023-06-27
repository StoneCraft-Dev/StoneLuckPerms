package me.lucko.luckperms.forge.messaging;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.Level;

public class ForgeNetworkEventFiringHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {
    private final ForgeEventChannel eventChannel;

    public ForgeNetworkEventFiringHandler(final ForgeEventChannel forgeEventChannel) {
        this.eventChannel = forgeEventChannel;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FMLProxyPacket msg) {
        this.eventChannel.fireRead(msg, ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
        this.eventChannel.fireUserEvent(evt, ctx);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
            throws Exception {
        FMLLog.log(Level.ERROR, cause, "ForgeNetworkEventFiringHandler exception");
        super.exceptionCaught(ctx, cause);
    }
}
