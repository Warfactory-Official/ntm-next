// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ArcFurnaceRecipe;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.ItemScraps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ArcFurnaceMoltenPage extends RecipePage<ArcFurnaceMoltenPage.Row> {

    ArcFurnaceMoltenPage() {
        super(PageIds.page("arc_furnace_fluid"), Row.class);
    }

    @Override
    public List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (ArcFurnaceRecipe recipe : ArcFurnaceRecipes.INSTANCE.pageRows()) {
            MaterialStack[] molten = recipe.fluidOutput();
            if (molten == null || molten.length == 0) continue;
            List<ItemStack> outputs = new ArrayList<>();
            for (MaterialStack stack : molten) outputs.add(ItemScraps.create(stack, true));
            rows.add(new Row(recipe.recipeId().identifier(), recipe.inputItem[0], null, outputs));
        }

        for (NTMMaterial mat : Mats.orderedList) {
            if (mat.smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) continue;
            MaterialStack ingot = new MaterialStack(mat, MaterialShapes.INGOT.q(1));
            rows.add(
                    new Row(
                            PageIds.derived(id(), mat.tagPath),
                            null,
                            ItemScraps.create(ingot, false),
                            List.of(ItemScraps.create(ingot, true))));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Row row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_arc_furnace");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ARC_FURNACE);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ARC_FURNACE));
    }

    @Override
    public void layout(Row row, PageLayout page) {
        page.input(48, 24)
                .background()
                .items(
                        row.ingredient() == null
                                ? List.of(row.scraps())
                                : row.ingredient().displayStacks());
        int[][] positions = GenericMachinePage.outputPositions(row.outputs().size());
        for (int i = 0; i < row.outputs().size(); i++) {
            page.output(positions[i][0], positions[i][1]).background().item(row.outputs().get(i));
        }
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ARC_FURNACE.get());
    }

    @Override
    public void draw(Row row, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Row(
            Identifier id,
            @Nullable CountIngredient ingredient,
            @Nullable ItemStack scraps,
            List<ItemStack> outputs) {}
}
