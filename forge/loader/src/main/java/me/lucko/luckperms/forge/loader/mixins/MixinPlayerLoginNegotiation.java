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

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import me.lucko.luckperms.forge.events.PlayerNegotiationEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkDispatcher.class, remap = false)
public abstract class MixinPlayerLoginNegotiation {

    private final List<Future<Void>> pendingFutures = new ArrayList<>();
    @Shadow
    @Final
    public NetworkManager manager;
    @Shadow
    private EntityPlayerMP player;
    private boolean negotiationStarted = false;

    @Inject(method = "serverInitiateHandshake", at = @At("HEAD"))
    public void serverInitiateHandshake(final CallbackInfoReturnable<Integer> cir) {
        if (!this.negotiationStarted) {
            final PlayerNegotiationEvent event =
                    new PlayerNegotiationEvent(this.manager, this.player.getGameProfile(),
                            this.pendingFutures);
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
                FMLLog.severe("Error during negotiation", ex.getCause());
            } catch (final CancellationException | InterruptedException ex) {
                // no-op
            }

            return true;
        });
    }
}
