// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.rei;

import com.hbm.client.gui.FluidGauge;
import com.hbm.integration.recipeviewer.PageLayout;
import com.hbm.integration.recipeviewer.PageSlot;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePages;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.lib.Library;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.client.entry.renderer.ForwardingEntryRenderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

final class ReiPageDisplay implements Display {

    static final Identifier SERIALIZER_ID = Library.id("recipe_page");
    static final DisplaySerializer<ReiPageDisplay> SERIALIZER =
            DisplaySerializer.of(
                    RecordCodecBuilder.<Key>mapCodec(
                                    instance ->
                                            instance.group(
                                                            Identifier.CODEC
                                                                    .fieldOf("page")
                                                                    .forGetter(Key::page),
                                                            Identifier.CODEC
                                                                    .fieldOf("row")
                                                                    .forGetter(Key::row))
                                                    .apply(instance, Key::new))
                            .flatXmap(
                                    ReiPageDisplay::resolve,
                                    display -> DataResult.success(display.key())),
                    StreamCodec.composite(
                                    Identifier.STREAM_CODEC,
                                    Key::page,
                                    Identifier.STREAM_CODEC,
                                    Key::row,
                                    Key::new)
                            .map(
                                    key -> resolve(key).getOrThrow(DecoderException::new),
                                    ReiPageDisplay::key)
                            .cast());

    private final RecipePage<?> page;
    private final Object row;
    private final Identifier rowId;
    private final boolean hidden;
    private final List<Slot> slots = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();
    private final List<int[]> arrows = new ArrayList<>();
    private final List<EntryIngredient> inputs = new ArrayList<>();
    private final List<EntryIngredient> required = new ArrayList<>();
    private final List<EntryIngredient> outputs = new ArrayList<>();

    private <T> ReiPageDisplay(RecipePage<T> page, T row) {
        this.page = page;
        this.row = row;
        this.rowId = page.rowId(row);
        this.hidden = page.hidden(row);
        page.layout(row, new Recorder());
        for (Slot slot : slots) {
            slot.finish();
            if (slot.entries.isEmpty()) continue;
            EntryIngredient ingredient = EntryIngredient.of(slot.entries);
            switch (slot.role) {
                case INPUT -> {
                    inputs.add(ingredient);
                    required.add(ingredient);
                }

                case CATALYST -> inputs.add(ingredient);
                case OUTPUT -> outputs.add(ingredient);
                case DISPLAY -> {}
            }
        }
    }

    static <T> ReiPageDisplay of(RecipePage<T> page, T row) {
        return new ReiPageDisplay(page, row);
    }

    private static DataResult<ReiPageDisplay> resolve(Key key) {
        List<RecipePage<?>> pages = new ArrayList<>(RecipePages.fixed());
        pages.addAll(RecipePages.customMachines());
        for (RecipePage<?> page : pages) {
            if (page.id().equals(key.page())) return resolve(page, key.row());
        }
        return DataResult.error(() -> "no recipe page " + key.page());
    }

    private static <T> DataResult<ReiPageDisplay> resolve(RecipePage<T> page, Identifier rowId) {
        for (T row : page.rows()) {
            if (page.rowId(row).equals(rowId)) return DataResult.success(of(page, row));
        }
        return DataResult.error(() -> page.id() + " holds no row " + rowId);
    }

    private Key key() {
        return new Key(page.id(), rowId);
    }

    RecipePage<?> page() {
        return page;
    }

    Object row() {
        return row;
    }

    boolean hidden() {
        return hidden;
    }

    List<Slot> slots() {
        return slots;
    }

    List<Text> texts() {
        return texts;
    }

    List<int[]> arrows() {
        return arrows;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getRequiredEntries() {
        return required;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(page.id());
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(rowId);
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }

    enum Role {
        INPUT,
        OUTPUT,
        CATALYST,
        DISPLAY
    }

    private record Key(Identifier page, Identifier row) {}

    record Text(Component text, int x, int y, int width, int height) {}

    static final class Slot implements PageSlot {

        final Role role;
        final int x;
        final int y;
        final List<EntryStack<?>> entries = new ArrayList<>();
        private final Map<EntryStack<?>, Tooltip> fluidLines = new IdentityHashMap<>();
        private final List<Tooltip> tooltips = new ArrayList<>();
        boolean background;

        Slot(Role role, int x, int y) {
            this.role = role;
            this.x = x;
            this.y = y;
        }

        @Override
        public PageSlot background() {
            background = true;
            return this;
        }

        @Override
        public PageSlot item(ItemStack stack) {
            entries.add(EntryStacks.of(stack));
            return this;
        }

        @Override
        public PageSlot items(List<ItemStack> stacks) {
            for (ItemStack stack : stacks) item(stack);
            return this;
        }

        @Override
        public PageSlot fluid(Fluid fluid, long milliBuckets, int pressure) {
            return fluid(
                    fluid,
                    milliBuckets * FluidStack.bucketAmount() / 1000L,
                    (shown, lines) -> {
                        lines.accept(Component.literal(milliBuckets + "mB"));
                        if (pressure > 0) FluidTraitTooltip.addPressureInfo(pressure, lines);
                    });
        }

        @Override
        public PageSlot fluid(Fluid fluid) {
            return fluid(fluid, FluidStack.bucketAmount(), (shown, lines) -> {});
        }

        private PageSlot fluid(Fluid fluid, long amount, Tooltip amountLines) {
            EntryStack<FluidStack> stack = EntryStacks.of(fluid, amount);
            stack.setting(EntryStack.Settings.FLUID_AMOUNT_VISIBLE, false);

            stack.withRenderer(
                    entry ->
                            new ForwardingEntryRenderer<>(
                                    EntryRendererRegistry.getInstance().get(entry)) {
                                @Override
                                public void render(
                                        EntryStack<FluidStack> entry,
                                        GuiGraphics graphics,
                                        Rectangle bounds,
                                        int mouseX,
                                        int mouseY,
                                        float delta) {
                                    FluidGauge.still(
                                            graphics,
                                            entry.getValue().getFluid(),
                                            bounds.x,
                                            bounds.y,
                                            bounds.width,
                                            bounds.height);
                                }
                            });
            fluidLines.put(
                    stack,
                    (shown, lines) -> {
                        amountLines.append(shown, lines);

                        FluidTraitTooltip.addInfo(fluid, lines);
                    });
            entries.add(stack);
            return this;
        }

        @Override
        public PageSlot tooltip(Tooltip tooltip) {
            tooltips.add(tooltip);
            return this;
        }

        private void finish() {
            for (EntryStack<?> entry : entries) {
                Tooltip fluid = fluidLines.get(entry);
                if (fluid != null || !tooltips.isEmpty()) process(entry, fluid);
            }
        }

        private <T> void process(EntryStack<T> entry, @Nullable Tooltip fluid) {
            entry.tooltipProcessor(
                    (shown, lines) -> {
                        ItemStack stack = shown.getValue() instanceof ItemStack item ? item : null;
                        if (fluid != null) fluid.append(stack, lines::add);
                        for (Tooltip tooltip : tooltips) tooltip.append(stack, lines::add);
                        return lines;
                    });
        }
    }

    private final class Recorder implements PageLayout {

        private PageSlot slot(Role role, int x, int y) {
            Slot slot = new Slot(role, x, y);
            slots.add(slot);
            return slot;
        }

        @Override
        public PageSlot input(int x, int y) {
            return slot(Role.INPUT, x, y);
        }

        @Override
        public PageSlot output(int x, int y) {
            return slot(Role.OUTPUT, x, y);
        }

        @Override
        public PageSlot catalyst(int x, int y) {
            return slot(Role.CATALYST, x, y);
        }

        @Override
        public PageSlot display(int x, int y) {
            return slot(Role.DISPLAY, x, y);
        }

        @Override
        public void arrow(int x, int y) {
            arrows.add(new int[] {x, y});
        }

        @Override
        public void text(Component text, int x, int y, int width, int height) {
            texts.add(new Text(text, x, y, width, height));
        }
    }
}
