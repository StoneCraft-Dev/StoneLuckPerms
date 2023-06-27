package me.lucko.luckperms.forge.loader;

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
