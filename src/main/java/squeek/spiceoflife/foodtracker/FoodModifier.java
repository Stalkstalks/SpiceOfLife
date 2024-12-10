package squeek.spiceoflife.foodtracker;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.food.FoodEvent;
import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.helpers.FoodHelper;
import squeek.spiceoflife.items.ItemFoodContainer;

public class FoodModifier {

    public static final FoodModifier GLOBAL = new FoodModifier();
    // public Expression expression;

    public FoodModifier() {
        this(ModConfig.FOOD_MODIFIER_FORMULA);
    }

    public FoodModifier(String formula) {
        setFormula(formula);
    }

    public void setFormula(String formula) {
        // expression = new Expression(formula);
    }

    public static void onGlobalFormulaChanged() {
        FoodModifier.GLOBAL.setFormula(ModConfig.FOOD_MODIFIER_FORMULA);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void getFoodValues(FoodEvent.GetPlayerFoodValues event) {
        if (ModConfig.FOOD_MODIFIER_ENABLED) {
            ItemStack actualFood = event.food;
            if (FoodHelper.isFoodContainer(event.food)) {
                actualFood = ((ItemFoodContainer) event.food.getItem())
                    .getBestFoodForPlayerToEat(event.food, event.player);
            }

            if (actualFood != null) {
                float modifier = FoodModifier.getFoodModifier(event.player, actualFood);

                event.foodValues = FoodModifier.getModifiedFoodValues(event.foodValues, modifier);
            }
        }
    }

    public static float getFoodModifier(EntityPlayer player, ItemStack food) {
        return getFoodModifier(FoodHistory.get(player), food);
    }

    public static FoodValues getModifiedFoodValues(FoodValues foodValues, float modifier) {
        int hunger = foodValues.hunger;
        float saturationModifier = foodValues.saturationModifier;

        if (ModConfig.AFFECT_FOOD_HUNGER_VALUES) {
            if (hunger < 0 && ModConfig.AFFECT_NEGATIVE_FOOD_HUNGER_VALUES)
                hunger = (int) ModConfig.FOOD_HUNGER_ROUNDING_MODE.round(hunger * (2 - modifier));
            else if (hunger > 0) hunger = (int) ModConfig.FOOD_HUNGER_ROUNDING_MODE.round(hunger * modifier);
        }

        if (ModConfig.AFFECT_FOOD_SATURATION_MODIFIERS) {
            if (saturationModifier < 0 && ModConfig.AFFECT_NEGATIVE_FOOD_SATURATION_MODIFIERS)
                saturationModifier *= 2 - modifier;
            else if (saturationModifier > 0) saturationModifier *= modifier;
        }

        return new FoodValues(hunger, saturationModifier);
    }

    public static float getFoodModifier(FoodHistory foodHistory, ItemStack food) {
        if (!ModConfig.FOOD_MODIFIER_ENABLED) return 1f;

        if (!FoodHelper.canFoodDiminish(food)) return 1f;

        if (ModConfig.FOOD_EATEN_THRESHOLD > 0 && foodHistory.totalFoodsEatenAllTime < ModConfig.FOOD_EATEN_THRESHOLD)
            return 1f;

        // if this food has mutliple food groups, calculate the modifier for each individually
        // and then take the average
        // for foods with <= 1 food group, this just means dividing the modifier by 1
        FoodGroup[] foodGroups = FoodGroupRegistry.getFoodGroupsForFood(food)
            .toArray(new FoodGroup[0]);
        int numIterations = Math.max(1, foodGroups.length);
        float modifierSum = 0f;

        for (int i = 0; i < numIterations; i++) {
            FoodGroup foodGroup = i < foodGroups.length ? foodGroups[i] : null;
            modifierSum += getFoodGroupModifier(foodHistory, food, foodGroup);
        }

        return modifierSum / numIterations;
    }

    public static float getFoodGroupModifier(FoodHistory foodHistory, ItemStack food, FoodGroup foodGroup) {
        FoodModifier effectiveFoodModifier = foodGroup != null ? foodGroup.getFoodModifier() : FoodModifier.GLOBAL;
        int count = foodHistory.getFoodCountForFoodGroup(food, foodGroup);
        int historySize = foodHistory.getHistoryLengthInRelevantUnits();
        FoodValues totalFoodValues = foodHistory.getTotalFoodValuesForFoodGroup(food, foodGroup);
        FoodValues foodValues = FoodValues.get(food);
        /*
         * if (foodValues != null) {
         * BigDecimal result = effectiveFoodModifier.expression.with("count", new BigDecimal(count))
         * .and("cur_history_length", new BigDecimal(historySize))
         * .and("food_hunger_value", new BigDecimal(foodValues.hunger))
         * .and("food_saturation_mod", new BigDecimal(foodValues.saturationModifier))
         * .and(
         * "cur_hunger",
         * new BigDecimal(
         * foodHistory.player.getFoodStats()
         * .getFoodLevel()))
         * .and(
         * "cur_saturation",
         * new BigDecimal(
         * foodHistory.player.getFoodStats()
         * .getSaturationLevel()))
         * .and("total_food_eaten", new BigDecimal(foodHistory.totalFoodsEatenAllTime))
         * .and("max_history_length", new BigDecimal(ModConfig.FOOD_HISTORY_LENGTH))
         * .and("hunger_count", new BigDecimal(totalFoodValues.hunger))
         * .and("saturation_count", new BigDecimal(totalFoodValues.saturationModifier))
         * .and(
         * "food_group_count",
         * new BigDecimal(
         * FoodGroupRegistry.getFoodGroupsForFood(food)
         * .size()))
         * .and(
         * "distinct_food_groups_eaten",
         * new BigDecimal(
         * foodHistory.getDistinctFoodGroups()
         * .size()))
         * .and("total_food_groups", new BigDecimal(FoodGroupRegistry.numFoodGroups()))
         * .and("exact_count", new BigDecimal(foodHistory.getFoodCountIgnoringFoodGroups(food)))
         * .eval();
         * return result.floatValue();
         * }
         */
        return 1.0f;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void getFoodEatingSpeed(PlayerUseItemEvent.Start event) {
        if (ModConfig.FOOD_EATING_SPEED_MODIFIER > 0 && AppleCoreAPI.accessor.isFood(event.item)) {
            ItemStack actualFood = event.item;
            if (FoodHelper.isFoodContainer(event.item)) {
                actualFood = ((ItemFoodContainer) event.item.getItem())
                    .getBestFoodForPlayerToEat(event.item, event.entityPlayer);
            }

            if (actualFood != null) {
                float nutritionalValue = FoodModifier.getFoodModifier(event.entityPlayer, actualFood);
                float denominator = (float) Math.pow(nutritionalValue, ModConfig.FOOD_EATING_SPEED_MODIFIER);

                if (denominator > 0) event.duration = (int) (event.duration / denominator);
                else event.duration = Short.MAX_VALUE;

                if (ModConfig.FOOD_EATING_DURATION_MAX > 0)
                    event.duration = Math.min(event.duration, ModConfig.FOOD_EATING_DURATION_MAX);
            }
        }
    }

    public static float getNutrientGain(FoodHistory foodHistory, ItemStack food) {
        // if this food has mutliple food groups, calculate the modifier for each individually
        // and then take the average
        // for foods with <= 1 food group, this just means dividing the modifier by 1
        FoodGroup[] foodGroups = FoodGroupRegistry.getFoodGroupsForFood(food)
            .toArray(new FoodGroup[0]);
        int numIterations = Math.max(0, foodGroups.length);
        float nutrientSum = 0f;
        float foodNutrients = FoodValues.get(food).hunger * 0.05f;

        for (int i = 0; i < numIterations; i++) {
            FoodGroup foodGroup = i < foodGroups.length ? foodGroups[i] : null;

            String groupName = foodGroup.identifier;
            int points = 0;

            if (foodHistory.foodGroupPoints.get(groupName) != null) points = foodHistory.foodGroupPoints.get(groupName);

            double proportion = (675 - points) * 3 / 500.0;

            int gain = (int) Math.round(
                (FoodValues.get(food).hunger + Math.ceil(
                    FoodValues.get(food)
                        .getSaturationIncrement()))
                    * proportion);

            // float current = foodHistory.getFoodGroupsPercentage(foodGroup.identifier);
            // float gain = Math.min(1-current,foodNutrients);
            nutrientSum += gain;
        }

        return nutrientSum;
    }
}
