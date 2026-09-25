// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FurnaceRecipesUsed {

    private static final String KEY = "RecipesUsed";

    private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> LANE_CODEC =
            Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);

    private static final Codec<List<Map<ResourceKey<Recipe<?>>, Integer>>> CODEC =
            LANE_CODEC.listOf();

    private final int[] outputSlots;
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>>[] lanes;

    @SuppressWarnings("unchecked")
    public FurnaceRecipesUsed(int... outputSlots) {
        this.outputSlots = outputSlots;
        this.lanes =
                (Reference2IntOpenHashMap<ResourceKey<Recipe<?>>>[])
                        new Reference2IntOpenHashMap<?>[outputSlots.length];
        for (int i = 0; i < outputSlots.length; i++)
            this.lanes[i] = new Reference2IntOpenHashMap<>();
    }

    private static List<RecipeHolder<?>> drain(
            ServerLevel level,
            Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> pending,
            Vec3 position) {
        List<RecipeHolder<?>> resolved = new ArrayList<>(pending.size());
        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry :
                pending.reference2IntEntrySet()) {
            level.recipeAccess()
                    .byKey(entry.getKey())
                    .ifPresent(
                            recipe -> {
                                resolved.add(recipe);

                                if (recipe.value() instanceof AbstractCookingRecipe cooking) {
                                    createExperience(
                                            level,
                                            position,
                                            entry.getIntValue(),
                                            cooking.experience());
                                }
                            });
        }
        return resolved;
    }

    private static void createExperience(
            ServerLevel level, Vec3 position, int amount, float value) {
        int xpReward = Mth.floor(amount * value);
        float xpFraction = Mth.frac(amount * value);
        if (xpFraction != 0.0F && level.getRandom().nextFloat() < xpFraction) xpReward++;
        ExperienceOrb.award(level, position, xpReward);
    }

    public void record(int lane, @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (recipe != null) lanes[lane].addTo(recipe.id(), 1);
    }

    public boolean isEmpty(int lane) {
        return lanes[lane].isEmpty();
    }

    public int laneCount() {
        return lanes.length;
    }

    public int laneOf(int slot) {
        for (int lane = 0; lane < outputSlots.length; lane++)
            if (outputSlots[lane] == slot) return lane;
        return -1;
    }

    public boolean award(ServerPlayer player, int slot, List<ItemStack> ingredients) {
        int lane = laneOf(slot);
        if (lane < 0 || lanes[lane].isEmpty() || !(player.level() instanceof ServerLevel level))
            return false;
        List<RecipeHolder<?>> toAward = drain(level, lanes[lane], player.position());
        player.awardRecipes(toAward);
        for (RecipeHolder<?> recipe : toAward) player.triggerRecipeCrafted(recipe, ingredients);
        lanes[lane].clear();
        return true;
    }

    public void popExperience(ServerLevel level, BlockPos pos) {
        Vec3 position = Vec3.atCenterOf(pos);
        for (Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> lane : lanes)
            drain(level, lane, position);
    }

    public void save(ValueOutput output) {
        List<Map<ResourceKey<Recipe<?>>, Integer>> stored = new ArrayList<>(lanes.length);
        boolean any = false;
        for (Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> lane : lanes) {
            stored.add(lane);
            any |= !lane.isEmpty();
        }
        if (any) output.store(KEY, CODEC, stored);
    }

    public void load(ValueInput input) {
        for (Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> lane : lanes) lane.clear();
        List<Map<ResourceKey<Recipe<?>>, Integer>> stored =
                input.read(KEY, CODEC).orElse(List.of());
        for (int i = 0; i < Math.min(stored.size(), lanes.length); i++)
            lanes[i].putAll(stored.get(i));
    }
}
