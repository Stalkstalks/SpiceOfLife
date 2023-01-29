package squeek.spiceoflife.foodtracker.foodgroups;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import squeek.spiceoflife.compat.IByteIO;
import squeek.spiceoflife.interfaces.IPackable;

public class FoodGroupMember implements IPackable {

    String oredictName = null;
    ItemStack itemStack = null;

    /*
     * Constructors
     */
    public FoodGroupMember() {}

    public FoodGroupMember(String oredictName) {
        this.oredictName = oredictName;
    }

    public FoodGroupMember(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public List<ItemStack> getBaseItemList() {
        return itemStack != null ? Collections.singletonList(itemStack) : OreDictionary.getOres(oredictName);
    }

    /*
     * Packet handling
     */
    @Override
    public void pack(IByteIO data) {
        data.writeItemStack(itemStack);
        data.writeUTF(oredictName != null ? oredictName : "");
    }

    @Override
    public void unpack(IByteIO data) {
        itemStack = data.readItemStack();
        oredictName = data.readUTF();
        oredictName = !oredictName.equals("") ? oredictName : null;
    }
}
