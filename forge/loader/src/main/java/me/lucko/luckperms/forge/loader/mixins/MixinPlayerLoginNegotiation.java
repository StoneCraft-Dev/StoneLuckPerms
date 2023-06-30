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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import me.lucko.luckperms.forge.events.PlayerNegotiationEvent;
import net.minecraft.server.network.NetHandlerLoginServer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerLoginServer.class)
public abstract class MixinPlayerLoginNegotiation {

    private final List<Future<Void>> pendingFutures = new ArrayList<>();
    private boolean negotiationStarted = false;

    @Inject(method = "onNetworkTick", at = @At("HEAD"))
    public void tickServer(final CallbackInfo ci) {
        final NetHandlerLoginServerAccessor accessor = (NetHandlerLoginServerAccessor) this;

        if (!this.negotiationStarted) {
            final PlayerNegotiationEvent event =
                    new PlayerNegotiationEvent(accessor.getNetworkManager(),
                            accessor.getLoginGameProfile(), this.pendingFutures);
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
                NetHandlerLoginServerAccessor.getLogger()
                        .error("Error during negotiation", ex.getCause());
            } catch (final CancellationException | InterruptedException ex) {
                // no-op
            }

            return true;
        });
    }
}
