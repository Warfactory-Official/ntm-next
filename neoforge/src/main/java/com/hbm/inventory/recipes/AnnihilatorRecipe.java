// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.NuclearTech;
import com.hbm.inventory.recipes.loader.INamedRecipe;
import com.hbm.inventory.recipes.loader.NtmRecipeInput;
import com.hbm.util.DataCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public record AnnihilatorRecipe(Key key, List<Milestone> milestones, List<String> conditions)
        implements Recipe<NtmRecipeInput>, INamedRecipe {

    public static final MapCodec<AnnihilatorRecipe> MAP_CODEC =
            DataCodecs.recipe(
                    RecordCodecBuilder.<AnnihilatorRecipe>mapCodec(
                            i ->
                                    i.group(
                                                    Key.MAP_CODEC.forGetter(AnnihilatorRecipe::key),
                                                    Milestone.CODEC
                                                            .listOf()
                                                            .fieldOf("milestones")
                                                            .forGetter(
                                                                    AnnihilatorRecipe::milestones))
                                            .apply(
                                                    i,
                                                    (key, milestones) ->
                                                            new AnnihilatorRecipe(
                                                                    key, milestones, List.of()))));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Milestone>>
            MILESTONES_STREAM_CODEC = Milestone.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, AnnihilatorRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Key.STREAM_CODEC,
                    AnnihilatorRecipe::key,
                    MILESTONES_STREAM_CODEC,
                    AnnihilatorRecipe::milestones,
                    (key, milestones) -> new AnnihilatorRecipe(key, milestones, List.of()));

    public static final RecipeSerializer<AnnihilatorRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public AnnihilatorRecipe {
        milestones = List.copyOf(milestones);
        conditions = List.copyOf(conditions);
    }

    @Override
    public String getInternalName() {
        if (key.item != null) return "item_" + name(BuiltInRegistries.ITEM.getKey(key.item));
        if (key.tag != null) return "tag_" + name(key.tag.location());
        if (key.fluid != null) return "fluid_" + name(BuiltInRegistries.FLUID.getKey(key.fluid));
        return "stack_" + name(BuiltInRegistries.ITEM.getKey(key.stack.item().value()));
    }

    private static String name(Identifier id) {
        String namespace =
                id.getNamespace().equals(NuclearTech.MOD_ID) ? "" : id.getNamespace() + "_";
        return namespace + id.getPath().replace('/', '_');
    }

    @Override
    public RecipeType<AnnihilatorRecipe> getType() {
        return AnnihilatorRecipes.INSTANCE;
    }

    @Override
    public RecipeSerializer<AnnihilatorRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return INamedRecipe.BOOK_CATEGORY;
    }

    @Override
    public boolean matches(NtmRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(NtmRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    public record Key(
            @Nullable Item item,
            @Nullable TagKey<Item> tag,
            @Nullable Fluid fluid,
            @Nullable ItemStackTemplate stack) {

        private static final Codec<Either<TagKey<Item>, Item>> ITEM_SELECTOR =
                Codec.either(
                        TagKey.hashedCodec(Registries.ITEM), BuiltInRegistries.ITEM.byNameCodec());

        public static final MapCodec<Key> MAP_CODEC =
                RecordCodecBuilder.<Key>mapCodec(
                                i ->
                                        i.group(
                                                        ITEM_SELECTOR
                                                                .optionalFieldOf("item")
                                                                .forGetter(
                                                                        k ->
                                                                                k.tag != null
                                                                                        ? Optional
                                                                                                .of(
                                                                                                        Either
                                                                                                                .left(
                                                                                                                        k.tag))
                                                                                        : Optional
                                                                                                .ofNullable(
                                                                                                        k.item)
                                                                                                .map(
                                                                                                        Either
                                                                                                                ::right)),
                                                        BuiltInRegistries.FLUID
                                                                .byNameCodec()
                                                                .optionalFieldOf("fluid")
                                                                .forGetter(
                                                                        (Key k) ->
                                                                                Optional.ofNullable(
                                                                                        k.fluid)),
                                                        ItemStackTemplate.CODEC
                                                                .optionalFieldOf("stack")
                                                                .forGetter(
                                                                        (Key k) ->
                                                                                Optional.ofNullable(
                                                                                        k.stack)))
                                                .apply(
                                                        i,
                                                        (item, fluid, stack) ->
                                                                new Key(
                                                                        item.flatMap(
                                                                                        value ->
                                                                                                value
                                                                                                        .right())
                                                                                .orElse(null),
                                                                        item.flatMap(
                                                                                        value ->
                                                                                                value
                                                                                                        .left())
                                                                                .orElse(null),
                                                                        fluid.orElse(null),
                                                                        stack.orElse(null))))
                        .validate(Key::validate);

        public static final Codec<Key> CODEC = DataCodecs.strict(MAP_CODEC).codec();

        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Item>>
                ITEM_STREAM_CODEC = ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM));

        private static final StreamCodec<ByteBuf, Optional<TagKey<Item>>> TAG_STREAM_CODEC =
                ByteBufCodecs.optional(
                        Identifier.STREAM_CODEC.map(
                                id -> TagKey.create(Registries.ITEM, id), TagKey::location));

        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Fluid>>
                FLUID_STREAM_CODEC =
                        ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.FLUID));

        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemStackTemplate>>
                STACK_STREAM_CODEC = ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC);

        public static final StreamCodec<RegistryFriendlyByteBuf, Key> STREAM_CODEC =
                StreamCodec.composite(
                        ITEM_STREAM_CODEC,
                        (Key k) -> Optional.ofNullable(k.item),
                        TAG_STREAM_CODEC,
                        (Key k) -> Optional.ofNullable(k.tag),
                        FLUID_STREAM_CODEC,
                        (Key k) -> Optional.ofNullable(k.fluid),
                        STACK_STREAM_CODEC,
                        (Key k) -> Optional.ofNullable(k.stack),
                        (item, tag, fluid, stack) ->
                                new Key(
                                        item.orElse(null),
                                        tag.orElse(null),
                                        fluid.orElse(null),
                                        stack.orElse(null)));

        public static Key of(Item item) {
            return new Key(item, null, null, null);
        }

        public static Key of(TagKey<Item> tag) {
            return new Key(null, tag, null, null);
        }

        public static Key of(Fluid fluid) {
            return new Key(null, null, fluid, null);
        }

        public static Key of(ItemStackTemplate stack) {
            return new Key(null, null, null, stack);
        }

        private static DataResult<Key> validate(Key key) {
            int named =
                    (key.item != null ? 1 : 0)
                            + (key.tag != null ? 1 : 0)
                            + (key.fluid != null ? 1 : 0)
                            + (key.stack != null ? 1 : 0);
            return named == 1
                    ? DataResult.success(key)
                    : DataResult.error(
                            () -> "annihilator needs exactly one of 'item', 'fluid' and 'stack'");
        }

        public Object poolKey() {
            if (item != null) return item;
            if (tag != null) return tag;
            if (fluid != null) return fluid;
            return AnnihilatorRecipes.StackKey.of(stack.create());
        }
    }

    public record Milestone(BigInteger amount, ItemStackTemplate payout) {

        private static final Codec<BigInteger> AMOUNT_CODEC =
                Codec.STRING.comapFlatMap(
                        text -> {
                            try {
                                return DataResult.success(new BigInteger(text));
                            } catch (NumberFormatException e) {
                                return DataResult.error(
                                        () -> "annihilator amount is not an integer: " + text);
                            }
                        },
                        BigInteger::toString);

        private static final StreamCodec<ByteBuf, BigInteger> AMOUNT_STREAM_CODEC =
                ByteBufCodecs.BYTE_ARRAY.map(BigInteger::new, BigInteger::toByteArray);

        public static final Codec<Milestone> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                AMOUNT_CODEC
                                                        .fieldOf("amount")
                                                        .forGetter(Milestone::amount),
                                                ItemStackTemplate.CODEC
                                                        .fieldOf("payout")
                                                        .forGetter(Milestone::payout))
                                        .apply(i, Milestone::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Milestone> STREAM_CODEC =
                StreamCodec.composite(
                        AMOUNT_STREAM_CODEC,
                        Milestone::amount,
                        ItemStackTemplate.STREAM_CODEC,
                        Milestone::payout,
                        Milestone::new);
    }
}
