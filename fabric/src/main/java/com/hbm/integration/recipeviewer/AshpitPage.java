// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.items.machine.EnumCokeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class AshpitPage extends RecipePage<AshpitPage.Recipe> {

    AshpitPage() {
        super(PageIds.page("ashpit"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<ItemStack> ovens =
                List.of(
                        new ItemStack(ModBlocks.HEATER_FIREBOX),
                        new ItemStack(ModBlocks.HEATER_OVEN));
        List<ItemStack> chimneys =
                List.of(
                        new ItemStack(ModBlocks.CHIMNEY_BRICK),
                        new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL));
        List<ItemStack> industrial = List.of(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL));
        List<Recipe> rows = new ArrayList<>();
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(Items.COAL),
                                new ItemStack(ModItems.LIGNITE),
                                ModItems.COKE.stack(EnumCokeType.COAL)),
                        EnumAshType.COAL));
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(Items.OAK_LOG),
                                new ItemStack(Items.ACACIA_LOG),
                                new ItemStack(Items.OAK_PLANKS),
                                new ItemStack(Items.OAK_SAPLING)),
                        EnumAshType.WOOD));
        rows.add(
                fuel(
                        ovens,
                        List.of(
                                new ItemStack(ModItems.SOLID_FUEL),
                                new ItemStack(ModItems.SCRAP),
                                new ItemStack(ModItems.DUST),
                                new ItemStack(ModItems.ROCKET_FUEL)),
                        EnumAshType.MISC));

        for (Fluid smoke :
                List.of(NTMFluids.SMOKE, NTMFluids.SMOKE_LEADED, NTMFluids.SMOKE_POISON)) {
            String key = PageIds.segment(BuiltInRegistries.FLUID.getKey(smoke));
            rows.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(EnumAshType.FLY), key),
                            chimneys,
                            List.of(),
                            new FluidStackNTM(smoke, MachineData.ASHPIT_THRESHOLD_FLY.get()),
                            EnumAshType.FLY));
            rows.add(
                    new Recipe(
                            PageIds.derived(id(), PageIds.segment(EnumAshType.SOOT), key),
                            industrial,
                            List.of(),
                            new FluidStackNTM(smoke, MachineData.ASHPIT_THRESHOLD_SOOT.get()),
                            EnumAshType.SOOT));
        }
        return rows;
    }

    private Recipe fuel(List<ItemStack> ovens, List<ItemStack> fuels, EnumAshType ash) {
        return new Recipe(PageIds.derived(id(), PageIds.segment(ash)), ovens, fuels, null, ash);
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_ashpit");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ASHPIT);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ASHPIT));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        int[][] inputs = GenericMachinePage.universalInputPositions(2);
        page.input(inputs[0][0], inputs[0][1]).background().items(recipe.sources());
        PageSlot second = page.input(inputs[1][0], inputs[1][1]).background();
        if (recipe.smoke() != null) second.fluid(recipe.smoke());
        else second.items(recipe.fuels());
        page.output(102, 24).background().item(ModItems.POWDER_ASH.stack(recipe.ash()));
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ASHPIT.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id,
            List<ItemStack> sources,
            List<ItemStack> fuels,
            @Nullable FluidStackNTM smoke,
            EnumAshType ash) {}
}
