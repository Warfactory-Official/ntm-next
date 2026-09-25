// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.util.BobMathUtil;
import com.hbm.util.DataCodecs;
import com.hbm.util.I18nUtil;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public abstract class GenericRecipe implements Recipe<NtmRecipeInput>, INamedRecipe {

    private @Nullable String name;
    public CountIngredient[] inputItem;
    public FluidStackNTM[] inputFluid = FluidStackNTM.EMPTY;
    public FluidStackNTM[] outputFluid = FluidStackNTM.EMPTY;
    public int duration;
    public long power;
    public String autoSwitchGroup = null;

    public int order = Integer.MAX_VALUE;

    public List<String> conditions = List.of();
    protected String nameWrapper;

    protected WeightedList<Optional<ItemStackTemplate>>[] outputTemplate;
    protected Optional<ItemStackTemplate> iconTemplate = Optional.empty();
    protected boolean writeIcon = false;

    protected boolean iconFromFirstIngredient = false;
    protected boolean customLocalization = false;

    protected String[] authoredPools = null;
    protected String[] blueprintPools = null;
    protected String[] blueprintPools528 = null;

    protected ResourceKey<Recipe<?>> id;

    private volatile WeightedList<ItemStack>[] outputItem;

    private volatile ItemStack icon;

    protected GenericRecipe(@Nullable String name) {
        this.name = name;
    }

    public static <T extends GenericRecipe> MapCodec<T> mapCodec(Function<String, T> factory) {
        return DataCodecs.recipe(baseCodec(factory, false));
    }

    private static <T extends GenericRecipe> MapCodec<T> baseCodec(
            Function<String, T> factory, boolean fixedGrid) {
        MapCodec<List<CountIngredient>> inputs =
                fixedGrid
                        ? MapCodec.unit(List.<CountIngredient>of())
                        : CountIngredient.CODEC.listOf().optionalFieldOf("input_items", List.of());
        return RecordCodecBuilder.mapCodec(
                i ->
                        i.group(
                                        ExtraCodecs.NON_EMPTY_STRING
                                                .optionalFieldOf("name")
                                                .forGetter(GenericRecipe::authoredName),
                                        inputs.forGetter(r -> nullSafe(r.inputItem)),
                                        FluidStackNTM.CODEC
                                                .listOf()
                                                .optionalFieldOf("input_fluids", List.of())
                                                .forGetter(r -> nullSafe(r.inputFluid)),
                                        Outputs.TEMPLATE_CODEC
                                                .listOf()
                                                .optionalFieldOf("output_items", List.of())
                                                .forGetter(GenericRecipe::outputTemplates),
                                        FluidStackNTM.CODEC
                                                .listOf()
                                                .optionalFieldOf("output_fluids", List.of())
                                                .forGetter(r -> nullSafe(r.outputFluid)),
                                        Codec.INT
                                                .optionalFieldOf("duration", 0)
                                                .forGetter(r -> r.duration),
                                        Codec.LONG
                                                .optionalFieldOf("power", 0L)
                                                .forGetter(r -> r.power),
                                        ItemStackTemplate.MAP_CODEC
                                                .codec()
                                                .optionalFieldOf("icon")
                                                .forGetter(GenericRecipe::iconTemplate),
                                        Codec.BOOL
                                                .optionalFieldOf("icon_from_input", false)
                                                .forGetter(r -> r.iconFromFirstIngredient),
                                        Codec.STRING
                                                .optionalFieldOf("name_wrapper")
                                                .forGetter(r -> Optional.ofNullable(r.nameWrapper)),
                                        Codec.BOOL
                                                .optionalFieldOf("custom_localization", false)
                                                .forGetter(r -> r.customLocalization),
                                        Codec.STRING
                                                .listOf()
                                                .optionalFieldOf("blueprint_pools")
                                                .forGetter(
                                                        r ->
                                                                Optional.ofNullable(r.authoredPools)
                                                                        .map(Arrays::asList)),
                                        Codec.STRING
                                                .listOf()
                                                .optionalFieldOf("blueprint_pools_528")
                                                .forGetter(
                                                        r ->
                                                                Optional.ofNullable(
                                                                                r.blueprintPools528)
                                                                        .map(Arrays::asList)),
                                        Codec.STRING
                                                .optionalFieldOf("auto_switch_group")
                                                .forGetter(
                                                        r ->
                                                                Optional.ofNullable(
                                                                        r.autoSwitchGroup)),
                                        Codec.INT
                                                .optionalFieldOf("order", Integer.MAX_VALUE)
                                                .forGetter(r -> r.order))
                                .apply(
                                        i,
                                        (name,
                                                inputItems,
                                                inputFluids,
                                                outputItems,
                                                outputFluids,
                                                duration,
                                                power,
                                                icon,
                                                iconFromInput,
                                                nameWrapper,
                                                customLocalization,
                                                pools,
                                                pools528,
                                                group,
                                                order) -> {
                                            T recipe = factory.apply(name.orElse(null));
                                            recipe.inputItem =
                                                    inputItems.toArray(CountIngredient[]::new);
                                            recipe.inputFluid =
                                                    inputFluids.toArray(FluidStackNTM[]::new);
                                            recipe.outputTemplate = templateArray(outputItems);
                                            recipe.outputFluid =
                                                    outputFluids.toArray(FluidStackNTM[]::new);
                                            recipe.duration = duration;
                                            recipe.power = power;
                                            recipe.iconTemplate = icon;
                                            recipe.writeIcon = icon.isPresent();
                                            recipe.iconFromFirstIngredient = iconFromInput;
                                            recipe.nameWrapper = nameWrapper.orElse(null);
                                            recipe.customLocalization = customLocalization;
                                            recipe.authoredPools =
                                                    pools.map(p -> p.toArray(String[]::new))
                                                            .orElse(null);
                                            recipe.blueprintPools528 =
                                                    pools528.map(p -> p.toArray(String[]::new))
                                                            .orElse(null);
                                            recipe.blueprintPools =
                                                    effectivePools(
                                                            recipe.authoredPools,
                                                            recipe.blueprintPools528,
                                                            RecipeConditions.test("enable_528"));
                                            recipe.autoSwitchGroup = group.orElse(null);
                                            recipe.order = order;
                                            return recipe;
                                        }));
    }

    public static <T extends GenericRecipe, E> MapCodec<T> mapCodec(
            Function<String, T> factory,
            MapCodec<E> extras,
            BiConsumer<T, E> apply,
            Function<T, E> extract) {
        return DataCodecs.recipe(
                Codec.mapPair(baseCodec(factory, false), extras)
                        .xmap(
                                pair -> {
                                    T recipe = pair.getFirst();
                                    apply.accept(recipe, pair.getSecond());
                                    return recipe;
                                },
                                recipe -> Pair.of(recipe, extract.apply(recipe))));
    }

    private static <T extends GenericRecipe> MapCodec<T> gridBaseCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> applyCells,
            Function<T, int[]> cells) {
        return Codec.mapPair(baseCodec(factory, true), FixedGrid.MAP_CODEC)
                .xmap(
                        pair -> {
                            T recipe = pair.getFirst();
                            recipe.inputItem = pair.getSecond().inputs();
                            applyCells.accept(recipe, pair.getSecond().cells());
                            return recipe;
                        },
                        recipe ->
                                Pair.of(
                                        recipe,
                                        new FixedGrid(recipe.inputItem, cells.apply(recipe))));
    }

    public static <T extends GenericRecipe> MapCodec<T> gridCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> applyCells,
            Function<T, int[]> cells) {
        return DataCodecs.recipe(gridBaseCodec(factory, applyCells, cells));
    }

    public static <T extends GenericRecipe, E> MapCodec<T> gridCodec(
            Function<String, T> factory,
            BiConsumer<T, int[]> applyCells,
            Function<T, int[]> cells,
            MapCodec<E> extras,
            BiConsumer<T, E> apply,
            Function<T, E> extract) {
        return DataCodecs.recipe(
                Codec.mapPair(gridBaseCodec(factory, applyCells, cells), extras)
                        .xmap(
                                pair -> {
                                    T recipe = pair.getFirst();
                                    apply.accept(recipe, pair.getSecond());
                                    return recipe;
                                },
                                recipe -> Pair.of(recipe, extract.apply(recipe))));
    }

    public static <T extends GenericRecipe, E> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(
            Function<String, T> factory,
            StreamCodec<RegistryFriendlyByteBuf, E> extras,
            BiConsumer<T, E> apply,
            Function<T, E> extract) {
        StreamCodec<RegistryFriendlyByteBuf, T> base = streamCodec(factory);
        return StreamCodec.of(
                (buf, recipe) -> {
                    base.encode(buf, recipe);
                    extras.encode(buf, extract.apply(recipe));
                },
                buf -> {
                    T recipe = base.decode(buf);
                    apply.accept(recipe, extras.decode(buf));
                    return recipe;
                });
    }

    public static <T extends GenericRecipe> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(
            Function<String, T> factory) {

        return StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeUtf(recipe.getInternalName());
                    buf.writeCollection(
                            nullSafe(recipe.inputItem),
                            (b, ci) ->
                                    CountIngredient.STREAM_CODEC.encode(
                                            (RegistryFriendlyByteBuf) b, ci));
                    FluidStackNTM.ARRAY_STREAM_CODEC.encode(buf, nullSafeArray(recipe.inputFluid));
                    buf.writeCollection(
                            recipe.outputTemplates(),
                            (b, out) ->
                                    Outputs.TEMPLATE_STREAM_CODEC.encode(
                                            (RegistryFriendlyByteBuf) b, out));
                    FluidStackNTM.ARRAY_STREAM_CODEC.encode(buf, nullSafeArray(recipe.outputFluid));
                    buf.writeVarInt(recipe.duration);
                    buf.writeVarLong(recipe.power);
                    buf.writeOptional(
                            recipe.iconTemplate(),
                            (b, template) ->
                                    ItemStackTemplate.STREAM_CODEC.encode(
                                            (RegistryFriendlyByteBuf) b, template));
                    buf.writeBoolean(recipe.iconFromFirstIngredient);
                    buf.writeOptional(
                            Optional.ofNullable(recipe.nameWrapper), FriendlyByteBuf::writeUtf);
                    buf.writeBoolean(recipe.customLocalization);
                    buf.writeOptional(
                            Optional.ofNullable(recipe.authoredPools),
                            (b, pools) ->
                                    b.writeCollection(
                                            Arrays.asList(pools), FriendlyByteBuf::writeUtf));
                    buf.writeOptional(
                            Optional.ofNullable(recipe.blueprintPools528),
                            (b, pools) ->
                                    b.writeCollection(
                                            Arrays.asList(pools), FriendlyByteBuf::writeUtf));

                    buf.writeBoolean(
                            recipe.blueprintPools528 != null
                                    && recipe.blueprintPools == recipe.blueprintPools528);
                    buf.writeOptional(
                            Optional.ofNullable(recipe.autoSwitchGroup), FriendlyByteBuf::writeUtf);
                    buf.writeVarInt(recipe.order);
                },
                buf -> {
                    T recipe = factory.apply(buf.readUtf());
                    recipe.inputItem =
                            buf.readCollection(
                                            ArrayList::new,
                                            b ->
                                                    CountIngredient.STREAM_CODEC.decode(
                                                            (RegistryFriendlyByteBuf) b))
                                    .toArray(CountIngredient[]::new);
                    recipe.inputFluid = FluidStackNTM.ARRAY_STREAM_CODEC.decode(buf);
                    recipe.outputTemplate =
                            templateArray(
                                    buf.readCollection(
                                            ArrayList::new,
                                            b ->
                                                    Outputs.TEMPLATE_STREAM_CODEC.decode(
                                                            (RegistryFriendlyByteBuf) b)));
                    recipe.outputFluid = FluidStackNTM.ARRAY_STREAM_CODEC.decode(buf);
                    recipe.duration = buf.readVarInt();
                    recipe.power = buf.readVarLong();
                    recipe.iconTemplate =
                            buf.readOptional(
                                    b ->
                                            ItemStackTemplate.STREAM_CODEC.decode(
                                                    (RegistryFriendlyByteBuf) b));
                    recipe.writeIcon = recipe.iconTemplate.isPresent();
                    recipe.iconFromFirstIngredient = buf.readBoolean();
                    recipe.nameWrapper = buf.readOptional(FriendlyByteBuf::readUtf).orElse(null);
                    recipe.customLocalization = buf.readBoolean();
                    recipe.authoredPools =
                            buf.readOptional(
                                            b ->
                                                    b.readCollection(
                                                                    ArrayList::new,
                                                                    FriendlyByteBuf::readUtf)
                                                            .toArray(String[]::new))
                                    .orElse(null);
                    recipe.blueprintPools528 =
                            buf.readOptional(
                                            b ->
                                                    b.readCollection(
                                                                    ArrayList::new,
                                                                    FriendlyByteBuf::readUtf)
                                                            .toArray(String[]::new))
                                    .orElse(null);
                    recipe.blueprintPools =
                            effectivePools(
                                    recipe.authoredPools,
                                    recipe.blueprintPools528,
                                    buf.readBoolean());
                    recipe.autoSwitchGroup =
                            buf.readOptional(FriendlyByteBuf::readUtf).orElse(null);
                    recipe.order = buf.readVarInt();
                    return recipe;
                });
    }

    private static <E> List<E> nullSafe(E[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    private static FluidStackNTM[] nullSafeArray(FluidStackNTM[] array) {
        return array == null ? FluidStackNTM.EMPTY : array;
    }

    @SuppressWarnings("unchecked")
    private static WeightedList<ItemStack>[] outputArray(int size) {
        return (WeightedList<ItemStack>[]) new WeightedList<?>[size];
    }

    @SuppressWarnings("unchecked")
    private static WeightedList<Optional<ItemStackTemplate>>[] templateArray(int size) {
        return (WeightedList<Optional<ItemStackTemplate>>[]) new WeightedList<?>[size];
    }

    private static WeightedList<Optional<ItemStackTemplate>>[] templateArray(
            List<WeightedList<Optional<ItemStackTemplate>>> outputs) {
        return outputs.toArray(templateArray(0));
    }

    protected static List<String> label(WeightedList<ItemStack> output) {
        List<Weighted<ItemStack>> entries = output.unwrap();
        List<Weighted<ItemStack>> real = new ArrayList<>(entries.size());
        for (Weighted<ItemStack> w : entries) if (!w.value().isEmpty()) real.add(w);
        if (real.isEmpty()) return List.of();

        int total = 0;
        for (Weighted<ItemStack> w : entries) total += w.weight();

        if (real.size() == 1) {
            Weighted<ItemStack> only = real.get(0);
            ItemStack stack = only.value();
            float chance = total == 0 ? 1F : (float) only.weight() / (float) total;
            return List.of(
                    ChatFormatting.GRAY
                            + ""
                            + stack.getCount()
                            + "x "
                            + stack.getHoverName().getString()
                            + (chance >= 1 ? "" : " (" + (int) (chance * 1000) / 10F + "%)"));
        }

        List<String> label = new ArrayList<>(real.size() + 1);
        label.add("One of:");
        for (Weighted<ItemStack> w : real) {
            ItemStack stack = w.value();
            float chance = (float) w.weight() / (float) total;
            label.add(
                    "  "
                            + ChatFormatting.GRAY
                            + stack.getCount()
                            + "x "
                            + stack.getHoverName().getString()
                            + " ("
                            + (int) (chance * 1000F) / 10F
                            + "%)");
        }
        return label;
    }

    private static String fluidLine(FluidStackNTM fluid) {
        String pressurePart =
                fluid.pressure() == 0
                        ? ""
                        : " "
                                + I18nUtil.resolveKey("gui.recipe.atPressure")
                                + " "
                                + ChatFormatting.RED
                                + fluid.pressure()
                                + " PU";
        return ChatFormatting.BLUE
                + ""
                + fluid.amount()
                + "mB "
                + NTMFluidProperties.getDisplayName(fluid.type()).getString()
                + pressurePart;
    }

    public void materialize() {
        if (outputTemplate != null) {
            WeightedList<ItemStack>[] built = outputArray(outputTemplate.length);
            for (int i = 0; i < outputTemplate.length; i++) {
                WeightedList.Builder<ItemStack> slot = WeightedList.builder();
                for (Weighted<Optional<ItemStackTemplate>> entry : outputTemplate[i].unwrap()) {
                    slot.add(
                            entry.value().map(ItemStackTemplate::create).orElse(ItemStack.EMPTY),
                            entry.weight());
                }
                built[i] = slot.build();
            }

            outputItem = built;
        }
        iconTemplate.ifPresent(itemStackTemplate -> icon = itemStackTemplate.create());
    }

    public WeightedList<ItemStack>[] outputItems() {
        WeightedList<ItemStack>[] live = outputItem;
        if (live == null && outputTemplate != null) {
            materialize();
            live = outputItem;
        }
        return live;
    }

    public List<WeightedList<Optional<ItemStackTemplate>>> outputTemplates() {
        if (outputTemplate != null) return Arrays.asList(outputTemplate);
        if (outputItem == null) return List.of();
        List<WeightedList<Optional<ItemStackTemplate>>> out = new ArrayList<>(outputItem.length);
        for (WeightedList<ItemStack> slot : outputItem) {
            WeightedList.Builder<Optional<ItemStackTemplate>> builder = WeightedList.builder();
            for (Weighted<ItemStack> entry : slot.unwrap()) {
                ItemStack stack = entry.value();
                builder.add(
                        stack.isEmpty()
                                ? Optional.empty()
                                : Optional.of(ItemStackTemplate.fromStack(stack)),
                        entry.weight());
            }
            out.add(builder.build());
        }
        return out;
    }

    public Optional<ItemStackTemplate> iconTemplate() {
        if (iconTemplate.isPresent()) return iconTemplate;
        return writeIcon && icon != null && !icon.isEmpty()
                ? Optional.of(ItemStackTemplate.fromStack(icon))
                : Optional.empty();
    }

    @SafeVarargs
    public final GenericRecipe outputTemplates(
            WeightedList<Optional<ItemStackTemplate>>... output) {
        this.outputTemplate = output;
        return this;
    }

    public GenericRecipe outputTemplates(ItemStackTemplate... output) {
        WeightedList<Optional<ItemStackTemplate>>[] slots = templateArray(output.length);
        for (int i = 0; i < output.length; i++) slots[i] = WeightedList.of(Optional.of(output[i]));
        this.outputTemplate = slots;
        return this;
    }

    public GenericRecipe setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public GenericRecipe setPower(long power) {
        this.power = power;
        return this;
    }

    public GenericRecipe setup(int duration, long power) {
        return setDuration(duration).setPower(power);
    }

    public GenericRecipe setupNamed(int duration, long power) {
        return setDuration(duration).setPower(power).setNamed();
    }

    public GenericRecipe setNameWrapper(String wrapper) {
        this.nameWrapper = wrapper;
        return this;
    }

    public GenericRecipe setIcon(Item item, int meta) {
        return setIcon(new ItemStack(item));
    }

    public GenericRecipe setNamed() {
        this.customLocalization = true;
        return this;
    }

    public GenericRecipe setConditions(String... conditions) {
        this.conditions = List.of(conditions);
        return this;
    }

    public GenericRecipe setGroup(String autoSwitch) {
        this.autoSwitchGroup = autoSwitch;
        return this;
    }

    public GenericRecipe inputItems(CountIngredient... input) {
        this.inputItem = input;
        return this;
    }

    public GenericRecipe inputFluids(FluidStackNTM... input) {
        this.inputFluid = input;
        return this;
    }

    public GenericRecipe outputFluids(FluidStackNTM... output) {
        this.outputFluid = output;
        return this;
    }

    @SafeVarargs
    public final GenericRecipe outputItems(WeightedList<ItemStack>... output) {
        this.outputItem = output;
        return this;
    }

    public GenericRecipe outputItems(ItemStack... output) {

        WeightedList<ItemStack>[] built = outputArray(output.length);
        for (int i = 0; i < output.length; i++) built[i] = Outputs.fixed(output[i]);
        this.outputItem = built;
        return this;
    }

    public GenericRecipe setIconToFirstIngredient() {
        this.iconFromFirstIngredient = true;
        return this;
    }

    public boolean isPooled() {
        return blueprintPools != null;
    }

    public String[] getPools() {
        return blueprintPools;
    }

    public GenericRecipe setPools(String... pools) {
        this.authoredPools = pools;
        this.blueprintPools = pools;
        return this;
    }

    public GenericRecipe setPools528(String... pools) {
        this.blueprintPools528 = pools;
        return this;
    }

    public static String @Nullable [] effectivePools(
            String @Nullable [] pools, String @Nullable [] pools528, boolean enable528) {
        return pools528 != null && enable528 ? pools528 : pools;
    }

    public boolean isPartOfPool(String looking) {
        if (!isPooled()) return false;
        for (String p : blueprintPools) if (p.equals(looking)) return true;
        return false;
    }

    public ItemStack getIcon() {
        ItemStack held = icon;
        if (held == null && iconFromFirstIngredient && inputItem != null && inputItem.length > 0) {
            List<ItemStack> first = inputItem[0].displayStacks();
            if (!first.isEmpty()) icon = held = first.getFirst();
        }
        if (held == null) {
            WeightedList<ItemStack>[] outputs = outputItems();
            if (outputs != null && outputs.length > 0) {
                List<Weighted<ItemStack>> entries = outputs[0].unwrap();
                if (!entries.isEmpty()) icon = held = entries.get(0).value();
            } else if (outputFluid.length > 0) {

                icon = held = ItemFluidIcon.make(outputFluid[0]);
            }
        }
        return held != null ? held : ItemStack.EMPTY;
    }

    public GenericRecipe setIcon(ItemStackTemplate icon) {
        this.iconTemplate = Optional.of(icon);
        this.writeIcon = true;
        return this;
    }

    public GenericRecipe setIcon(ItemStack icon) {
        this.icon = icon;
        this.writeIcon = true;
        return this;
    }

    public GenericRecipe setIcon(Item item) {
        return setIcon(new ItemStack(item));
    }

    public GenericRecipe setIcon(Block block) {
        return setIcon(new ItemStack(block));
    }

    @Override
    public String getInternalName() {
        String held = name;
        if (held == null)
            throw new IllegalStateException("nameless recipe row read before bind: " + id);
        return held;
    }

    public String getLocalizedName() {
        String localized;
        if (customLocalization) {
            localized = I18nUtil.resolveKey(getInternalName());
        } else {
            ItemStack ic = getIcon();
            localized = ic.isEmpty() ? getInternalName() : ic.getHoverName().getString();
        }
        if (nameWrapper != null) localized = I18nUtil.resolveKey(nameWrapper, localized);
        return localized;
    }

    public boolean matchesSearch(String substring) {
        return getLocalizedName().toLowerCase(Locale.US).contains(substring.toLowerCase(Locale.US));
    }

    public List<String> print() {
        List<String> list = new ArrayList<>();
        header(list);
        autoSwitch(list);
        duration(list);
        power(list);
        input(list);
        output(list);
        return list;
    }

    protected void header(List<String> list) {
        list.add(ChatFormatting.YELLOW + getLocalizedName());
        if (ModifierKeys.leftShiftHeld())
            list.add(ChatFormatting.DARK_GRAY + "Internal: " + getInternalName());
    }

    protected void autoSwitch(List<String> list) {
        if (autoSwitchGroup != null) {
            for (String line :
                    I18nUtil.resolveKeyArray("autoswitch", I18nUtil.resolveKey(autoSwitchGroup))) {
                list.add(ChatFormatting.GOLD + line);
            }
        }
    }

    protected void duration(List<String> list) {
        if (duration > 0) {
            list.add(
                    ChatFormatting.RED
                            + I18nUtil.resolveKey("gui.recipe.duration")
                            + ": "
                            + duration / 20D
                            + "s");
        }
    }

    protected void power(List<String> list) {
        if (power > 0) {
            list.add(
                    ChatFormatting.RED
                            + I18nUtil.resolveKey("gui.recipe.consumption")
                            + ": "
                            + BobMathUtil.getShortNumber(power)
                            + "HE/t");
        }
    }

    protected void input(List<String> list) {
        list.add(ChatFormatting.BOLD + I18nUtil.resolveKey("gui.recipe.input") + ":");
        if (inputItem != null)
            for (CountIngredient ci : inputItem) {
                ItemStack display = ci.extractForCyclingDisplay(20);
                if (display.isEmpty()) continue;
                list.add(
                        "  "
                                + ChatFormatting.GRAY
                                + display.getCount()
                                + "x "
                                + display.getHoverName().getString());
            }
        for (FluidStackNTM fluid : inputFluid) list.add("  " + fluidLine(fluid));
    }

    protected void output(List<String> list) {
        list.add(ChatFormatting.BOLD + I18nUtil.resolveKey("gui.recipe.output") + ":");
        WeightedList<ItemStack>[] outputs = outputItems();
        if (outputs != null)
            for (WeightedList<ItemStack> slot : outputs) {
                for (String line : label(slot)) list.add("  " + line);
            }
        for (FluidStackNTM fluid : outputFluid) list.add("  " + fluidLine(fluid));
    }

    @Override
    public boolean matches(NtmRecipeInput input, Level level) {
        if (inputItem == null || inputItem.length == 0) return true;
        int[] ingredientForSlot = new int[input.size()];
        Arrays.fill(ingredientForSlot, -1);
        for (int ri = 0; ri < inputItem.length; ri++) {
            if (!assign(ri, input, ingredientForSlot, new boolean[ingredientForSlot.length]))
                return false;
        }
        return true;
    }

    private boolean assign(
            int ri, NtmRecipeInput input, int[] ingredientForSlot, boolean[] visitedSlot) {
        for (int si = 0; si < ingredientForSlot.length; si++) {
            if (visitedSlot[si]) continue;
            ItemStack stack = input.getItem(si);
            if (stack.isEmpty() || !inputItem[ri].test(stack)) continue;
            visitedSlot[si] = true;
            int owner = ingredientForSlot[si];
            if (owner == -1 || assign(owner, input, ingredientForSlot, visitedSlot)) {
                ingredientForSlot[si] = ri;
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(NtmRecipeInput input) {
        WeightedList<ItemStack>[] outputs = outputItems();
        if (outputs == null || outputs.length == 0) return ItemStack.EMPTY;
        return outputs[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    public @Nullable ResourceKey<Recipe<?>> recipeId() {
        return id;
    }

    public void bind(ResourceKey<Recipe<?>> id) {
        this.id = id;
        if (name == null) name = defaultName(id);
    }

    public static String defaultName(ResourceKey<Recipe<?>> id) {
        String path = id.identifier().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private Optional<String> authoredName() {
        return name == null || id != null && name.equals(defaultName(id))
                ? Optional.empty()
                : Optional.of(name);
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

    public boolean matchesInputFluids(FluidTankNTM[] inputTanks) {
        for (int i = 0; i < Math.min(inputFluid.length, inputTanks.length); i++) {
            if (!inputFluid[i].matches(inputTanks[i])) return false;
        }
        return true;
    }

    public boolean outputFluidsFit(FluidTankNTM[] outputTanks) {
        for (int i = 0; i < Math.min(outputFluid.length, outputTanks.length); i++) {
            if (!outputFluid[i].fitsInto(outputTanks[i])) return false;
        }
        return true;
    }

    public void drainInputFluids(FluidTankNTM[] inputTanks) {
        for (int i = 0; i < Math.min(inputFluid.length, inputTanks.length); i++) {
            inputFluid[i].drainFrom(inputTanks[i]);
        }
    }

    public void fillOutputFluids(FluidTankNTM[] outputTanks) {
        for (int i = 0; i < Math.min(outputFluid.length, outputTanks.length); i++) {
            outputFluid[i].fillInto(outputTanks[i]);
        }
    }

    public abstract GenericRecipes<?, ?> table();

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
        return INamedRecipe.BOOK_CATEGORY;
    }
}
