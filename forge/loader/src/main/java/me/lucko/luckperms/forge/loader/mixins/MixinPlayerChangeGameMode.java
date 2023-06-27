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

import javax.annotation.Nullable;
import me.lucko.luckperms.forge.events.PlayerChangeGameModeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ItemInWorldManager.class, remap = false)
public class MixinPlayerChangeGameMode {

    @Shadow
    public EntityPlayerMP thisPlayerMP;
    @Shadow
    private WorldSettings.GameType gameType;

    /**
     * @author AlphaConqueror
     * @reason Implementation of {@link PlayerChangeGameModeEvent}
     */
    @Overwrite
    public void setGameType(WorldSettings.GameType p_73076_1_) {
        p_73076_1_ = this.onChangeGameType(this.thisPlayerMP, this.gameType, p_73076_1_);

        if (p_73076_1_ == null) {
            return;
        }

        this.gameType = p_73076_1_;
        p_73076_1_.configurePlayerCapabilities(this.thisPlayerMP.capabilities);
        this.thisPlayerMP.sendPlayerAbilities();
    }

    @Unique
    @Nullable
    private WorldSettings.GameType onChangeGameType(final EntityPlayer player,
            final WorldSettings.GameType currentGameType,
            final WorldSettings.GameType newGameType) {
        if (currentGameType != newGameType) {
            final PlayerChangeGameModeEvent evt =
                    new PlayerChangeGameModeEvent(player, currentGameType, newGameType);
            MinecraftForge.EVENT_BUS.post(evt);
            return evt.isCanceled() ? null : evt.getNewGameMode();
        }
        return newGameType;
    }
}
