package squeek.spiceoflife.network;

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import squeek.spiceoflife.ModInfo;
import squeek.spiceoflife.network.simpleimpl.BetterSimpleNetworkWrapper;

public class PacketHandler implements IMessageHandler<PacketBase, PacketBase> {

    public static final BetterSimpleNetworkWrapper channel = new BetterSimpleNetworkWrapper(ModInfo.NETCHANNEL);

    @Override
    public PacketBase onMessage(PacketBase message, MessageContext ctx) {
        return message.processAndReply(ctx.side, NetworkHelper.getSidedPlayer(ctx));
    }

    public enum PacketType {

        ConfigSync(PacketConfigSync.class),
        FoodHistory(PacketFoodHistory.class),
        FoodEatenAllTime(PacketFoodEatenAllTime.class),
        ToggleFoodContainer(PacketToggleFoodContainer.class, Side.SERVER),
        FoodGroup(PacketFoodGroup.class),
        NutritionExhaust(PacketNutritionExhaust.class);

        public final Class<? extends PacketBase> packet;

        PacketType(Class<? extends PacketBase> clazz) {
            this(clazz, Side.CLIENT);
        }

        PacketType(Class<? extends PacketBase> clazz, Side side) {
            packet = clazz;
            channel.registerMessage(PacketHandler.class, clazz, ordinal(), side);
        }

        public static int getIdOf(PacketBase packet) {
            for (PacketType packetType : PacketType.values()) {
                if (packetType.packet.isInstance(packet)) return packetType.ordinal();
            }
            return -1;
        }
    }
}
