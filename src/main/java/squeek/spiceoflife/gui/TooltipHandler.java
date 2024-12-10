package squeek.spiceoflife.gui;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.helpers.ColorHelper;
import squeek.spiceoflife.helpers.FoodHelper;
import squeek.spiceoflife.helpers.KeyHelper;
import squeek.spiceoflife.helpers.StringHelper;

@SideOnly(Side.CLIENT)
public class TooltipHandler {

    private static final DecimalFormat df = new DecimalFormat("##.##");
    private static final FoodGroupComparator foodGroupComparator = new FoodGroupComparator();

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event == null || event.entityPlayer == null
            || event.itemStack == null
            || !FoodHelper.isValidFood(event.itemStack)) return;

        int totalFoodEaten = FoodHistory.get(event.entityPlayer).totalFoodsEatenAllTime;
        List<String> toolTipStringsToAdd = new ArrayList<>();
        Set<FoodGroup> foodGroups = FoodGroupRegistry.getFoodGroupsForFood(event.itemStack);
        Set<FoodGroup> visibleFoodGroups = getFoodGroupsForDisplay(foodGroups);

        if (ModConfig.FOOD_MODIFIER_ENABLED && !visibleFoodGroups.isEmpty()) {
            String foodGroupString = visibleFoodGroups.size() > 1
                ? StatCollector.translateToLocal("spiceoflife.tooltip.food.groups")
                : StatCollector.translateToLocal("spiceoflife.tooltip.food.group");
            String joinedFoodGroups = joinFoodGroupsForDisplay(
                visibleFoodGroups,
                ", ",
                EnumChatFormatting.GRAY.toString());
            toolTipStringsToAdd.add(
                EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.ITALIC
                    + foodGroupString
                    + EnumChatFormatting.GRAY
                    + EnumChatFormatting.ITALIC
                    + joinedFoodGroups);
        }
        if (ModConfig.FOOD_EATEN_THRESHOLD > 0 && totalFoodEaten < ModConfig.FOOD_EATEN_THRESHOLD) {
            if (ModConfig.FOOD_MODIFIER_ENABLED) {
                int timesUntilMeetsThreshold = ModConfig.FOOD_EATEN_THRESHOLD - totalFoodEaten;
                toolTipStringsToAdd.add(
                    EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.ITALIC
                        + StatCollector.translateToLocal("spiceoflife.tooltip.food.until.enabled.1"));
                toolTipStringsToAdd.add(
                    EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.ITALIC
                        + StatCollector.translateToLocalFormatted(
                            "spiceoflife.tooltip.food.until.enabled.2",
                            timesUntilMeetsThreshold,
                            timesUntilMeetsThreshold == 1
                                ? StatCollector.translateToLocal("spiceoflife.tooltip.times.singular")
                                : StatCollector.translateToLocal("spiceoflife.tooltip.times.plural")));
            }
        } else {
            FoodHistory foodHistory = FoodHistory.get(event.entityPlayer);
            float foodModifier = FoodModifier.getFoodModifier(foodHistory, event.itemStack);
            FoodValues foodValues = FoodValues.get(event.itemStack, event.entityPlayer);
            boolean foodOrItsFoodGroupsEatenRecently = foodHistory.containsFoodOrItsFoodGroups(event.itemStack);

            if (FoodHelper.canFoodDiminish(event.itemStack) && (foodOrItsFoodGroupsEatenRecently || foodModifier != 1))
                toolTipStringsToAdd.add(
                    0,
                    EnumChatFormatting.GRAY + StatCollector.translateToLocal("spiceoflife.tooltip.nutritional.value")
                        + getNutritionalValueString(foodModifier)
                        + (foodValues.hunger == 0 && foodModifier != 0f ? EnumChatFormatting.DARK_RED + " ("
                            + foodValues.hunger
                            + " "
                            + StatCollector.translateToLocal("spiceoflife.tooltip.hunger")
                            + ")" : ""));

            boolean shouldShowPressShift = visibleFoodGroups.size() > 1 && !KeyHelper.isShiftKeyDown();
            boolean shouldShowFoodGroupDetails = visibleFoodGroups.size() <= 1 || KeyHelper.isShiftKeyDown();
            String bulletPoint = EnumChatFormatting.DARK_GRAY + "- " + EnumChatFormatting.GRAY;

            if (shouldShowPressShift) toolTipStringsToAdd.add(
                bulletPoint + EnumChatFormatting.DARK_GRAY
                    + StatCollector.translateToLocalFormatted(
                        "spiceoflife.tooltip.hold.key.for.details",
                        EnumChatFormatting.YELLOW.toString() + EnumChatFormatting.ITALIC
                            + "Shift"
                            + EnumChatFormatting.RESET
                            + EnumChatFormatting.DARK_GRAY));

            if (shouldShowFoodGroupDetails) {
                toolTipStringsToAdd.add(getFullHistoryToolTip(foodHistory, event.itemStack));
                int foodGroupsToShow = Math.max(1, visibleFoodGroups.size());
                FoodGroup[] visibleFoodGroupsArray = visibleFoodGroups.toArray(new FoodGroup[visibleFoodGroups.size()]);

                for (int i = 0; i < foodGroupsToShow; i++) {
                    FoodGroup foodGroup = i < visibleFoodGroupsArray.length ? visibleFoodGroupsArray[i] : null;
                    boolean shouldShowNutritionalValue = foodGroupsToShow > 0;
                    String prefix = (foodGroupsToShow > 1 ? bulletPoint : "");
                    toolTipStringsToAdd.add(
                        prefix + getEatenRecentlyTooltip(
                            foodHistory,
                            event.itemStack,
                            foodGroup,
                            shouldShowNutritionalValue));
                }
            }
        }

        event.toolTip.addAll(toolTipStringsToAdd);
    }

    public static Set<FoodGroup> getFoodGroupsForDisplay(Set<FoodGroup> foodGroups) {
        Set<FoodGroup> visibleFoodGroups = new TreeSet<>(foodGroupComparator);
        for (FoodGroup foodGroup : foodGroups) {
            if (!foodGroup.hidden()) visibleFoodGroups.add(foodGroup);
        }
        return visibleFoodGroups;
    }

    public static String joinFoodGroupsForDisplay(Set<FoodGroup> foodGroups, String delimiter, String resetFormatting) {
        List<String> stringsToJoin = new ArrayList<>();
        for (FoodGroup foodGroup : foodGroups) {
            stringsToJoin
                .add(foodGroup.formatString(EnumChatFormatting.ITALIC.toString() + foodGroup) + resetFormatting);
        }
        return StringHelper.join(stringsToJoin, delimiter);
    }

    public String getNutritionalValueString(float foodModifier) {
        return ColorHelper.getNutrientColor(foodModifier) + df.format(foodModifier * 100f) + "%";
    }

    public String getEatenRecentlyTooltip(FoodHistory foodHistory, ItemStack itemStack, FoodGroup foodGroup,
        boolean shouldShowNutritionalValue) {
        final int count = foodHistory.getFoodCountForFoodGroup(itemStack, foodGroup);
        final String prefix = (foodGroup != null
            ? foodGroup.formatString(EnumChatFormatting.DARK_AQUA.toString() + foodGroup) + " "
            : "") + EnumChatFormatting.RESET.toString();
        final String eatenRecently;
        final String nutritionalValue = shouldShowNutritionalValue
            ? EnumChatFormatting.DARK_GRAY + " ["
                + ColorHelper.getNutrientColor(foodHistory.getFoodGroupsPercentage(foodGroup.identifier))
                + foodHistory.getFoodGroupsPercentage(foodGroup.identifier)
                + EnumChatFormatting.DARK_GRAY
                + "%]"
            : "";
        if (count > 0) {
            if (ModConfig.USE_HUNGER_QUEUE) {
                eatenRecently = StatCollector.translateToLocalFormatted(
                    "spiceoflife.tooltip.eaten.recently.hunger",
                    StringHelper.getQuantityDescriptor(count),
                    StringHelper.hungerHistoryLength(ModConfig.FOOD_HISTORY_LENGTH));
            } else {
                eatenRecently = StatCollector.translateToLocalFormatted(
                    "spiceoflife.tooltip.eaten.recently",
                    StringHelper.getQuantityDescriptor(count),
                    ModConfig.FOOD_HISTORY_LENGTH);
            }
        } else eatenRecently = StatCollector.translateToLocal("spiceoflife.tooltip.not.eaten.recently");
        return prefix + (foodGroup != null ? StringHelper.decapitalize(eatenRecently, StringHelper.getMinecraftLocale())
            : eatenRecently) + nutritionalValue;
    }

    public String getFullHistoryToolTip(FoodHistory foodHistory, ItemStack itemStack) {
        final String prefix = "Extra Hearts: " + EnumChatFormatting.DARK_AQUA.toString()
            + EnumChatFormatting.ITALIC.toString();
        if (!foodHistory.hasEverEaten(itemStack)) {
            return prefix + StatCollector.translateToLocal("spiceoflife.tooltip.not.eaten.ever");
        } else {
            // TODO: Cheap foods don't count
            return prefix + StatCollector.translateToLocal("spiceoflife.tooltip.eaten.ever");
        }
    }

    static class FoodGroupComparator implements Comparator<FoodGroup>, Serializable {

        private static final long serialVersionUID = -4556648064321616158L;

        @Override
        public int compare(FoodGroup a, FoodGroup b) {
            return a.getLocalizedName()
                .compareToIgnoreCase(b.getLocalizedName());
        }
    }
}
