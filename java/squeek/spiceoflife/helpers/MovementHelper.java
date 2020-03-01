package squeek.spiceoflife.helpers;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;

import java.util.HashMap;

public class MovementHelper {
    public static HashMap<EntityPlayer, MovementInfo> movementInfoByPlayer = new HashMap<EntityPlayer, MovementInfo>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new MovementHelper());
    }

    public static boolean getDidJumpLastTick(EntityPlayer player) {
        MovementInfo movementInfo = movementInfoByPlayer.get(player);
        if (movementInfo != null) {
            return player.worldObj.getWorldTime() - movementInfo.lastJump <= 2;
        } else
            return false;
    }

    @SubscribeEvent
    public void onLivingJump(LivingJumpEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            MovementInfo movementInfo = movementInfoByPlayer.get(player);

            if (movementInfo == null)
                movementInfo = new MovementInfo();

            movementInfo.lastJump = event.entityLiving.worldObj.getWorldTime();
            movementInfoByPlayer.put(player, movementInfo);
        }
    }

    public static class MovementInfo {
        public long lastJump;
    }
}
