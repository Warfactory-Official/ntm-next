// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.config.InteractionConfig;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.platform.Services;
import com.hbm.util.BobMathUtil;
import com.hbm.util.GameTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class GenericMachinePage<T extends GenericRecipe> extends TablePage<T> {

    private final Supplier<List<T>> rows;
    private final Component title;
    private final ItemLike machine;
    private final List<ItemLike> catalysts;
    private final Layout layout;
    private final Extras extras;
    private final boolean fluidInputsFirst;
    private final boolean fluidOutputsFirst;
    private final FluidView fluidView;
    private final boolean secretRows;

    private GenericMachinePage(Builder<T> builder) {
        super(builder.id, builder.rowType);
        this.rows = builder.rows;
        this.title = builder.title;
        this.machine = builder.machine;
        List<ItemLike> catalysts = new ArrayList<>(builder.catalysts.size() + 1);
        catalysts.add(machine);
        catalysts.addAll(builder.catalysts);
        this.catalysts = List.copyOf(catalysts);
        this.layout = builder.layout;
        this.extras = builder.extras;
        this.fluidInputsFirst = builder.fluidInputsFirst;
        this.fluidOutputsFirst = builder.fluidOutputsFirst;
        this.fluidView = builder.fluidView;
        this.secretRows = builder.secretRows;
    }

    public static <T extends GenericRecipe> Builder<T> builder(
            String path,
            Class<? extends T> rowType,
            GenericRecipes<T, ?> table,
            String title,
            ItemLike machine,
            Layout layout) {
        return new Builder<>(
                PageIds.page(path), rowType, table, Component.translatable(title), machine, layout);
    }

    private static List<FluidStackNTM> fluids(FluidStackNTM @Nullable [] array) {
        if (array == null) return List.of();
        List<FluidStackNTM> out = new ArrayList<>(array.length);
        for (FluidStackNTM fluid : array) if (fluid.type() != Fluids.EMPTY) out.add(fluid);
        return out;
    }

    static int[][] inputPositions(int count) {
        if (count == 1) return new int[][] {{48, 24}};
        if (count == 2) return new int[][] {{30, 24}, {48, 24}};
        if (count == 3) return new int[][] {{12, 24}, {30, 24}, {48, 24}};
        if (count == 4) return new int[][] {{30, 15}, {48, 15}, {30, 33}, {48, 33}};
        if (count == 5) return new int[][] {{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
        if (count == 6)
            return new int[][] {{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};

        int[][] positions = new int[count][2];
        int columns = (count + 2) / 3;
        for (int i = 0; i < count; i++) {
            positions[i][0] = 12 + i % columns * 18 - (columns == 4 ? 18 : 0);
            positions[i][1] = 6 + i / columns * 18;
        }
        return positions;
    }

    static int[][] universalInputPositions(int count) {
        if (count == 1) return new int[][] {{48, 24}};
        if (count == 2) return new int[][] {{48, 24}, {30, 24}};
        if (count == 3) return new int[][] {{48, 24}, {30, 24}, {12, 24}};
        if (count == 4) return new int[][] {{48, 15}, {30, 15}, {48, 33}, {30, 33}};
        if (count == 5) return new int[][] {{48, 15}, {30, 15}, {12, 24}, {48, 33}, {30, 33}};
        if (count == 6)
            return new int[][] {{48, 15}, {30, 15}, {12, 15}, {48, 33}, {30, 33}, {12, 33}};
        if (count == 7)
            return new int[][] {
                {48, 6}, {30, 15}, {12, 15}, {48, 24}, {30, 33}, {12, 33}, {48, 42}
            };
        if (count == 8) {
            return new int[][] {
                {48, 6}, {30, 6}, {12, 15}, {48, 24}, {30, 24}, {12, 33}, {48, 42}, {30, 42}
            };
        }
        if (count == 9) {
            return new int[][] {
                {48, 6}, {30, 6}, {12, 6}, {48, 24}, {30, 24}, {12, 24}, {48, 42}, {30, 42},
                {12, 42}
            };
        }

        int[][] positions = new int[count][2];
        for (int i = 0; i < count; i++) {
            positions[i][0] = i % 4 * 18;
            positions[i][1] = i / 4 * 18;
        }
        return positions;
    }

    static int[][] outputPositions(int count) {
        return switch (count) {
            case 0 -> new int[0][2];
            case 1 -> new int[][] {{102, 24}};
            case 2 -> new int[][] {{102, 24}, {120, 24}};
            case 3 -> new int[][] {{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][] {{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            case 5 -> new int[][] {{102, 15}, {120, 15}, {102, 33}, {120, 33}, {138, 24}};
            case 6 -> new int[][] {{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}};
            case 7 ->
                    new int[][] {
                        {102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}
                    };
            case 8 ->
                    new int[][] {
                        {102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24},
                        {138, 42}
                    };

            default -> grid(count, 102, 6, 4);
        };
    }

    private static List<ItemStack> outputStacks(WeightedList<ItemStack> output) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Weighted<ItemStack> entry : output.unwrap()) stacks.add(entry.value().copy());
        return stacks;
    }

    private static List<ItemStack> blueprintStacks(String[] pools) {
        List<ItemStack> blueprints = new ArrayList<>();
        for (String pool : pools) blueprints.add(ItemBlueprints.make(pool));
        return blueprints;
    }

    private static int inputOffset(int inputCount) {
        return inputCount > 12 ? -9 : inputCount > 9 ? 18 : 0;
    }

    private static int machineOffset(int inputCount) {
        return inputCount > 12 ? 27 : inputCount > 9 ? 18 : 0;
    }

    private static int length(Object[] array) {
        return array == null ? 0 : array.length;
    }

    private static int[][] grid(int count, int x, int y, int columns) {
        int[][] positions = new int[count][2];
        for (int i = 0; i < count; i++) {
            positions[i][0] = x + i % columns * 18;
            positions[i][1] = y + i / columns * 18;
        }
        return positions;
    }

    static Component ticks(int duration) {
        return Component.translatable("jei.hbm.duration", BobMathUtil.getShortNumber(duration));
    }

    static Component consumption(long power) {
        return Component.translatable("jei.hbm.consumption", BobMathUtil.getShortNumber(power));
    }

    private static boolean isSecret(GenericRecipe recipe) {
        if (recipe.isPooled()) {
            for (String pool : recipe.getPools()) {
                if (pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return true;
            }
        }

        Set<Item> excluded = new HashSet<>();
        for (var member : ModItems.ITEM_SECRET) excluded.add(member.get());
        var ladder = ModItems.METEORITE_SWORDS;
        for (var sword : ladder.subList(1, ladder.size())) excluded.add(sword.get());
        if (recipe.inputItem != null) {
            for (CountIngredient input : recipe.inputItem) {
                if (input.ingredient().items().anyMatch(item -> excluded.contains(item.value())))
                    return true;
            }
        }
        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        if (outputs != null) {
            for (WeightedList<ItemStack> output : outputs) {
                for (Weighted<ItemStack> weighted : output.unwrap()) {
                    if (excluded.contains(weighted.value().getItem())) return true;
                }
            }
        }
        return false;
    }

    @Override
    public Component title() {
        return title;
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(machine);
    }

    @Override
    public List<T> rows() {
        return rows.get();
    }

    @Override
    public List<ItemStack> catalysts() {
        return catalysts.stream().map(ItemStack::new).toList();
    }

    @Override
    public boolean hidden(T row) {

        return secretRows && InteractionConfig.recipeViewerHidesSecrets && isSecret(row);
    }

    @Override
    public void layout(T recipe, PageLayout page) {
        List<FluidStackNTM> shownInputs = fluidView.inputs().apply(fluids(recipe.inputFluid));
        List<FluidStackNTM> shownOutputs = fluidView.outputs().apply(fluids(recipe.outputFluid));
        int inputCount = length(recipe.inputItem) + shownInputs.size();
        int outputCount = length(recipe.outputItems()) + shownOutputs.size();
        int inputOffset = layout == Layout.ASSEMBLY ? inputOffset(inputCount) : 0;
        int machineOffset = layout == Layout.ASSEMBLY ? machineOffset(inputCount) : 0;
        int[][] inputs =
                layout == Layout.UNIVERSAL
                        ? universalInputPositions(inputCount)
                        : inputPositions(inputCount);
        int[][] outputs = outputPositions(outputCount);

        int slot = 0;
        if (fluidInputsFirst) slot = fluidSlots(page, shownInputs, inputs, inputOffset, slot, true);
        slot = inputItems(page, recipe, inputs, inputOffset, slot);
        if (!fluidInputsFirst) fluidSlots(page, shownInputs, inputs, inputOffset, slot, true);

        slot = 0;
        if (fluidOutputsFirst)
            slot = fluidSlots(page, shownOutputs, outputs, machineOffset, slot, false);
        slot = outputItems(page, recipe, outputs, machineOffset, slot);
        if (!fluidOutputsFirst) fluidSlots(page, shownOutputs, outputs, machineOffset, slot, false);

        boolean hasBlueprint = recipe.isPooled();
        if (hasBlueprint) {
            List<ItemStack> blueprints = blueprintStacks(recipe.getPools());
            if (!blueprints.isEmpty()) page.catalyst(75 + machineOffset, 10).items(blueprints);
        }

        page.catalyst(75 + machineOffset, hasBlueprint ? 38 : 31).item(machine);
    }

    private int inputItems(PageLayout page, T recipe, int[][] inputs, int offset, int slot) {
        if (recipe.inputItem == null) return slot;
        for (CountIngredient input : recipe.inputItem) {
            page.input(inputs[slot][0] + offset, inputs[slot][1])
                    .background()
                    .items(input.displayStacks());
            slot++;
        }
        return slot;
    }

    private int fluidSlots(
            PageLayout page,
            List<FluidStackNTM> shown,
            int[][] positions,
            int offset,
            int slot,
            boolean input) {
        for (FluidStackNTM fluid : shown) {
            int x = positions[slot][0] + offset;
            int y = positions[slot][1];
            (input ? page.input(x, y) : page.output(x, y)).background().fluid(fluid);
            slot++;
        }
        return slot;
    }

    private int outputItems(PageLayout page, T recipe, int[][] outputs, int offset, int slot) {
        if (recipe.outputItems() == null) return slot;
        for (WeightedList<ItemStack> output : recipe.outputItems()) {
            page.output(outputs[slot][0] + offset, outputs[slot][1])
                    .background()
                    .items(outputStacks(output));
            slot++;
        }
        return slot;
    }

    @Override
    public void draw(T recipe, GuiGraphicsExtractor graphics) {
        int machineOffset =
                layout == Layout.ASSEMBLY
                        ? machineOffset(
                                length(recipe.inputItem)
                                        + fluidView
                                                .inputs()
                                                .apply(fluids(recipe.inputFluid))
                                                .size())
                        : 0;
        if (recipe.isPooled()) {
            RecipePanel.operationWithTemplate(graphics, 74 + machineOffset);
        } else {
            RecipePanel.operation(graphics, 74 + machineOffset);
        }
        extras.draw(graphics, recipe);
    }

    public static final class Builder<T extends GenericRecipe> {

        private final Identifier id;
        private final Class<? extends T> rowType;
        private Supplier<List<T>> rows;
        private final Component title;
        private final ItemLike machine;
        private final Layout layout;
        private final List<ItemLike> catalysts = new ArrayList<>();
        private Extras extras = Extras.DURATION_AND_POWER;
        private boolean fluidInputsFirst;
        private boolean fluidOutputsFirst;
        private FluidView fluidView = FluidView.TABLE;
        private boolean secretRows;

        private Builder(
                Identifier id,
                Class<? extends T> rowType,
                GenericRecipes<T, ?> table,
                Component title,
                ItemLike machine,
                Layout layout) {
            this.id = id;
            this.rowType = rowType;
            this.rows = table::recipes;
            this.title = title;
            this.machine = machine;
            this.layout = layout;
        }

        public Builder<T> extras(Extras extras) {
            this.extras = extras;
            return this;
        }

        public Builder<T> fluidInputsFirst() {
            this.fluidInputsFirst = true;
            return this;
        }

        public Builder<T> fluidOutputsFirst() {
            this.fluidOutputsFirst = true;
            return this;
        }

        public Builder<T> fluidView(FluidView fluidView) {
            this.fluidView = fluidView;
            return this;
        }

        public Builder<T> catalysts(ItemLike... machines) {
            catalysts.addAll(List.of(machines));
            return this;
        }

        public Builder<T> rows(Supplier<List<T>> rows) {
            this.rows = rows;
            return this;
        }

        public Builder<T> secretRows() {
            this.secretRows = true;
            return this;
        }

        public GenericMachinePage<T> build() {
            return new GenericMachinePage<>(this);
        }
    }

    public record FluidView(
            UnaryOperator<List<FluidStackNTM>> inputs, UnaryOperator<List<FluidStackNTM>> outputs) {

        public static final FluidView TABLE =
                new FluidView(UnaryOperator.identity(), UnaryOperator.identity());

        public static FluidView batch(int head, int factor) {
            return batch(head, 0, factor);
        }

        public static FluidView batch(int head, int headPressure, int factor) {
            return new FluidView(
                    list -> {
                        List<FluidStackNTM> out = new ArrayList<>(list.size());
                        for (int i = 0; i < list.size(); i++) {
                            FluidStackNTM fluid = list.get(i);
                            out.add(
                                    i == 0
                                            ? new FluidStackNTM(fluid.type(), head, headPressure)
                                            : new FluidStackNTM(
                                                    fluid.type(),
                                                    fluid.amount() * factor,
                                                    fluid.pressure()));
                        }
                        return out;
                    },
                    list -> {
                        List<FluidStackNTM> out = new ArrayList<>(list.size());
                        for (FluidStackNTM fluid : list) {
                            out.add(
                                    new FluidStackNTM(
                                            fluid.type(),
                                            fluid.amount() * factor,
                                            fluid.pressure()));
                        }
                        return out;
                    });
        }

        public static UnaryOperator<List<FluidStackNTM>> append(
                java.util.function.Supplier<FluidStackNTM> extra) {
            return list -> {
                List<FluidStackNTM> out = new ArrayList<>(list);
                out.add(extra.get());
                return out;
            };
        }
    }

    public enum Layout {
        GENERIC,
        ASSEMBLY,
        UNIVERSAL
    }

    public enum Extras {
        DURATION_AND_POWER {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {
                RecipePanel.rightAligned(graphics, ticks(recipe.duration), 45, RecipePanel.TEXT);
                RecipePanel.rightAligned(graphics, consumption(recipe.power), 57, RecipePanel.TEXT);
            }
        },
        DURATION_ONLY {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {
                RecipePanel.rightAligned(graphics, ticks(recipe.duration), 45, RecipePanel.TEXT);
            }
        },
        ONE_GREEN_LINE {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {
                RecipePanel.rightAligned(
                        graphics,
                        Component.translatable(
                                "jei.hbm.duration_and_consumption",
                                BobMathUtil.getShortNumber(recipe.duration),
                                BobMathUtil.getShortNumber(recipe.power)),
                        57,
                        0xFF306030);
            }
        },
        IGNITION {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {
                RecipePanel.rightAligned(graphics, ticks(recipe.duration), 45, RecipePanel.TEXT);

                if (GameTime.now() % 2000 < 1000) {
                    RecipePanel.rightAligned(
                            graphics, consumption(recipe.power), 57, RecipePanel.TEXT);
                } else {
                    RecipePanel.rightAligned(graphics, ignition(recipe), 57, 0xFFA000A0);
                }
            }

            private Component ignition(GenericRecipe recipe) {
                if (recipe instanceof FusionRecipe fusion) {
                    return Component.translatable(
                            "jei.hbm.ignition_ky", BobMathUtil.getShortNumber(fusion.ignitionTemp));
                }
                return Component.translatable(
                        "jei.hbm.ignition_tu",
                        BobMathUtil.getShortNumber(((PlasmaForgeRecipe) recipe).ignitionTemp));
            }
        },
        EFFECTIVENESS {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {
                float productivity = ((CrystallizerRecipe) recipe).productivity;
                if (productivity <= 0F) return;

                RecipePanel.leftAligned(
                        graphics,
                        Component.translatable(
                                "jei.hbm.effectiveness", Math.min((int) (productivity * 100), 99)),
                        8,
                        52,
                        RecipePanel.TEXT);
            }
        },
        NONE {
            @Override
            void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe) {}
        };

        abstract void draw(GuiGraphicsExtractor graphics, GenericRecipe recipe);
    }
}
