// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.integration.recipeviewer.PageLayout;
import com.hbm.integration.recipeviewer.PageSlot;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

final class JeiPageCategory<T> implements IRecipeCategory<T> {

    private final RecipePage<T> page;
    private final IRecipeType<T> type;
    private final IDrawable icon;
    private final long unitsPerMilliBucket;

    JeiPageCategory(
            RecipePage<T> page, IRecipeType<T> type, IGuiHelper helper, long unitsPerMilliBucket) {
        this.page = page;
        this.type = type;
        this.icon = helper.createDrawableItemStack(page.icon());
        this.unitsPerMilliBucket = unitsPerMilliBucket;
    }

    @Override
    public IRecipeType<T> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return page.title();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return page.width();
    }

    @Override
    public int getHeight() {
        return page.height();
    }

    @Override
    public Identifier getIdentifier(T row) {
        return page.rowId(row);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T row, IFocusGroup focuses) {
        page.layout(row, new SlotLayout(builder));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T row, IFocusGroup focuses) {
        page.layout(row, new ExtrasLayout(builder));
    }

    @Override
    public void draw(
            T row,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor graphics,
            double mouseX,
            double mouseY) {
        page.draw(row, graphics);
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip, T row, IRecipeSlotsView slots, double mouseX, double mouseY) {
        page.tooltip(row, mouseX, mouseY, tooltip::add);
    }

    RowIngredients ingredients(T row, IIngredientManager manager, IPlatformFluidHelper<?> fluids) {
        IngredientLayout layout = new IngredientLayout(manager, fluids);
        page.layout(row, layout);
        return new RowIngredients(layout.inputs, layout.outputs);
    }

    record RowIngredients(List<ITypedIngredient<?>> inputs, List<ITypedIngredient<?>> outputs) {}

    private final class SlotLayout implements PageLayout {

        private final IRecipeLayoutBuilder builder;

        SlotLayout(IRecipeLayoutBuilder builder) {
            this.builder = builder;
        }

        private PageSlot slot(RecipeIngredientRole role, int x, int y) {
            return new Slot(builder.addSlot(role, x, y));
        }

        @Override
        public PageSlot input(int x, int y) {
            return slot(RecipeIngredientRole.INPUT, x, y);
        }

        @Override
        public PageSlot output(int x, int y) {
            return slot(RecipeIngredientRole.OUTPUT, x, y);
        }

        @Override
        public PageSlot catalyst(int x, int y) {
            return slot(RecipeIngredientRole.CRAFTING_STATION, x, y);
        }

        @Override
        public PageSlot display(int x, int y) {
            return slot(RecipeIngredientRole.RENDER_ONLY, x, y);
        }

        @Override
        public void arrow(int x, int y) {}

        @Override
        public void text(Component text, int x, int y, int width, int height) {}
    }

    private final class Slot implements PageSlot {

        private final IRecipeSlotBuilder slot;

        Slot(IRecipeSlotBuilder slot) {
            this.slot = slot;
        }

        @Override
        public PageSlot background() {
            slot.setStandardSlotBackground();
            return this;
        }

        @Override
        public PageSlot item(ItemStack stack) {
            slot.add(stack);
            return this;
        }

        @Override
        public PageSlot items(List<ItemStack> stacks) {
            slot.addItemStacks(stacks);
            return this;
        }

        @Override
        public PageSlot fluid(Fluid fluid, long milliBuckets, int pressure) {
            long units = milliBuckets * unitsPerMilliBucket;
            slot.setFluidRenderer(units, false, 16, 16).add(fluid, units);
            if (pressure > 0) {
                slot.addRichTooltipCallback(
                        (view, tooltip) ->
                                FluidTraitTooltip.addPressureInfo(pressure, tooltip::add));
            }
            return this;
        }

        @Override
        public PageSlot fluid(Fluid fluid) {
            slot.add(fluid, 1000L * unitsPerMilliBucket);
            return this;
        }

        @Override
        public PageSlot tooltip(Tooltip tooltip) {
            slot.addRichTooltipCallback(
                    (view, lines) -> {
                        if (view.getDisplayedIngredient().isEmpty()) return;
                        tooltip.append(view.getDisplayedItemStack().orElse(null), lines::add);
                    });
            return this;
        }
    }

    private static final class ExtrasLayout implements PageLayout {

        private static final PageSlot IGNORED =
                new PageSlot() {
                    @Override
                    public PageSlot background() {
                        return this;
                    }

                    @Override
                    public PageSlot item(ItemStack stack) {
                        return this;
                    }

                    @Override
                    public PageSlot items(List<ItemStack> stacks) {
                        return this;
                    }

                    @Override
                    public PageSlot fluid(Fluid fluid, long milliBuckets, int pressure) {
                        return this;
                    }

                    @Override
                    public PageSlot fluid(Fluid fluid) {
                        return this;
                    }

                    @Override
                    public PageSlot tooltip(Tooltip tooltip) {
                        return this;
                    }
                };

        private final IRecipeExtrasBuilder builder;

        ExtrasLayout(IRecipeExtrasBuilder builder) {
            this.builder = builder;
        }

        @Override
        public PageSlot input(int x, int y) {
            return IGNORED;
        }

        @Override
        public PageSlot output(int x, int y) {
            return IGNORED;
        }

        @Override
        public PageSlot catalyst(int x, int y) {
            return IGNORED;
        }

        @Override
        public PageSlot display(int x, int y) {
            return IGNORED;
        }

        @Override
        public void arrow(int x, int y) {
            builder.addRecipeArrowWidget().setPosition(x, y);
        }

        @Override
        public void text(Component text, int x, int y, int width, int height) {
            builder.addText(text, width, height).setPosition(x, y);
        }
    }

    private final class IngredientLayout implements PageLayout {

        private final List<ITypedIngredient<?>> inputs = new ArrayList<>();
        private final List<ITypedIngredient<?>> outputs = new ArrayList<>();
        private final IIngredientManager manager;
        private final IPlatformFluidHelper<?> fluids;

        IngredientLayout(IIngredientManager manager, IPlatformFluidHelper<?> fluids) {
            this.manager = manager;
            this.fluids = fluids;
        }

        private <V> void add(List<ITypedIngredient<?>> into, IIngredientType<V> type, V value) {
            manager.createTypedIngredient(type, value, false).ifPresent(into::add);
        }

        private <V> void addFluid(
                List<ITypedIngredient<?>> into,
                IPlatformFluidHelper<V> helper,
                Fluid fluid,
                long units) {
            add(
                    into,
                    helper.getFluidIngredientType(),
                    helper.create(fluid.builtInRegistryHolder(), units));
        }

        private PageSlot collect(List<ITypedIngredient<?>> into) {
            return new PageSlot() {
                @Override
                public PageSlot background() {
                    return this;
                }

                @Override
                public PageSlot item(ItemStack stack) {
                    add(into, VanillaTypes.ITEM_STACK, stack);
                    return this;
                }

                @Override
                public PageSlot items(List<ItemStack> stacks) {
                    for (ItemStack stack : stacks) add(into, VanillaTypes.ITEM_STACK, stack);
                    return this;
                }

                @Override
                public PageSlot fluid(Fluid fluid, long milliBuckets, int pressure) {
                    addFluid(into, fluids, fluid, milliBuckets * unitsPerMilliBucket);
                    return this;
                }

                @Override
                public PageSlot fluid(Fluid fluid) {
                    addFluid(into, fluids, fluid, 1000L * unitsPerMilliBucket);
                    return this;
                }

                @Override
                public PageSlot tooltip(Tooltip tooltip) {
                    return this;
                }
            };
        }

        @Override
        public PageSlot input(int x, int y) {
            return collect(inputs);
        }

        @Override
        public PageSlot output(int x, int y) {
            return collect(outputs);
        }

        @Override
        public PageSlot catalyst(int x, int y) {
            return ExtrasLayout.IGNORED;
        }

        @Override
        public PageSlot display(int x, int y) {
            return ExtrasLayout.IGNORED;
        }

        @Override
        public void arrow(int x, int y) {}

        @Override
        public void text(Component text, int x, int y, int width, int height) {}
    }
}
