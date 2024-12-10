package squeek.spiceoflife.network;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.foodtracker.FoodHistory;

public class PacketNutritionExhaust extends PacketBase {

    public PacketNutritionExhaust() {}

    @Override
    public void pack(IByteIO data) {}

    @Override
    public void unpack(IByteIO data) {}

    @Override
    public PacketBase processAndReply(Side side, EntityPlayer player) {
        FoodHistory foodHistory = FoodHistory.get(player);
        foodHistory.exhaustFoodGroupPoints();
        return null;
    }
}
