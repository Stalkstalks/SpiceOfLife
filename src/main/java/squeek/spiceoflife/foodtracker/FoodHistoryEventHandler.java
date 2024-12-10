package squeek.spiceoflife.foodtracker;

import net.minecraft.entity.player.EntityPlayerMP;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import squeek.applecore.api.hunger.ExhaustionEvent;
import squeek.applecore.api.hunger.HealthRegenEvent;
import squeek.applecore.api.hunger.StarvationEvent;
import squeek.spiceoflife.compat.PacketDispatcher;
import squeek.spiceoflife.network.PacketNutritionExhaust;

public class FoodHistoryEventHandler {

    // major breakpoints are:
    // .02% reduces starvation damage, prevents regen speed penalty
    // 35% for +1 exhaustion/regen speed (this is the point where just rotating single-type foods doesn't cut it
    // anymore)
    // 100% for +2 exhaustion/regen speed - just for neatness.
    // 135% drops to +1 again, this is the point where alternating between 2 and 3 group meals doesn't cut it anymore.
    // 168% drops to 0 again, this is the point where you need to be eating 5-group meals when you aren't even hungry.

    @SubscribeEvent
    public void getMaxExhaustion(ExhaustionEvent.GetMaxExhaustion event) {
        FoodHistory foodHistory = FoodHistory.get(event.player);
        event.maxExhaustionLevel *= (.5f + (.25f * foodHistory.getFoodGroupsBonus()));
        // event.maxExhaustionLevel * (.5f + (.25f * foodHistory.getFoodGroupsBonus()));
        // (.5f + (.25f * foodHistory.getFoodGroupsWithinRange(675,500) +
        // foodHistory.getFoodGroupsWithinRange(840,175)));
    }

    @SubscribeEvent
    public void onExhausted(ExhaustionEvent.Exhausted event) {
        FoodHistory foodHistory = FoodHistory.get(event.player);
        foodHistory.exhaustFoodGroupPoints();
        if (!event.player.worldObj.isRemote && event.player instanceof EntityPlayerMP) PacketDispatcher.get()
            .sendTo(new PacketNutritionExhaust(), (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onHealthRegenTick(HealthRegenEvent.GetRegenTickPeriod event) {
        try {
            FoodHistory foodHistory = FoodHistory.get(event.player);
            event.regenTickPeriod = Math.round(
                event.regenTickPeriod
                    * ((5f + foodHistory.getFoodGroupsAtMost(0)) / (5f + foodHistory.getFoodGroupsBonus() * 1.5f)));
        } catch (NullPointerException e) {
            FMLLog.log(Level.ERROR, e.getMessage(), "fuck its healthRegenIssues");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onStarve(StarvationEvent.Starve event) {
        FoodHistory foodHistory = FoodHistory.get(event.player);
        float mult = 1.0f - (foodHistory.getFoodGroupsAtLeast(1) * 0.2f);

        foodHistory.exhaustFoodGroupPoints();
        if (!event.player.worldObj.isRemote && event.player instanceof EntityPlayerMP) PacketDispatcher.get()
            .sendTo(new PacketNutritionExhaust(), (EntityPlayerMP) event.player);

        event.starveDamage *= mult;
    }
}
