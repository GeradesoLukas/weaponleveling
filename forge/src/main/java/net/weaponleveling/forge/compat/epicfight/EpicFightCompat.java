package net.weaponleveling.forge.compat.epicfight;

import dev.architectury.platform.Platform;
import net.minecraft.world.entity.player.Player;

public class EpicFightCompat {


    public static final String modId = "epicfight";
    public static final Boolean isLoaded = Platform.isModLoaded(modId);


    public static void updateEpicItem (Player player, int xp) {
        if(isLoaded) {
          EpicFightMethods.updateEpicItem(player,xp);

        }
    }
}
