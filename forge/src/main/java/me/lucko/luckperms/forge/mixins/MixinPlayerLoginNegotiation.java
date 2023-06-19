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

package me.lucko.luckperms.forge.mixins;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import me.lucko.luckperms.forge.util.PlayerNegotiationEvent;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import net.minecraftforge.fml.network.FMLLoginWrapper;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FMLHandshakeHandler.class, remap = false)
public final class MixinPlayerLoginNegotiation {

    @Shadow
    @Final
    static Marker FMLHSMARKER;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private static FMLLoginWrapper loginWrapper;
    private final List<Future<Void>> pendingFutures = new ArrayList<>();
    @Shadow
    @Final
    private NetworkManager manager;
    @Shadow
    private int packetPosition;
    @Shadow
    private List<NetworkRegistry.LoginPayload> messageList;
    @Shadow
    private List<Integer> sentMessages;
    private boolean negotiationStarted = false;

    /**
     * @author AlphaConqueror
     * @reason PlayerNegotiationEvent not implemented in 1.16.5
     */
    @Overwrite
    public boolean tickServer() {
        if (!this.negotiationStarted) {
            final GameProfile profile =
                    ((MixinServerLoginNetHandlerAccessor) this.manager.getPacketListener()).getGameProfile();
            final PlayerNegotiationEvent event =
                    new PlayerNegotiationEvent(this.manager, profile, this.pendingFutures);
            MinecraftForge.EVENT_BUS.post(event);
            this.negotiationStarted = true;
        }

        if (this.packetPosition < this.messageList.size()) {
            final NetworkRegistry.LoginPayload message = this.messageList.get(this.packetPosition);

            LOGGER.debug(FMLHSMARKER, "Sending ticking packet info '{}' to '{}' sequence {}",
                    message.getMessageContext(), message.getChannelName(), this.packetPosition);
            this.sentMessages.add(this.packetPosition);
            ((MixinFMLLoginWrapperInvoker) loginWrapper).sendServerToClientLoginPacket(
                    message.getChannelName(), message.getData(), this.packetPosition, this.manager);
            this.packetPosition++;
        }

        this.pendingFutures.removeIf(future -> {
            if (!future.isDone()) {
                return false;
            }

            try {
                future.get();
            } catch (final ExecutionException ex) {
                LOGGER.error("Error during negotiation", ex.getCause());
            } catch (final CancellationException | InterruptedException ex) {
                // no-op
            }

            return true;
        });

        // we're done when sentMessages is empty
        if (this.sentMessages.isEmpty() && this.packetPosition >= this.messageList.size() - 1) {
            // clear ourselves - we're done!
            this.manager.channel().attr(MixinFMLNetworkConstantsAccessor.getFML_HANDSHAKE_HANDLER())
                    .set(null);
            LOGGER.debug(FMLHSMARKER, "Handshake complete!");
            return true;
        }
        return false;
    }
}
