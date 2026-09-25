// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class GasCentrifugeRecipe extends GenericRecipe {

    private static final MapCodec<Extras> EXTRAS_MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.STRING.fieldOf("stage").forGetter(Extras::stage),
                                            Codec.INT
                                                    .optionalFieldOf("consumed", 0)
                                                    .forGetter(Extras::consumed),
                                            Codec.INT
                                                    .optionalFieldOf("produced", 0)
                                                    .forGetter(Extras::produced),
                                            Codec.STRING
                                                    .optionalFieldOf("next")
                                                    .forGetter(Extras::next),
                                            Codec.BOOL
                                                    .optionalFieldOf("requires_upgrade", false)
                                                    .forGetter(Extras::requiresUpgrade),
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .optionalFieldOf("feed")
                                                    .forGetter(Extras::feed),
                                            Codec.INT
                                                    .optionalFieldOf("dead_end_volume", 0)
                                                    .forGetter(Extras::deadEndVolume),
                                            ItemStackTemplate.MAP_CODEC
                                                    .codec()
                                                    .listOf()
                                                    .optionalFieldOf("dead_end_items", List.of())
                                                    .forGetter(Extras::deadEndItems))
                                    .apply(i, Extras::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Extras> EXTRAS_STREAM_CODEC =
            StreamCodec.of(
                    (buf, extras) -> {
                        buf.writeUtf(extras.stage());
                        buf.writeVarInt(extras.consumed());
                        buf.writeVarInt(extras.produced());
                        buf.writeOptional(extras.next(), (b, s) -> b.writeUtf(s));
                        buf.writeBoolean(extras.requiresUpgrade());
                        buf.writeOptional(
                                extras.feed(),
                                (b, f) ->
                                        ByteBufCodecs.registry(Registries.FLUID)
                                                .encode((RegistryFriendlyByteBuf) b, f));
                        buf.writeVarInt(extras.deadEndVolume());
                        buf.writeCollection(
                                extras.deadEndItems(),
                                (b, t) ->
                                        ItemStackTemplate.STREAM_CODEC.encode(
                                                (RegistryFriendlyByteBuf) b, t));
                    },
                    buf ->
                            new Extras(
                                    buf.readUtf(),
                                    buf.readVarInt(),
                                    buf.readVarInt(),
                                    buf.readOptional(b -> b.readUtf()),
                                    buf.readBoolean(),
                                    buf.readOptional(
                                            b ->
                                                    ByteBufCodecs.registry(Registries.FLUID)
                                                            .decode((RegistryFriendlyByteBuf) b)),
                                    buf.readVarInt(),
                                    buf.readList(
                                            b ->
                                                    ItemStackTemplate.STREAM_CODEC.decode(
                                                            (RegistryFriendlyByteBuf) b))));
    private static final BiConsumer<GasCentrifugeRecipe, Extras> APPLY_EXTRAS =
            (recipe, extras) -> {
                recipe.stage = extras.stage();
                recipe.consumed = extras.consumed();
                recipe.produced = extras.produced();
                recipe.next = extras.next().orElse(null);
                recipe.requiresUpgrade = extras.requiresUpgrade();
                recipe.feed = extras.feed().orElse(null);
                recipe.deadEndVolume = extras.deadEndVolume();
                recipe.deadEndTemplate = extras.deadEndItems();
                recipe.deadEndItems = null;
            };
    private static final Function<GasCentrifugeRecipe, Extras> EXTRACT_EXTRAS =
            recipe ->
                    new Extras(
                            recipe.stage,
                            recipe.consumed,
                            recipe.produced,
                            Optional.ofNullable(recipe.next),
                            recipe.requiresUpgrade,
                            Optional.ofNullable(recipe.feed),
                            recipe.deadEndVolume,
                            recipe.deadEndTemplate);
    public static final MapCodec<GasCentrifugeRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(
                    GasCentrifugeRecipe::new, EXTRAS_MAP_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCentrifugeRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    GasCentrifugeRecipe::new, EXTRAS_STREAM_CODEC, APPLY_EXTRAS, EXTRACT_EXTRAS);
    public static final RecipeSerializer<GasCentrifugeRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public String stage = "";
    public int consumed;

    public int produced;

    public @Nullable String next;

    public boolean requiresUpgrade;
    public @Nullable Fluid feed;

    public int deadEndVolume;
    private List<ItemStackTemplate> deadEndTemplate = List.of();
    private volatile ItemStack @Nullable [] deadEndItems;

    public GasCentrifugeRecipe(String name) {
        super(name);
    }

    public static String stageName(@Nullable String stage) {
        return I18nUtil.resolveKey(
                "hbmpseudofluid." + (stage == null ? "none" : stage.toLowerCase(Locale.US)));
    }

    public GasCentrifugeRecipe setStage(String stage) {
        this.stage = stage;
        return this;
    }

    public GasCentrifugeRecipe setVolumes(int consumed, int produced) {
        this.consumed = consumed;
        this.produced = produced;
        return this;
    }

    public GasCentrifugeRecipe setNext(String next) {
        this.next = next;
        return this;
    }

    public GasCentrifugeRecipe setRequiresUpgrade() {
        this.requiresUpgrade = true;
        return this;
    }

    public GasCentrifugeRecipe setFeed(Fluid feed) {
        this.feed = feed;
        return this;
    }

    public GasCentrifugeRecipe setDeadEnd(int volume, ItemStackTemplate... items) {
        this.deadEndVolume = volume;
        this.deadEndTemplate = List.of(items);
        return this;
    }

    public ItemStack[] deadEndItems() {
        ItemStack[] live = deadEndItems;
        if (live == null) {
            materialize();
            live = deadEndItems;
        }
        return live == null ? new ItemStack[0] : live;
    }

    @Override
    public void materialize() {
        super.materialize();
        ItemStack[] built = new ItemStack[deadEndTemplate.size()];
        for (int i = 0; i < built.length; i++) built[i] = deadEndTemplate.get(i).create();
        deadEndItems = built;
    }

    @Override
    public List<String> print() {
        List<String> list = new ArrayList<>();
        list.add(ChatFormatting.YELLOW + getLocalizedName());

        duration(list);
        power(list);
        list.add(ChatFormatting.LIGHT_PURPLE + stageName(stage) + ": " + consumed + " mB");
        if (next != null) {
            list.add(ChatFormatting.LIGHT_PURPLE + stageName(next) + ": " + produced + " mB");
        }
        output(list);

        return list;
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return GasCentrifugeRecipes.INSTANCE;
    }

    private record Extras(
            String stage,
            int consumed,
            int produced,
            Optional<String> next,
            boolean requiresUpgrade,
            Optional<Fluid> feed,
            int deadEndVolume,
            List<ItemStackTemplate> deadEndItems) {}
}
