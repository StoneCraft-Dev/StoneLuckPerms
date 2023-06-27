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

package me.lucko.luckperms.forge.events;

import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Cancelable
public class PlayerChangeGameModeEvent extends PlayerEvent {
    private final WorldSettings.GameType currentGameMode;
    private WorldSettings.GameType newGameMode;

    public PlayerChangeGameModeEvent(final EntityPlayer player,
            final WorldSettings.GameType currentGameMode,
            final WorldSettings.GameType newGameMode) {
        super(player);
        this.currentGameMode = currentGameMode;
        this.newGameMode = newGameMode;
    }

    public WorldSettings.GameType getCurrentGameMode() {
        return this.currentGameMode;
    }

    public WorldSettings.GameType getNewGameMode() {
        return this.newGameMode;
    }

    /**
     * Sets the game mode the player will be changed to if this event is not cancelled.
     */
    public void setNewGameMode(final WorldSettings.GameType newGameMode) {
        this.newGameMode = newGameMode;
    }
}
