// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class GrenadeAssemblyPage extends RecipePage<GrenadeAssemblyPage.Recipe> {

    GrenadeAssemblyPage() {
        super(PageIds.page("grenade_assembly"), Recipe.class);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> recipes = new ArrayList<>();
        for (EnumGrenadeShell shell : EnumGrenadeShell.values()) {
            for (EnumGrenadeFilling filling : EnumGrenadeFilling.values()) {
                if (!GrenadeCraftingRecipe.compatible(shell, filling)) continue;
                for (EnumGrenadeFuze fuze : EnumGrenadeFuze.values()) {
                    recipes.add(make(shell, filling, fuze, null));
                    for (EnumGrenadeExtra extra : EnumGrenadeExtra.values())
                        recipes.add(make(shell, filling, fuze, extra));
                }
            }
        }
        return recipes;
    }

    private Recipe make(
            EnumGrenadeShell shell,
            EnumGrenadeFilling filling,
            EnumGrenadeFuze fuze,
            @Nullable EnumGrenadeExtra extra) {
        String[] segments =
                Stream.<Enum<?>>of(shell, filling, fuze, extra)
                        .filter(Objects::nonNull)
                        .map(PageIds::segment)
                        .toArray(String[]::new);
        return new Recipe(
                PageIds.derived(id(), segments),
                ModItems.GRENADE_SHELL.stack(shell),
                ModItems.GRENADE_FILLING.stack(filling),
                ModItems.GRENADE_FUZE.stack(fuze),
                extra == null ? null : ModItems.GRENADE_EXTRA.stack(extra),
                ItemGrenadeUniversal.make(shell, filling, fuze, extra));
    }

    @Override
    public Identifier rowId(Recipe row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("item.hbm.grenade_universal");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(Items.CRAFTING_TABLE));
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {

        int[][] positions =
                GenericMachinePage.universalInputPositions(recipe.extra() == null ? 3 : 4);
        page.input(positions[0][0], positions[0][1]).background().item(recipe.shell());
        page.input(positions[1][0], positions[1][1]).background().item(recipe.filling());
        page.input(positions[2][0], positions[2][1]).background().item(recipe.fuze());
        if (recipe.extra() != null)
            page.input(positions[3][0], positions[3][1]).background().item(recipe.extra());
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(Items.CRAFTING_TABLE);
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            Identifier id,
            ItemStack shell,
            ItemStack filling,
            ItemStack fuze,
            @Nullable ItemStack extra,
            ItemStack output) {}
}
