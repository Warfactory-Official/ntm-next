// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.rei;

import com.hbm.client.gui.ScreenAnvil;
import com.hbm.client.gui.ScreenMachineAutocrafter;
import com.hbm.client.gui.ScreenMachineCustom;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.integration.recipeviewer.CraftingRows;
import com.hbm.integration.recipeviewer.CustomMachinePage;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePages;
import com.hbm.tileentity.machine.BlockEntityMachineAutocrafter;
import dev.architectury.event.EventResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.filtering.base.BasicFilteringRule;
import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

public class NTMReiClientPlugin implements REIClientPlugin {

    private final List<CustomMachinePage> customMachines = new ArrayList<>();

    @Override
    public void registerEntryRenderers(EntryRendererRegistry registry) {

        registry.transformTooltip(
                VanillaEntryTypes.FLUID,
                (entry, mouse, tooltip) -> {
                    if (tooltip != null)
                        QMAWClient.fluidTooltip(entry.getValue().getFluid(), tooltip::add);
                    return tooltip;
                });
    }

    private List<RecipePage<?>> pages() {
        List<RecipePage<?>> pages = new ArrayList<>(RecipePages.fixed());
        pages.addAll(customMachines);
        return pages;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        customMachines.clear();
        customMachines.addAll(RecipePages.customMachines());
        for (RecipePage<?> page : pages()) {
            registry.add(new ReiPageCategory(page));
            registry.addWorkstations(
                    CategoryIdentifier.of(page.id()),
                    page.catalysts().stream()
                            .map(EntryIngredients::of)
                            .toArray(EntryIngredient[]::new));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (RecipePage<?> page : pages()) addRows(registry, page);
        for (RecipeHolder<CraftingRecipe> row : CraftingRows.rows()) {
            ShapelessCraftingRecipeDisplay display =
                    (ShapelessCraftingRecipeDisplay) row.value().display().getFirst();
            registry.add(
                    new DefaultCustomShapelessDisplay(
                            display.ingredients().stream()
                                    .map(EntryIngredients::ofSlotDisplay)
                                    .toList(),
                            List.of(EntryIngredients.ofSlotDisplay(display.result())),
                            Optional.of(row.id().identifier())));
        }
        registry.registerVisibilityPredicate(
                (category, display) ->
                        display instanceof ReiPageDisplay page
                                        && (page.hidden() || !ReiLookupFilter.answers(page))
                                ? EventResult.interruptFalse()
                                : EventResult.pass());
    }

    private static <T> void addRows(DisplayRegistry registry, RecipePage<T> page) {
        for (T row : page.rows()) registry.add(ReiPageDisplay.of(page, row));
    }

    @Override
    public void registerBasicEntryFiltering(BasicFilteringRule<?> rule) {
        rule.hide(
                () -> {
                    List<EntryStack<?>> hidden = new ArrayList<>();

                    for (ItemStack stack : RecipePages.hiddenStacks()) {
                        hidden.add(EntryStacks.of(stack));
                        hidden.add(EntryStacks.of(stack.getItem()));
                    }
                    return hidden;
                });
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        for (RecipePages.ClickArea area : RecipePages.clickAreas()) {
            CategoryIdentifier<?> category =
                    area.page().equals(RecipePages.VANILLA_SMELTING)
                            ? BuiltinPlugin.SMELTING
                            : CategoryIdentifier.of(area.page());
            clickArea(
                    registry,
                    area.screen(),
                    new Rectangle(area.x(), area.y(), area.width(), area.height()),
                    category);
        }

        for (CustomMachinePage page : customMachines) {
            registry.registerContainerClickArea(
                    screen ->
                            page.key().equals(screen.getMenu().blockEntity().machineType)
                                    ? new Rectangle(
                                            RecipePages.CUSTOM_MACHINE_X,
                                            RecipePages.CUSTOM_MACHINE_Y,
                                            RecipePages.CUSTOM_MACHINE_WIDTH,
                                            RecipePages.CUSTOM_MACHINE_HEIGHT)
                                    : new Rectangle(),
                    ScreenMachineCustom.class,
                    CategoryIdentifier.of(page.id()));
        }
        registry.registerDraggableStackVisitor(new AutocrafterPatterns());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void clickArea(
            ScreenRegistry registry,
            Class<? extends AbstractContainerScreen<?>> screen,
            Rectangle area,
            CategoryIdentifier<?> category) {
        registry.registerContainerClickArea(area, (Class) screen, category);
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(
                ScreenAnvil.class,
                screen -> {
                    Rect2i panel = screen.recipePanelArea();
                    return List.of(
                            new Rectangle(
                                    panel.getX(),
                                    panel.getY(),
                                    panel.getWidth(),
                                    panel.getHeight()));
                });
    }

    private static final class AutocrafterPatterns
            implements DraggableStackVisitor<ScreenMachineAutocrafter> {

        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return screen instanceof ScreenMachineAutocrafter;
        }

        private static Stream<Rect2i> areas(ScreenMachineAutocrafter screen) {
            return IntStream.range(0, BlockEntityMachineAutocrafter.GRID_SIZE)
                    .mapToObj(slot -> screen.patternArea(slot));
        }

        @Override
        public DraggedAcceptorResult acceptDraggedStack(
                DraggingContext<ScreenMachineAutocrafter> context, DraggableStack stack) {
            if (!(stack.getStack().getValue() instanceof ItemStack dropped))
                return DraggedAcceptorResult.PASS;
            ScreenMachineAutocrafter screen = context.getScreen();
            var at = context.getCurrentPosition();
            for (int i = 0; i < BlockEntityMachineAutocrafter.GRID_SIZE; i++) {
                if (screen.patternArea(i).contains(at.x, at.y)) {
                    screen.sendPattern(i, dropped);
                    return DraggedAcceptorResult.ACCEPTED;
                }
            }
            return DraggedAcceptorResult.PASS;
        }

        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(
                DraggingContext<ScreenMachineAutocrafter> context, DraggableStack stack) {
            if (!(stack.getStack().getValue() instanceof ItemStack)) return Stream.empty();
            return areas(context.getScreen())
                    .map(
                            area ->
                                    BoundsProvider.ofRectangle(
                                            new Rectangle(
                                                    area.getX(),
                                                    area.getY(),
                                                    area.getWidth(),
                                                    area.getHeight())));
        }
    }
}
