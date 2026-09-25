// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class GasCentrifugePage extends RecipePage<GasCentrifugePage.Cascade> {

    private static final int MAX_STEPS = 32;

    GasCentrifugePage() {
        super(PageIds.page("gas_centrifuge"), Cascade.class);
    }

    @Override
    public List<Cascade> rows() {
        GasCentrifugeRecipes table = GasCentrifugeRecipes.INSTANCE;
        List<Cascade> cascades = new ArrayList<>();

        List<GasCentrifugeRecipe> feeds = new ArrayList<>();
        for (GasCentrifugeRecipe recipe : table.allStages()) {
            if (recipe.feed != null) feeds.add(recipe);
        }

        feeds.sort(
                Comparator.comparingInt((GasCentrifugeRecipe r) -> r.order)
                        .thenComparing((GasCentrifugeRecipe r) -> r.stage));

        for (GasCentrifugeRecipe feed : feeds) {
            List<GasCentrifugeRecipe> chain = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            GasCentrifugeRecipe recipe = feed;
            while (recipe != null && seen.add(recipe.stage) && chain.size() < MAX_STEPS) {
                chain.add(recipe);
                Cascade cascade = build(feed.feed, chain);
                if (cascade != null) cascades.add(cascade);
                recipe = recipe.produced > 0 ? table.byStage(recipe.next) : null;
            }
        }

        return cascades;
    }

    private static @Nullable Cascade build(Fluid feed, List<GasCentrifugeRecipe> chain) {
        int size = chain.size();
        GasCentrifugeRecipe last = chain.get(size - 1);

        for (GasCentrifugeRecipe recipe : chain) if (recipe.consumed <= 0) return null;

        boolean handsOver = last.produced > 0 && last.next != null;
        GasCentrifugeRecipe leftover =
                handsOver ? GasCentrifugeRecipes.INSTANCE.byStage(last.next) : null;
        if (handsOver && (leftover == null || leftover.deadEndVolume <= 0)) return null;

        long[] num = new long[size];
        long[] den = new long[size];
        num[0] = 1;
        den[0] = 1;
        for (int k = 1; k < size; k++) {
            long n = num[k - 1] * chain.get(k - 1).produced;
            long d = den[k - 1] * chain.get(k).consumed;
            long g = gcd(n, d);
            num[k] = n / g;
            den[k] = d / g;
        }

        long batches = 1;
        for (int k = 0; k < size; k++) batches = lcm(batches, den[k]);
        if (leftover != null) {

            long n = num[size - 1] * last.produced;
            long d = den[size - 1] * (long) leftover.deadEndVolume;
            batches = lcm(batches, d / gcd(n, d));
        }

        List<Step> steps = new ArrayList<>(size);
        List<ItemStack> outputs = new ArrayList<>();
        List<List<ItemStack>> alternatives = new ArrayList<>();
        boolean requiresUpgrade = false;

        for (int k = 0; k < size; k++) {
            long operations = batches * num[k] / den[k];
            GasCentrifugeRecipe recipe = chain.get(k);
            steps.add(new Step(recipe, operations));
            requiresUpgrade |= recipe.requiresUpgrade;
            collect(recipe.outputItems(), operations, outputs, alternatives);
        }

        Step deadEnd = null;
        if (leftover != null) {
            long runs =
                    (batches * num[size - 1] * last.produced / den[size - 1])
                            / leftover.deadEndVolume;
            deadEnd = new Step(leftover, runs);
            for (ItemStack stack : leftover.deadEndItems()) merge(outputs, stack, runs);
        }

        int volume = (int) (batches * chain.get(0).consumed);
        return new Cascade(
                chain.get(0).recipeId().identifier().withSuffix("/" + size),
                feed,
                volume,
                List.copyOf(steps),
                deadEnd,
                List.copyOf(outputs),
                List.copyOf(alternatives),
                requiresUpgrade);
    }

    private static void collect(
            WeightedList<ItemStack> @Nullable [] slots,
            long operations,
            List<ItemStack> outputs,
            List<List<ItemStack>> alternatives) {
        if (slots == null) return;
        for (WeightedList<ItemStack> slot : slots) {
            List<ItemStack> entries = new ArrayList<>();
            for (Weighted<ItemStack> entry : slot.unwrap()) {
                if (!entry.value().isEmpty()) entries.add(entry.value());
            }
            if (entries.isEmpty()) continue;
            if (entries.size() == 1) {
                merge(outputs, entries.get(0), operations);
            } else {

                List<ItemStack> copies = new ArrayList<>(entries.size());
                for (ItemStack entry : entries) copies.add(entry.copy());
                alternatives.add(List.copyOf(copies));
            }
        }
    }

    private static void merge(List<ItemStack> outputs, ItemStack stack, long runs) {
        int count = (int) Math.min(Integer.MAX_VALUE, stack.getCount() * runs);
        for (ItemStack held : outputs) {
            if (ItemStack.isSameItemSameComponents(held, stack)) {
                held.setCount(held.getCount() + count);
                return;
            }
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        outputs.add(copy);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }

    private static long lcm(long a, long b) {
        return a / gcd(a, b) * b;
    }

    private static Component ladder(Cascade cascade) {
        StringBuilder line = new StringBuilder();
        for (Step step : cascade.steps()) {
            if (!line.isEmpty()) line.append(" > ");
            line.append(step.operations())
                    .append("x ")
                    .append(GasCentrifugeRecipe.stageName(step.recipe().stage));
        }
        Step deadEnd = cascade.deadEnd();
        if (deadEnd != null) {
            line.append(" > ")
                    .append(deadEnd.operations())
                    .append("x ")
                    .append(GasCentrifugeRecipe.stageName(deadEnd.recipe().stage));
        }
        return Component.literal(line.toString());
    }

    @Override
    public Identifier rowId(Cascade row) {
        return row.id();
    }

    @Override
    public Component title() {
        return Component.translatable("gui.jei.category.gasCentrifuge");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.MACHINE_GASCENT);
    }

    @Override
    public int height() {
        return 80;
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(new ItemStack(ModBlocks.MACHINE_GASCENT));
    }

    @Override
    public void layout(Cascade cascade, PageLayout page) {
        page.input(5, 14).fluid(cascade.feed(), cascade.volume());

        ItemStack machines = new ItemStack(ModBlocks.MACHINE_GASCENT, cascade.steps().size());
        page.catalyst(56, 14).background().item(machines);

        if (cascade.requiresUpgrade())
            page.catalyst(56, 32).background().item(ModItems.UPGRADE_GC_SPEED.get());

        int slot = 0;
        for (ItemStack output : cascade.outputs()) {
            page.output(84 + slot % 4 * 18, 14 + slot / 4 * 18).background().item(output);
            slot++;
        }
        for (List<ItemStack> alternative : cascade.alternatives()) {
            page.output(84 + slot % 4 * 18, 14 + slot / 4 * 18).background().items(alternative);
            slot++;
        }

        page.arrow(27, 15);

        page.text(
                Component.translatable("jei.hbm.gas_centrifuge.cascade", cascade.steps().size()),
                3,
                51,
                width() - 6,
                9);
        page.text(ladder(cascade), 3, 61, width() - 6, 18);
    }

    public record Step(GasCentrifugeRecipe recipe, long operations) {}

    public record Cascade(
            Identifier id,
            Fluid feed,
            int volume,
            List<Step> steps,
            @Nullable Step deadEnd,
            List<ItemStack> outputs,
            List<List<ItemStack>> alternatives,
            boolean requiresUpgrade) {}
}
