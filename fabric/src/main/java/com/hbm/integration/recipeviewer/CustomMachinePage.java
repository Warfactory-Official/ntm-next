// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.recipes.CustomMachineRecipe;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.block.ItemCustomMachine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

public final class CustomMachinePage extends TablePage<CustomMachineRecipe> {

    private static final int IN_X = 12, OUT_X = 102, STRIDE = 18;
    private static final int FLUID_Y = 6, ITEM_Y = 24, ITEM_Y2 = 42;
    private static final int MACHINE_X = 75, MACHINE_Y = 42;
    private static final int ROW = 3;

    private static final int SIDE = 160, RADIATION_Y = 63, POLLUTION_Y = 75, CENTRE_X = 83;
    private static final int HEAT_Y = 8, FLUX_Y = 16;

    private final ResourceKey<CustomMachineDefinition> key;
    private final CustomMachineDefinition definition;
    private final ItemStack machine;

    public CustomMachinePage(
            ResourceKey<CustomMachineDefinition> key, CustomMachineDefinition definition) {
        super(
                PageIds.page(
                        "custom_machine."
                                + key.identifier().getNamespace()
                                + "."
                                + key.identifier().getPath()),
                CustomMachineRecipe.class);
        this.key = key;
        this.definition = definition;
        this.machine = ItemCustomMachine.of(ModBlocks.CUSTOM_MACHINE.get(), key);
    }

    public ResourceKey<CustomMachineDefinition> key() {
        return key;
    }

    private static float chance(WeightedList<ItemStack> output) {
        int total = 0;
        int present = 0;
        for (Weighted<ItemStack> entry : output.unwrap()) {
            total += entry.weight();
            if (!entry.value().isEmpty()) present += entry.weight();
        }
        return total == 0 ? 0F : (float) present / total;
    }

    private static List<ItemStack> stacks(WeightedList<ItemStack> output) {
        List<ItemStack> out = new ArrayList<>();
        for (Weighted<ItemStack> entry : output.unwrap()) {
            if (!entry.value().isEmpty()) out.add(entry.value().copy());
        }
        return out;
    }

    private static int[] slotAt(int base, int index) {
        return index < ROW
                ? new int[] {base + index * STRIDE, ITEM_Y}
                : new int[] {base + (index - ROW) * STRIDE, ITEM_Y2};
    }

    private static void centred(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        RecipePanel.leftAligned(
                graphics, text, CENTRE_X - RecipePanel.font().width(text) / 2, y, colour);
    }

    @Override
    public Component title() {
        return definition.name();
    }

    @Override
    public ItemStack icon() {
        return machine;
    }

    @Override
    public int height() {
        return POLLUTION_Y + 10;
    }

    @Override
    public List<CustomMachineRecipe> rows() {
        return CustomMachineRecipes.byKey(definition.recipeKey());
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(machine);
    }

    @Override
    public void layout(CustomMachineRecipe recipe, PageLayout page) {
        for (int i = 0; i < Math.min(recipe.inputFluid.length, ROW); i++) {
            FluidStackNTM fluid = recipe.inputFluid[i];
            if (fluid.type() == Fluids.EMPTY) continue;
            page.input(IN_X + i * STRIDE, FLUID_Y).background().fluid(fluid);
        }
        for (int i = 0; i < Math.min(recipe.inputItem.length, ROW * 2); i++) {
            int[] at = slotAt(IN_X, i);
            page.input(at[0], at[1]).background().items(recipe.inputItem[i].displayStacks());
        }

        for (int i = 0; i < Math.min(recipe.outputFluid.length, ROW); i++) {
            FluidStackNTM fluid = recipe.outputFluid[i];
            if (fluid.type() == Fluids.EMPTY) continue;
            page.output(OUT_X + i * STRIDE, FLUID_Y).background().fluid(fluid);
        }
        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        for (int i = 0; i < Math.min(outputs.length, ROW * 2); i++) {
            int[] at = slotAt(OUT_X, i);
            PageSlot slot = page.output(at[0], at[1]).background().items(stacks(outputs[i]));
            float chance = chance(outputs[i]);

            if (chance < 1F) {
                slot.tooltip(
                        (shown, lines) ->
                                lines.accept(
                                        Component.translatable(
                                                        "jei.hbm.customMachine.chance",
                                                        (int) (chance * Outputs.RESOLUTION) / 10D)
                                                .withStyle(ChatFormatting.RED)));
            }
        }

        page.catalyst(MACHINE_X, MACHINE_Y).item(machine);
    }

    @Override
    public void draw(CustomMachineRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.CUSTOM);

        if (recipe.radiationAmount != 0F) {
            RecipePanel.rightAligned(
                    graphics,
                    Component.translatable(
                            "jei.hbm.customMachine.radiation", recipe.radiationAmount),
                    SIDE,
                    RADIATION_Y,
                    0xFF08FF00);
        }
        if (recipe.pollutionAmount != 0F && recipe.pollutionType != null) {
            RecipePanel.rightAligned(
                    graphics,
                    Component.translatable(
                            "jei.hbm.customMachine.pollution",
                            recipe.pollutionType.name(),
                            recipe.pollutionAmount),
                    SIDE,
                    POLLUTION_Y,
                    RecipePanel.TEXT);
        }
        if (definition.fluxMode()) {
            centred(
                    graphics,
                    Component.translatable("gui.customMachine.flux", recipe.flux),
                    FLUX_Y,
                    0xFF08FF00);
        }
        if (definition.maxHeat() > 0 && recipe.heat > 0) {
            centred(
                    graphics,
                    Component.translatable("jei.hbm.customMachine.heat", recipe.heat),
                    HEAT_Y,
                    0xFFFF0000);
        }
    }
}
