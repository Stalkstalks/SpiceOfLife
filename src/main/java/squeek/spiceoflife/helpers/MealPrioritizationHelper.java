package squeek.spiceoflife.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodModifier;

public class MealPrioritizationHelper {

    public static final Comparator<InventoryFoodInfo> hungerComparator = (a,
        b) -> integerCompare(a.modifiedFoodValues.hunger, b.modifiedFoodValues.hunger);
    public static final Comparator<InventoryFoodInfo> diminishedComparator = (a, b) -> Float
        .compare(b.diminishingReturnsModifier, a.diminishingReturnsModifier);

    public static int findBestFoodForPlayerToEat(EntityPlayer player, IInventory inventory) {
        List<InventoryFoodInfo> allFoodInfo = getFoodInfoFromInventoryForPlayer(player, inventory);
        FoodHistory foodHistory = FoodHistory.get(player);

        int bestIndex = 0;
        for (int i = 0; i < allFoodInfo.size(); i++) {
            if (FoodModifier.getNutrientGain(foodHistory, allFoodInfo.get(i).itemStack)
                > FoodModifier.getNutrientGain(foodHistory, allFoodInfo.get(bestIndex).itemStack)) bestIndex = i;
        }

        return allFoodInfo.get(bestIndex).slotNum;

    }

    public static List<InventoryFoodInfo> getFoodInfoFromInventoryForPlayer(EntityPlayer player, IInventory inventory) {
        List<InventoryFoodInfo> foodInfo = new ArrayList<>();

        for (int slotNum = 0; slotNum < inventory.getSizeInventory(); slotNum++) {
            ItemStack stackInSlot = inventory.getStackInSlot(slotNum);
            if (stackInSlot == null) continue;
            if (FoodHelper.isFood(stackInSlot)) foodInfo.add(new InventoryFoodInfo(slotNum, stackInSlot, player));
        }

        return foodInfo;
    }

    public static List<InventoryFoodInfo> findBestFoodsForPlayerAccountingForVariety(EntityPlayer player,
        IInventory inventory, int limit) {
        List<InventoryFoodInfo> bestFoods = findBestFoodsForPlayerAccountingForVariety(player, inventory);
        if (bestFoods.size() > limit) bestFoods = bestFoods.subList(0, limit);
        return bestFoods;
    }

    public static List<InventoryFoodInfo> findBestFoodsForPlayerAccountingForVariety(EntityPlayer player,
        IInventory inventory) {
        List<InventoryFoodInfo> allFoodInfo = getFoodInfoFromInventoryForPlayer(player, inventory);
        Collections.shuffle(allFoodInfo);
        allFoodInfo.sort(diminishedComparator);
        return allFoodInfo;
    }

    public static List<List<InventoryFoodInfo>> stratifyFoodsByHunger(List<InventoryFoodInfo> allFoods) {
        List<List<InventoryFoodInfo>> stratifiedFoods = new ArrayList<>();
        if (allFoods.size() > 0) {
            allFoods.sort(hungerComparator);
            int strataHunger = allFoods.get(0).modifiedFoodValues.hunger;
            List<InventoryFoodInfo> currentStrata = new ArrayList<>();
            for (InventoryFoodInfo foodInfo : allFoods) {
                if (foodInfo.modifiedFoodValues.hunger != strataHunger) {
                    stratifiedFoods.add(currentStrata);
                    currentStrata = new ArrayList<>();
                    strataHunger = foodInfo.modifiedFoodValues.hunger;
                }
                currentStrata.add(foodInfo);
            }
            stratifiedFoods.add(currentStrata);
        }
        return stratifiedFoods;
    }

    private static int integerCompare(int a, int b) {
        return Integer.compare(a, b);
    }

    public static class InventoryFoodInfo {

        public ItemStack itemStack;
        public FoodValues defaultFoodValues;
        public float diminishingReturnsModifier = 1;
        public FoodValues modifiedFoodValues;
        public int slotNum;

        public InventoryFoodInfo() {}

        public InventoryFoodInfo(int slotNum, ItemStack itemStack, EntityPlayer player) {
            this.itemStack = itemStack;
            this.slotNum = slotNum;
            this.defaultFoodValues = FoodValues.get(this.itemStack);
            if (FoodHelper.canFoodDiminish(this.itemStack)) {
                this.diminishingReturnsModifier = FoodModifier.getFoodModifier(player, itemStack);
                this.modifiedFoodValues = FoodModifier
                    .getModifiedFoodValues(defaultFoodValues, diminishingReturnsModifier);
            } else {
                this.diminishingReturnsModifier = Float.NaN;
                this.modifiedFoodValues = defaultFoodValues;
            }
        }
    }

    public static class FoodInfoComparator implements Comparator<InventoryFoodInfo>, Serializable {

        private static final long serialVersionUID = -2142369827782900207L;
        public int maxHungerRestored;
        public boolean ignoreHungerRemainder = false;

        public FoodInfoComparator() {
            ignoreHungerRemainder = true;
        }

        public FoodInfoComparator(int maxHungerRestored) {
            this.maxHungerRestored = maxHungerRestored;
        }

        @Override
        public int compare(InventoryFoodInfo a, InventoryFoodInfo b) {
            // undiminished over diminished
            int compareResult = Float.compare(b.diminishingReturnsModifier, a.diminishingReturnsModifier);
            // restore to full over leaving a remainder
            if (compareResult == 0 && !ignoreHungerRemainder) {
                int aRemainder = maxHungerRestored - a.modifiedFoodValues.hunger;
                int bRemainder = maxHungerRestored - b.modifiedFoodValues.hunger;
                compareResult = integerCompare(Math.abs(aRemainder), Math.abs(bRemainder));
                if (compareResult == 0 && aRemainder != bRemainder) {
                    // too low over too high
                    compareResult = bRemainder > aRemainder ? 1 : -1;
                }
            }
            // better food over worse food
            if (compareResult == 0) compareResult = Float.compare(
                b.modifiedFoodValues.saturationModifier * b.modifiedFoodValues.hunger,
                a.modifiedFoodValues.saturationModifier * a.modifiedFoodValues.hunger);

            return compareResult;
        }
    }
}
