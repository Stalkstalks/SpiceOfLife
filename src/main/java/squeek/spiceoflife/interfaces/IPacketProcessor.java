package squeek.spiceoflife.interfaces;

import net.minecraft.entity.player.EntityPlayer;

import squeek.spiceoflife.network.PacketBase;
import cpw.mods.fml.relauncher.Side;

public interface IPacketProcessor {

    PacketBase processAndReply(Side side, EntityPlayer player);
}
