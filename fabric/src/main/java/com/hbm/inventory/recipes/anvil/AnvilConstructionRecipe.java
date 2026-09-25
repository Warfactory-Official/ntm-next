// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.anvil;

import com.hbm.config.BalanceConfig;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AnvilConstructionRecipe extends GenericRecipe {

    private static final Codec<OverlayType> OVERLAY_CODEC =
            Codec.STRING.xmap(
                    name -> OverlayType.valueOf(name.toUpperCase(Locale.ROOT)),
                    overlay -> overlay.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<ByteBuf, OverlayType> OVERLAY_STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> OverlayType.values()[id], Enum::ordinal);
    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .optionalFieldOf("tier", 0)
                                                    .forGetter(Extras::tierLower),
                                            Codec.INT
                                                    .optionalFieldOf("tier_upper", -1)
                                                    .forGetter(Extras::tierUpper),
                                            OVERLAY_CODEC
                                                    .fieldOf("overlay")
                                                    .forGetter(Extras::overlay))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    Extras::tierLower,
                    ByteBufCodecs.INT,
                    Extras::tierUpper,
                    OVERLAY_STREAM_CODEC,
                    Extras::overlay,
                    Extras::new);
    private static final BiConsumer<AnvilConstructionRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.tierLower = extras.tierLower();
                recipe.tierUpper = extras.tierUpper();
                recipe.overlay = extras.overlay();
            };

    private static final BiConsumer<AnvilConstructionRecipe, Extras> APPLY_DATAPACK_EXTRAS =
            (recipe, extras) -> {
                APPLY_EXTRAS.accept(recipe, extras);
                boolean unlock = BalanceConfig.enableLBSM && BalanceConfig.enableLBSMUnlockAnvil;
                recipe.tierLower = unlockedTier(recipe.tierLower, unlock);
                recipe.tierUpper =
                        recipe.tierUpper == -1 ? -1 : unlockedTier(recipe.tierUpper, unlock);
            };
    private static final Function<AnvilConstructionRecipe, Extras> EXTRACT_EXTRAS =
            recipe -> new Extras(recipe.tierLower, recipe.tierUpper, recipe.overlay);
    public static final MapCodec<AnvilConstructionRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    AnvilConstructionRecipe::new,
                    EXTRAS_MAP_CODEC,
                    APPLY_DATAPACK_EXTRAS,
                    EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilConstructionRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    AnvilConstructionRecipe::new,
                    EXTRAS_STREAM_CODEC,
                    APPLY_EXTRAS,
                    EXTRACT_EXTRAS);
    public static final RecipeSerializer<AnvilConstructionRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public int tierLower;
    public int tierUpper = -1;
    private OverlayType overlay = OverlayType.NONE;
    private List<CountIngredient> inputs;
    private List<AnvilOutput> outputs;

    public AnvilConstructionRecipe(String name) {
        super(name);
    }

    public static ItemStack representativeStack(CountIngredient ci) {
        List<ItemStack> stacks = ci.displayStacks();
        if (stacks.isEmpty()) return ItemStack.EMPTY;
        return stacks.get((int) (Math.abs(System.currentTimeMillis() / 1000) % stacks.size()));
    }

    public static int unlockedTier(int tier, boolean unlock) {
        return unlock ? 1 : tier;
    }

    public AnvilConstructionRecipe setTier(int tier) {
        this.tierLower = tier;
        return this;
    }

    public AnvilConstructionRecipe setTierRange(int lower, int upper) {
        this.tierLower = lower;
        this.tierUpper = upper;
        return this;
    }

    public boolean isTierValid(int tier) {
        if (tierUpper == -1) return tier >= tierLower;
        return tier >= tierLower && tier <= tierUpper;
    }

    public OverlayType getOverlay() {
        return overlay;
    }

    public AnvilConstructionRecipe setOverlay(OverlayType overlay) {
        this.overlay = overlay;
        return this;
    }

    public List<CountIngredient> input() {
        if (inputs == null) inputs = inputItem == null ? List.of() : Arrays.asList(inputItem);
        return inputs;
    }

    public List<AnvilOutput> outputs() {
        if (outputs == null) {

            WeightedList<ItemStack>[] slots = outputItems();
            List<AnvilOutput> built = new ArrayList<>();
            if (slots != null) {
                for (WeightedList<ItemStack> slot : slots) {
                    int total = 0;
                    for (Weighted<ItemStack> entry : slot.unwrap()) total += entry.weight();
                    for (Weighted<ItemStack> entry : slot.unwrap()) {
                        if (entry.value().isEmpty()) continue;
                        built.add(
                                new AnvilOutput(
                                        entry.value(),
                                        total == 0 ? 1F : (float) entry.weight() / (float) total));
                    }
                }
            }
            outputs = List.copyOf(built);
        }
        return outputs;
    }

    @Override
    public void materialize() {
        super.materialize();
        outputs = null;
        inputs = null;
    }

    public ItemStack getDisplay() {
        if (overlay == OverlayType.RECYCLING && !input().isEmpty()) {
            return representativeStack(input().get(0));
        }
        return outputs().isEmpty() ? ItemStack.EMPTY : outputs().get(0).stack();
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return AnvilConstructionRecipes.INSTANCE;
    }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }

    public record AnvilOutput(ItemStack stack, float chance) {
        public AnvilOutput(ItemStack stack) {
            this(stack, 1F);
        }
    }

    private record Extras(int tierLower, int tierUpper, OverlayType overlay) {}
}
