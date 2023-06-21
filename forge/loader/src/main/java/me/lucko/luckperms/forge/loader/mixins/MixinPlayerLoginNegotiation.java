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

package me.lucko.luckperms.forge.loader.mixins;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import me.lucko.luckperms.forge.loader.PlayerNegotiationEvent;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FMLHandshakeHandler.class, remap = false)
public final class MixinPlayerLoginNegotiation {
    @Shadow
    @Final
    private static Logger LOGGER;
    private final List<Future<Void>> pendingFutures = new ArrayList<>();
    @Shadow
    @Final
    private NetworkManager manager;
    private boolean negotiationStarted = false;

    @Inject(method = "tickServer", at = @At("HEAD"))
    public void tickServer(final CallbackInfoReturnable<Boolean> cir) {
        if (!this.negotiationStarted) {
            final GameProfile profile =
                    ((MixinServerLoginNetHandlerAccessor) this.manager.getPacketListener()).getGameProfile();
            final PlayerNegotiationEvent event =
                    new PlayerNegotiationEvent(this.manager, profile, this.pendingFutures);
            MinecraftForge.EVENT_BUS.post(event);
            this.negotiationStarted = true;
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
    }
}
