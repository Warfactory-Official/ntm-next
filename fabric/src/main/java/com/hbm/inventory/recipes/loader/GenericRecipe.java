/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.inventory.recipes.loader;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.FluidIconItem;
import com.hbm.util.DataCodecs;
import com.hbm.util.NumberFormatter;
import com.hbm.util.TooltipStyle;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public abstract class GenericRecipe implements Recipe<NtmRecipeInput>, NamedRecipe {
    public CountIngredient[] inputItem;
    public FluidStackNTM[] inputFluid = FluidStackNTM.EMPTY;
    public FluidStackNTM[] outputFluid = FluidStackNTM.EMPTY;
    public int duration;
    public long power;
    public String autoSwitchGroup;
    public int order = Integer.MAX_VALUE;
    public List<String> conditions = List.of();

    protected String nameWrapper;
    protected WeightedList<Optional<ItemStackTemplate>>[] outputTemplate;
    protected Optional<ItemStackTemplate> iconTemplate = Optional.empty();
    protected boolean writeIcon;
    protected boolean iconFromFirstIngredient;
    protected boolean customLocalization;
    protected String[] authoredPools;
    protected String[] blueprintPools;
    protected String[] blueprintPools528;
    protected ResourceKey<Recipe<?>> id;

    private @Nullable String internalName;
    private ItemStack explicitIcon = ItemStack.EMPTY;
    private volatile WeightedList<ItemStack> @Nullable [] preparedOutputs;
    private volatile ItemStack preparedIcon = ItemStack.EMPTY;
    private volatile boolean materialized;
    private volatile boolean awaitingInputDisplay;

    private static final Codec<String> NAME_CODEC =
            Codec.STRING.validate(
                    value ->
                            value.isEmpty()
                                    ? DataResult.error(() -> "Recipe name must not be empty")
                                    : DataResult.success(value));

    protected GenericRecipe(@Nullable String name) {
        internalName = name;
    }

    public abstract GenericRecipes<?, ?> table();

    public static <T extends GenericRecipe> MapCodec<T> mapCodec(Function<String, T> factory) {
        return DataCodecs.recipe(baseCodec(factory, false));
    }

    private static <T extends GenericRecipe> MapCodec<T> baseCodec(
            Function<String, T> factory, boolean fixedGrid) {
        MapCodec<List<CountIngredient>> inputs =
                fixedGrid
                        ? MapCodec.unit(List.of())
                        : CountIngredient.CODEC.listOf().optionalFieldOf("input_items", List.of());
        return RecordCodecBuilder.mapCodec(
                instance ->
                        instance.group(
                                        NAME_CODEC
                                                .optionalFieldOf("name")
                                                .forGetter(GenericRecipe::authoredName),
                                        inputs.forGetter(
                                                row ->
                                                        row.inputItem == null
                                                                ? List.of()
                                                                : Arrays.asList(row.inputItem)),
                                        FluidStackNTM.ARRAY_CODEC
                                                .optionalFieldOf(
                                                        "input_fluids", FluidStackNTM.EMPTY)
                                                .forGetter(
                                                        row ->
                                                                row.inputFluid.length == 0
                                                                        ? FluidStackNTM.EMPTY
                                                                        : row.inputFluid),
                                        Outputs.TEMPLATE_CODEC
                                                .listOf()
                                                .optionalFieldOf("output_items", List.of())
                                                .forGetter(GenericRecipe::outputTemplates),
                                        FluidStackNTM.ARRAY_CODEC
                                                .optionalFieldOf(
                                                        "output_fluids", FluidStackNTM.EMPTY)
                                                .forGetter(
                                                        row ->
                                                                row.outputFluid.length == 0
                                                                        ? FluidStackNTM.EMPTY
                                                                        : row.outputFluid),
                                        Codec.INT
                                                .optionalFieldOf("duration", 0)
                                                .forGetter(row -> row.duration),
                                        Codec.LONG
                                                .optionalFieldOf("power", 0L)
                                                .forGetter(row -> row.power),
                                        ItemStackTemplate.MAP_CODEC
                                                .codec()
                                                .optionalFieldOf("icon")
                                                .forGetter(GenericRecipe::iconTemplate),
                                        Codec.BOOL
                                                .optionalFieldOf("icon_from_input", false)
                                                .forGetter(row -> row.iconFromFirstIngredient),
                                        Codec.STRING
                                                .optionalFieldOf("name_wrapper")
                                                .forGetter(
                                                        row ->
                                                                Optional.ofNullable(
                                                                        row.nameWrapper)),
                                        Codec.BOOL
                                                .optionalFieldOf("custom_localization", false)
                                                .forGetter(row -> row.customLocalization),
                                        Codec.STRING
                                                .listOf()
                                                .optionalFieldOf("blueprint_pools")
                                                .forGetter(
                                                        row -> optionalStrings(row.authoredPools)),
                                        Codec.STRING
                                                .listOf()
                                                .optionalFieldOf("blueprint_pools_528")
                                                .forGetter(
                                                        row ->
                                                                optionalStrings(
                                                                        row.blueprintPools528)),
                                        Codec.STRING
                                                .optionalFieldOf("auto_switch_group")
                                                .forGetter(
                                                        row ->
                                                                Optional.ofNullable(
                                                                        row.autoSwitchGroup)),
                                        Codec.INT
                                                .optionalFieldOf("order", Integer.MAX_VALUE)
                                                .forGetter(row -> row.order))
                                .apply(
                                        instance,
                                        (name,
                                                inputItems,
                                                inputFluids,
                                                outputItems,
                                                outputFluids,
                                                duration,
                                                power,
                                                icon,
                                                inputIcon,
                                                wrapper,
                                                localized,
                                                pools,
                                                specialPools,
                                                group,
                                                order) -> {
                                            T row = factory.apply(name.orElse(null));
                                            row.inputItem =
                                                    inputItems.toArray(CountIngredient[]::new);
                                            row.inputFluid = inputFluids;
                                            row.outputTemplate = templateArray(outputItems);
                                            row.outputFluid = outputFluids;
                                            row.duration = duration;
                                            row.power = power;
                                            row.iconTemplate = icon;
                                            row.writeIcon = icon.isPresent();
                                            row.iconFromFirstIngredient = inputIcon;
                                            row.nameWrapper = wrapper.orElse(null);
                                            row.customLocalization = localized;
                                            row.authoredPools =
                                                    pools.map(
                                                                    values ->
                                                                            values.toArray(
                                                                                    String[]::new))
                                                            .orElse(null);
                                            row.blueprintPools528 =
                                                    specialPools
                                                            .map(
                                                                    values ->
                                                                            values.toArray(
                                                                                    String[]::new))
                                                            .orElse(null);
                                            row.blueprintPools =
                                                    effectivePools(
                                                            row.authoredPools,
                                                            row.blueprintPools528,
                                                            RecipeConditions.test("enable_528"));
                                            row.autoSwitchGroup = group.orElse(null);
                                            row.order = order;
                                            return row;
                                        }));
    }

    public static <T extends GenericRecipe, E> MapCodec<T> mapCodec(
            Function<String, T> factory,
            MapCodec<E> extras,
            BiConsumer<T, E> readExtras,
            Function<T, E> writeExtras) {
        return DataCodecs.recipe(
                Codec.mapPair(baseCodec(factory, false), extras)
                        .xmap(
                                pair -> {
                                    T row = pair.getFirst();
                                    readExtras.accept(row, pair.getSecond());
                                    return row;
                                },
                                row -> Pair.of(row, writeExtras.apply(row))));
    }

    private static <T extends GenericRecipe> MapCodec<T> gridBaseCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> readCells,
            Function<T, int[]> writeCells) {
        return Codec.mapPair(baseCodec(factory, true), FixedGrid.MAP_CODEC)
                .xmap(
                        pair -> {
                            T row = pair.getFirst();
                            row.inputItem = pair.getSecond().inputs();
                            readCells.accept(row, pair.getSecond().cells());
                            return row;
                        },
                        row -> Pair.of(row, new FixedGrid(row.inputItem, writeCells.apply(row))));
    }

    public static <T extends GenericRecipe> MapCodec<T> gridCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> readCells,
            Function<T, int[]> writeCells) {
        return DataCodecs.recipe(gridBaseCodec(factory, readCells, writeCells));
    }

    public static <T extends GenericRecipe, E> MapCodec<T> gridCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> readCells,
            Function<T, int[]> writeCells,
            MapCodec<E> extras,
            BiConsumer<T, E> readExtras,
            Function<T, E> writeExtras) {
        return DataCodecs.recipe(
                Codec.mapPair(gridBaseCodec(factory, readCells, writeCells), extras)
                        .xmap(
                                pair -> {
                                    T row = pair.getFirst();
                                    readExtras.accept(row, pair.getSecond());
                                    return row;
                                },
                                row -> Pair.of(row, writeExtras.apply(row))));
    }

    private Optional<String> authoredName() {
        return id != null && Objects.equals(internalName, defaultName(id))
                ? Optional.empty()
                : Optional.ofNullable(internalName);
    }

    private static Optional<List<String>> optionalStrings(String @Nullable [] values) {
        return values == null ? Optional.empty() : Optional.of(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    private static WeightedList<Optional<ItemStackTemplate>>[] templateArray(
            List<WeightedList<Optional<ItemStackTemplate>>> values) {
        return values.toArray(WeightedList[]::new);
    }

    @SuppressWarnings("unchecked")
    private static WeightedList<Optional<ItemStackTemplate>>[] templateArray(int size) {
        return new WeightedList[size];
    }

    public static <T extends GenericRecipe> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(
            Function<String, T> factory) {
        return streamCodec(
                factory,
                new StreamCodec<RegistryFriendlyByteBuf, Boolean>() {
                    @Override
                    public Boolean decode(RegistryFriendlyByteBuf buffer) {
                        return true;
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, Boolean value) {}
                },
                (_, _) -> {},
                _ -> true);
    }

    public static <T extends GenericRecipe, E> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(
            Function<String, T> factory,
            StreamCodec<RegistryFriendlyByteBuf, E> extras,
            BiConsumer<T, E> readExtras,
            Function<T, E> writeExtras) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
                var row = factory.apply(buffer.readUtf());
                row.inputItem =
                        readArray(buffer, CountIngredient.STREAM_CODEC, CountIngredient[]::new);
                row.inputFluid = FluidStackNTM.ARRAY_STREAM_CODEC.decode(buffer);
                row.outputTemplate =
                        readArray(
                                buffer,
                                Outputs.TEMPLATE_STREAM_CODEC,
                                GenericRecipe::templateArray);
                row.outputFluid = FluidStackNTM.ARRAY_STREAM_CODEC.decode(buffer);
                row.duration = buffer.readVarInt();
                row.power = buffer.readVarLong();
                row.iconTemplate =
                        buffer.readBoolean()
                                ? Optional.of(ItemStackTemplate.STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                row.writeIcon = row.iconTemplate.isPresent();
                row.iconFromFirstIngredient = buffer.readBoolean();
                row.nameWrapper = readOptionalString(buffer);
                row.customLocalization = buffer.readBoolean();
                row.authoredPools = readOptionalStrings(buffer);
                row.blueprintPools528 = readOptionalStrings(buffer);
                row.blueprintPools =
                        effectivePools(
                                row.authoredPools, row.blueprintPools528, buffer.readBoolean());
                row.autoSwitchGroup = readOptionalString(buffer);
                row.order = buffer.readVarInt();
                readExtras.accept(row, extras.decode(buffer));
                return row;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, T row) {
                buffer.writeUtf(row.getInternalName());
                writeValues(
                        buffer,
                        row.inputItem == null ? List.of() : Arrays.asList(row.inputItem),
                        CountIngredient.STREAM_CODEC);
                FluidStackNTM.ARRAY_STREAM_CODEC.encode(buffer, row.inputFluid);
                writeValues(buffer, row.outputTemplates(), Outputs.TEMPLATE_STREAM_CODEC);
                FluidStackNTM.ARRAY_STREAM_CODEC.encode(buffer, row.outputFluid);
                buffer.writeVarInt(row.duration);
                buffer.writeVarLong(row.power);
                var icon = row.iconTemplate();
                buffer.writeBoolean(icon.isPresent());
                icon.ifPresent(value -> ItemStackTemplate.STREAM_CODEC.encode(buffer, value));
                buffer.writeBoolean(row.iconFromFirstIngredient);
                writeOptionalString(buffer, row.nameWrapper);
                buffer.writeBoolean(row.customLocalization);
                writeOptionalStrings(buffer, row.authoredPools);
                writeOptionalStrings(buffer, row.blueprintPools528);
                buffer.writeBoolean(
                        row.blueprintPools528 != null
                                && row.blueprintPools == row.blueprintPools528);
                writeOptionalString(buffer, row.autoSwitchGroup);
                buffer.writeVarInt(row.order);
                extras.encode(buffer, writeExtras.apply(row));
            }
        };
    }

    private static <E> E[] readArray(
            RegistryFriendlyByteBuf buffer,
            StreamCodec<RegistryFriendlyByteBuf, E> codec,
            IntFunction<E[]> factory) {
        E[] values = factory.apply(buffer.readVarInt());
        for (int i = 0; i < values.length; i++) {
            values[i] = codec.decode(buffer);
        }
        return values;
    }

    private static <E> void writeValues(
            RegistryFriendlyByteBuf buffer,
            List<E> values,
            StreamCodec<RegistryFriendlyByteBuf, E> codec) {
        buffer.writeVarInt(values.size());
        for (var value : values) {
            codec.encode(buffer, value);
        }
    }

    private static @Nullable String readOptionalString(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUtf() : null;
    }

    private static void writeOptionalString(
            RegistryFriendlyByteBuf buffer, @Nullable String value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeUtf(value);
        }
    }

    private static String @Nullable [] readOptionalStrings(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }
        var values = new String[buffer.readVarInt()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.readUtf();
        }
        return values;
    }

    private static void writeOptionalStrings(
            RegistryFriendlyByteBuf buffer, String @Nullable [] values) {
        buffer.writeBoolean(values != null);
        if (values != null) {
            buffer.writeVarInt(values.length);
            for (var value : values) {
                buffer.writeUtf(value);
            }
        }
    }

    public GenericRecipe setDuration(int value) {
        duration = value;
        return this;
    }

    public GenericRecipe setPower(long value) {
        power = value;
        return this;
    }

    public GenericRecipe setup(int ticks, long energy) {
        duration = ticks;
        power = energy;
        return this;
    }

    public GenericRecipe setupNamed(int ticks, long energy) {
        return setup(ticks, energy).setNamed();
    }

    public GenericRecipe setNameWrapper(String wrapper) {
        nameWrapper = wrapper;
        return this;
    }

    public GenericRecipe setNamed() {
        customLocalization = true;
        return this;
    }

    public GenericRecipe setConditions(String... values) {
        conditions = List.of(values);
        return this;
    }

    public GenericRecipe setGroup(String group) {
        autoSwitchGroup = group;
        return this;
    }

    public GenericRecipe inputItems(CountIngredient... values) {
        inputItem = values;
        materialized = false;
        return this;
    }

    public GenericRecipe inputFluids(FluidStackNTM... values) {
        inputFluid = values;
        return this;
    }

    public GenericRecipe outputFluids(FluidStackNTM... values) {
        outputFluid = values;
        materialized = false;
        return this;
    }

    @SafeVarargs
    public final synchronized GenericRecipe outputItems(WeightedList<ItemStack>... values) {
        outputTemplate = null;
        preparedOutputs = values;
        materialized = false;
        return this;
    }

    @SuppressWarnings("unchecked")
    public GenericRecipe outputItems(ItemStack... values) {
        WeightedList<ItemStack>[] slots = new WeightedList[values.length];
        for (int i = 0; i < values.length; i++) {
            slots[i] = Outputs.fixed(values[i]);
        }
        return outputItems(slots);
    }

    @SafeVarargs
    public final synchronized GenericRecipe outputTemplates(
            WeightedList<Optional<ItemStackTemplate>>... values) {
        outputTemplate = values;
        preparedOutputs = null;
        materialized = false;
        return this;
    }

    @SuppressWarnings("unchecked")
    public GenericRecipe outputTemplates(ItemStackTemplate... values) {
        WeightedList<Optional<ItemStackTemplate>>[] slots = new WeightedList[values.length];
        for (int i = 0; i < values.length; i++) {
            slots[i] = WeightedList.of(Optional.of(values[i]));
        }
        return outputTemplates(slots);
    }

    public GenericRecipe setIconToFirstIngredient() {
        iconFromFirstIngredient = true;
        materialized = false;
        return this;
    }

    public synchronized GenericRecipe setIcon(ItemStackTemplate value) {
        iconTemplate = Optional.of(value);
        explicitIcon = ItemStack.EMPTY;
        writeIcon = true;
        materialized = false;
        return this;
    }

    public synchronized GenericRecipe setIcon(ItemStack value) {
        iconTemplate = Optional.empty();
        explicitIcon = value;
        writeIcon = true;
        materialized = false;
        return this;
    }

    public GenericRecipe setIcon(Item value) {
        return setIcon(new ItemStack(value));
    }

    public GenericRecipe setIcon(Block value) {
        return setIcon(new ItemStack(value));
    }

    public GenericRecipe setIcon(Item value, int ignored) {
        return setIcon(new ItemStack(value));
    }

    public GenericRecipe setPools(String... values) {
        authoredPools = values;
        blueprintPools = values;
        return this;
    }

    public GenericRecipe setPools528(String... values) {
        blueprintPools528 = values;
        return this;
    }

    public static String @Nullable [] effectivePools(
            String @Nullable [] normal, String @Nullable [] special, boolean useSpecial) {
        return special != null && useSpecial ? special : normal;
    }

    public boolean isPooled() {
        return blueprintPools != null;
    }

    public String[] getPools() {
        return blueprintPools;
    }

    public boolean isPartOfPool(String pool) {
        if (blueprintPools != null) {
            for (var value : blueprintPools) {
                if (value.equals(pool)) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void materialize() {
        prepare();
    }

    public synchronized void refreshInputDisplay() {
        if (!materialized) materialize();
        else if (!writeIcon && iconFromFirstIngredient) preparedIcon = derivedIcon();
    }

    private void ensureMaterialized() {
        if (!materialized) {
            synchronized (this) {
                if (!materialized) {
                    prepare();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void prepare() {
        materialized = false;
        var outputs = preparedOutputs;
        if (outputTemplate != null) {
            outputs = new WeightedList[outputTemplate.length];
            for (int i = 0; i < outputTemplate.length; i++) {
                var alternatives = new ArrayList<Weighted<ItemStack>>();
                for (var entry : outputTemplate[i].unwrap()) {
                    alternatives.add(
                            new Weighted<>(
                                    entry.value()
                                            .map(ItemStackTemplate::create)
                                            .orElse(ItemStack.EMPTY),
                                    entry.weight()));
                }
                outputs[i] = WeightedList.of(alternatives);
            }
        }

        preparedOutputs = outputs;
        awaitingInputDisplay = false;
        var icon = explicitIcon;
        if (writeIcon && iconTemplate.isPresent()) icon = iconTemplate.get().create();
        preparedIcon = writeIcon ? icon : derivedIcon();
        materialized = true;
    }

    private ItemStack derivedIcon() {
        awaitingInputDisplay = false;
        if (iconFromFirstIngredient && inputItem != null && inputItem.length > 0) {
            var displays = inputItem[0].displayStacks();
            if (!displays.isEmpty()) return displays.getFirst();
            awaitingInputDisplay = true;
        }
        var outputs = preparedOutputs;
        if (outputs != null && outputs.length > 0) {
            var alternatives = outputs[0].unwrap();
            return alternatives.isEmpty() ? ItemStack.EMPTY : alternatives.getFirst().value();
        }
        return outputFluid.length == 0 ? ItemStack.EMPTY : FluidIconItem.make(outputFluid[0]);
    }

    public WeightedList<ItemStack>[] outputItems() {
        ensureMaterialized();
        return preparedOutputs;
    }

    public List<WeightedList<Optional<ItemStackTemplate>>> outputTemplates() {
        if (outputTemplate != null) {
            return Arrays.asList(outputTemplate);
        }
        var outputs = preparedOutputs;
        if (outputs == null) {
            return List.of();
        }
        var templates = new ArrayList<WeightedList<Optional<ItemStackTemplate>>>(outputs.length);
        for (var slot : outputs) {
            var alternatives = new ArrayList<Weighted<Optional<ItemStackTemplate>>>();
            for (var entry : slot.unwrap()) {
                var stack = entry.value();
                Optional<ItemStackTemplate> template =
                        stack.isEmpty()
                                ? Optional.empty()
                                : Optional.of(ItemStackTemplate.fromStack(stack));
                alternatives.add(new Weighted<>(template, entry.weight()));
            }
            templates.add(WeightedList.of(alternatives));
        }
        return templates;
    }

    public Optional<ItemStackTemplate> iconTemplate() {
        if (!writeIcon || iconTemplate.isPresent()) {
            return iconTemplate;
        }
        return Optional.of(ItemStackTemplate.fromStack(explicitIcon));
    }

    public ItemStack getIcon() {
        ensureMaterialized();
        if (awaitingInputDisplay) {
            synchronized (this) {
                if (awaitingInputDisplay) {
                    var displays = inputItem[0].displayStacks();
                    if (!displays.isEmpty()) {
                        preparedIcon = displays.getFirst();
                        awaitingInputDisplay = false;
                    }
                }
            }
        }
        return preparedIcon;
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    public Component getLocalizedName() {
        Component name;
        if (customLocalization) {
            name = Component.translatable(getInternalName());
        } else {
            var icon = getIcon();
            name = icon.isEmpty() ? Component.literal(getInternalName()) : icon.getHoverName();
        }
        return nameWrapper == null ? name : Component.translatable(nameWrapper, name);
    }

    public boolean matchesSearch(String search) {
        return getLocalizedName()
                .getString()
                .toLowerCase(Locale.ROOT)
                .contains(search.toLowerCase(Locale.ROOT));
    }

    public List<Component> print() {
        var lines = new ArrayList<Component>();
        header(lines);
        autoSwitch(lines);
        duration(lines);
        power(lines);
        input(lines);
        output(lines);
        return lines;
    }

    protected void header(List<Component> lines) {
        lines.add(getLocalizedName().copy().withStyle(ChatFormatting.YELLOW));
        if (ModifierKeys.detailsHeld()) {
            lines.add(
                    Component.translatable("gui.recipe.internal", getInternalName())
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    protected void autoSwitch(List<Component> lines) {
        if (autoSwitchGroup != null) {
            var text =
                    Component.translatable("autoswitch", Component.translatable(autoSwitchGroup));
            for (var line : TooltipStyle.lines(text)) {
                lines.add(line.copy().withStyle(ChatFormatting.GOLD));
            }
        }
    }

    protected void duration(List<Component> lines) {
        if (duration > 0) {
            lines.add(
                    Component.translatable("gui.recipe.duration")
                            .append(": ")
                            .append(Component.translatable("unit.seconds", duration / 20.0D))
                            .withStyle(ChatFormatting.RED));
        }
    }

    protected void power(List<Component> lines) {
        if (power > 0) {
            lines.add(
                    Component.translatable("gui.recipe.consumption")
                            .append(": ")
                            .append(
                                    Component.translatable(
                                            "unit.energy_per_tick",
                                            NumberFormatter.formatCompact(power)))
                            .withStyle(ChatFormatting.RED));
        }
    }

    protected void input(List<Component> lines) {
        lines.add(
                Component.translatable("gui.recipe.input")
                        .append(":")
                        .withStyle(ChatFormatting.BOLD));
        if (inputItem != null) {
            for (var ingredient : inputItem) {
                var stack = ingredient.extractForCyclingDisplay(20);
                if (!stack.isEmpty()) {
                    lines.add(itemText(stack, "  "));
                }
            }
        }
        for (var fluid : inputFluid) {
            lines.add(Component.literal("  ").append(fluidText(fluid)));
        }
    }

    protected void output(List<Component> lines) {
        lines.add(
                Component.translatable("gui.recipe.output")
                        .append(":")
                        .withStyle(ChatFormatting.BOLD));
        var outputs = outputItems();
        if (outputs != null) {
            for (var slot : outputs) {
                for (var line : label(slot)) {
                    lines.add(Component.literal("  ").append(line));
                }
            }
        }
        for (var fluid : outputFluid) {
            lines.add(Component.literal("  ").append(fluidText(fluid)));
        }
    }

    private static MutableComponent itemText(ItemStack stack, String indentation) {
        return Component.literal(indentation + stack.getCount() + "x ")
                .withStyle(ChatFormatting.GRAY)
                .append(stack.getHoverName());
    }

    protected static List<Component> label(WeightedList<ItemStack> slot) {
        var entries = slot.unwrap();
        int total = 0;
        int shown = 0;
        for (var entry : entries) {
            total += entry.weight();
            if (!entry.value().isEmpty()) {
                shown++;
            }
        }
        if (shown == 0) {
            return List.of();
        }
        var lines = new ArrayList<Component>();
        if (shown > 1) {
            lines.add(Component.translatable("gui.recipe.one_of"));
        }
        for (var entry : entries) {
            if (entry.value().isEmpty()) {
                continue;
            }
            float probability =
                    total == 0 && entries.size() == 1 ? 1.0F : (float) entry.weight() / total;
            var text = itemText(entry.value(), shown > 1 ? "  " : "");
            if (shown > 1 || probability < 1.0F) {
                float percent = (int) (probability * 1000) / 10.0F;
                text.append(" (")
                        .append(Component.translatable("unit.percent", percent))
                        .append(")");
            }
            lines.add(text);
        }
        return lines;
    }

    private static Component fluidText(FluidStackNTM fluid) {
        var text =
                Component.translatable("unit.millibuckets", fluid.amount())
                        .withStyle(ChatFormatting.BLUE)
                        .append(" ")
                        .append(NTMFluidProperties.getDisplayName(fluid.type()));
        if (fluid.pressure() != 0) {
            text.append(" ")
                    .append(Component.translatable("gui.recipe.atPressure"))
                    .append(" ")
                    .append(
                            Component.translatable("unit.pressure", fluid.pressure())
                                    .withStyle(ChatFormatting.RED));
        }
        return text;
    }

    @Override
    public boolean matches(NtmRecipeInput input, Level level) {
        if (inputItem == null || inputItem.length == 0) {
            return true;
        }
        int[] assigned = new int[input.size()];
        Arrays.fill(assigned, -1);
        boolean[] visited = new boolean[input.size()];
        for (int ingredient = 0; ingredient < inputItem.length; ingredient++) {
            Arrays.fill(visited, false);
            if (!assignSlot(ingredient, input, assigned, visited)) {
                return false;
            }
        }
        return true;
    }

    private boolean assignSlot(
            int ingredient, NtmRecipeInput input, int[] assigned, boolean[] visited) {
        for (int slot = 0; slot < assigned.length; slot++) {
            if (visited[slot]) {
                continue;
            }
            var stack = input.getItem(slot);
            if (stack.isEmpty() || !inputItem[ingredient].test(stack)) {
                continue;
            }
            visited[slot] = true;
            int previous = assigned[slot];
            if (previous == -1 || assignSlot(previous, input, assigned, visited)) {
                assigned[slot] = ingredient;
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(NtmRecipeInput input) {
        var outputs = outputItems();
        return outputs == null || outputs.length == 0
                ? ItemStack.EMPTY
                : outputs[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    public @Nullable ResourceKey<Recipe<?>> recipeId() {
        return id;
    }

    public void bind(ResourceKey<Recipe<?>> key) {
        id = key;
        if (internalName == null) {
            internalName = defaultName(key);
        }
    }

    public static String defaultName(ResourceKey<Recipe<?>> key) {
        var path = key.identifier().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
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

    public boolean matchesInputFluids(FluidTankNTM[] tanks) {
        for (int i = 0; i < Math.min(inputFluid.length, tanks.length); i++) {
            if (!inputFluid[i].matches(tanks[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean outputFluidsFit(FluidTankNTM[] tanks) {
        for (int i = 0; i < Math.min(outputFluid.length, tanks.length); i++) {
            if (!outputFluid[i].fitsInto(tanks[i])) {
                return false;
            }
        }
        return true;
    }

    public void drainInputFluids(FluidTankNTM[] tanks) {
        for (int i = 0; i < Math.min(inputFluid.length, tanks.length); i++) {
            inputFluid[i].drainFrom(tanks[i]);
        }
    }

    public void fillOutputFluids(FluidTankNTM[] tanks) {
        for (int i = 0; i < Math.min(outputFluid.length, tanks.length); i++) {
            outputFluid[i].fillInto(tanks[i]);
        }
    }

    @Override
    public RecipeType<? extends Recipe<NtmRecipeInput>> getType() {
        return table().type();
    }

    @Override
    public RecipeSerializer<? extends Recipe<NtmRecipeInput>> getSerializer() {
        return table().serializer();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return NamedRecipe.BOOK_CATEGORY;
    }
}
