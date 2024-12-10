package squeek.spiceoflife.foodtracker;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.relauncher.Side;
import squeek.applecore.api.food.FoodEvent;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.compat.PacketDispatcher;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.items.ItemFoodJournal;
import squeek.spiceoflife.network.PacketFoodEatenAllTime;
import squeek.spiceoflife.network.PacketFoodHistory;

public class FoodTracker {

    public static int getFoodHistoryLengthInRelevantUnits(EntityPlayer player) {
        return FoodHistory.get(player)
            .getHistoryLength();
    }

    public static ItemStack getFoodLastEatenBy(EntityPlayer player) {
        return FoodHistory.get(player)
            .getLastEatenFood().itemStack;
    }

    /**
     * Save food eaten to the history
     */
    @SubscribeEvent
    public void onFoodEaten(FoodEvent.FoodEaten event) {
        if (event.player.worldObj.isRemote) return;

        FoodEaten foodEaten = new FoodEaten(event.food);
        foodEaten.foodValues = event.foodValues;

        FoodTracker.addFoodEatenByPlayer(foodEaten, event.player);
    }

    public static void addFoodEatenByPlayer(FoodEaten foodEaten, EntityPlayer player) {
        // client needs to be told by the server otherwise the client can get out of sync easily
        if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) PacketDispatcher.get()
            .sendTo(new PacketFoodHistory(foodEaten, player), (EntityPlayerMP) player);
        FoodHistory.get(player)
            .addFood(foodEaten);
    }

    /**
     * Add relevant extended entity data whenever an entity comes into existence
     */
    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) {
            FoodHistory.get((EntityPlayer) event.entity);
        }
    }

    /**
     * Sync savedata/config whenever a player joins the server
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        // server needs to send config settings to the client
        ModConfig.sync((EntityPlayerMP) event.player);

        // server needs to send food groups to the client
        FoodGroupRegistry.sync((EntityPlayerMP) event.player);

        // server needs to send any loaded data to the client
        FoodHistory foodHistory = FoodHistory.get(event.player);
        foodHistory.validate();
        syncFoodHistory(foodHistory);

        // give food journal
        if (!foodHistory.wasGivenFoodJournal && ModConfig.GIVE_FOOD_JOURNAL_ON_START) {
            ItemFoodJournal.giveToPlayer(event.player);
            foodHistory.wasGivenFoodJournal = true;
        }
    }

    public static void syncFoodHistory(FoodHistory foodHistory) {
        PacketDispatcher.get()
            .sendTo(
                new PacketFoodEatenAllTime(foodHistory.totalFoodsEatenAllTime),
                (EntityPlayerMP) foodHistory.player);
        PacketDispatcher.get()
            .sendTo(new PacketFoodHistory(foodHistory, true), (EntityPlayerMP) foodHistory.player);
        MaxHealthHandler.updateFoodHPModifier(foodHistory.player);
    }

    /**
     * Resync food history whenever a player changes dimensions
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        FoodHistory foodHistory = FoodHistory.get(event.player);
        syncFoodHistory(foodHistory);
    }

    /**
     * Save death-persistent data to avoid any rollbacks on respawn
     */
    @SubscribeEvent
    public void onLivingDeathEvent(LivingDeathEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient() || !(event.entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.entity;

        FoodHistory foodHistory = FoodHistory.get(player);
        foodHistory.saveNBTData(null);
    }

    /**
     * Load any death-persistent savedata on respawn and sync it
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient()) return;

        // load any persistent food history data
        FoodHistory foodHistory = FoodHistory.get(event.player);
        foodHistory.loadNBTData(null);

        // server needs to send any loaded data to the client
        syncFoodHistory(foodHistory);
    }

    /**
     * Assume the server doesn't have the mod
     */
    @SubscribeEvent
    public void onClientConnectedToServer(ClientConnectedToServerEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.CLIENT) ModConfig.assumeClientOnly();
    }
}
