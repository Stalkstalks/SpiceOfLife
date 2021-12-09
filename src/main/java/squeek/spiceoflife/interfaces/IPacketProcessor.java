package squeek.spiceoflife.interfaces;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import squeek.spiceoflife.network.PacketBase;

public interface IPacketProcessor {
    PacketBase processAndReply(Side side, EntityPlayer player);
}
