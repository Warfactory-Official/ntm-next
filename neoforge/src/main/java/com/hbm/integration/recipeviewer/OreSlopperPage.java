// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blockentity.machine.BlockEntityMachineOreSlopper;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import com.hbm.items.special.ItemBedrockOreNew;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class OreSlopperPage extends RecipePage<OreSlopperPage.Recipe> {

    OreSlopperPage() {
        super(PageIds.page("ore_slopper"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (ItemStack base : BedrockOreSet.SAMPLES.get().get()) {
            BedrockOreSet set = ItemBedrockOreBase.sample(base).set();
            List<ItemStack> ores = new ArrayList<>();
            for (BedrockOreKind type : set.kinds())
                ores.add(ItemBedrockOreNew.make(BedrockOreGrade.BASE, type));
            Identifier id =
                    PageIds.derived(
                            id(), PageIds.segment(BuiltInRegistries.ITEM.getKey(base.getItem())));
            if (set != BedrockOreSet.NTM) id = PageIds.derived(id, set.id());
            rows.add(new Recipe(id, base, ores));
        }
        return rows;
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("block.hbm.machine_ore_slopper");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_ORE_SLOPPER);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_ORE_SLOPPER));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        int[][] inputs = GenericMachinePage.universalInputPositions(2);
        page.input(inputs[0][0], inputs[0][1])
                .background()
                .fluid(NTMFluids.WATER, BlockEntityMachineOreSlopper.waterUsed);
        page.input(inputs[1][0], inputs[1][1]).background().item(recipe.input());

        List<ItemStack> ores = recipe.ores();
        int[][] outputs = GenericMachinePage.outputPositions(ores.size() + 1);
        for (int i = 0; i < ores.size(); i++) {
            page.output(outputs[i][0], outputs[i][1]).background().item(ores.get(i));
        }
        page.output(outputs[ores.size()][0], outputs[ores.size()][1])
                .background()
                .fluid(NTMFluids.SLOP, BlockEntityMachineOreSlopper.waterUsed);
        page.catalyst(75, 31).item(ModBlocks.MACHINE_ORE_SLOPPER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(Identifier id, ItemStack input, List<ItemStack> ores) {}
}
