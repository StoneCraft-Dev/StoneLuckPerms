package me.lucko.luckperms.forge.loader.mixins;

import javax.annotation.Nullable;
import me.lucko.luckperms.forge.loader.PlayerChangeGameModeEvent;
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
     * @reason Implementation of {@link me.lucko.luckperms.forge.loader.PlayerChangeGameModeEvent}
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
