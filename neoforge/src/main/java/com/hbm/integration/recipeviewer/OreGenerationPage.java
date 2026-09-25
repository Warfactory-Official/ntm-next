// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.world.ore.OreGeneration;
import com.hbm.world.ore.OreGenerationProfile;
import com.hbm.world.ore.OreHeightProfile;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OreGenerationPage extends RecipePage<OreGenerationProfile> {
    private static final int GRAPH_X = 26;
    private static final int GRAPH_Y = 48;
    private static final int GRAPH_WIDTH = 204;
    private static final int GRAPH_HEIGHT = 30;

    OreGenerationPage() {
        super(PageIds.page("ore_generation"), OreGenerationProfile.class);
    }

    @Override
    public Component title() {
        return text("title");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.IRON_PICKAXE);
    }

    @Override
    public int width() {
        return 240;
    }

    @Override
    public int height() {
        return 152;
    }

    @Override
    public List<OreGenerationProfile> rows() {
        List<OreGenerationProfile> profiles =
                Minecraft.getInstance().getSingleplayerServer() == null
                        ? OreGeneration.clientProfiles()
                        : OreGeneration.serverProfiles();
        return profiles.stream()
                .filter(
                        profile ->
                                profile.blocks().stream()
                                        .anyMatch(
                                                id ->
                                                        BuiltInRegistries.BLOCK
                                                                        .getValue(id)
                                                                        .asItem()
                                                                != Items.AIR))
                .toList();
    }

    @Override
    public Identifier rowId(OreGenerationProfile row) {
        return row.id();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of();
    }

    @Override
    public void layout(OreGenerationProfile row, PageLayout layout) {
        List<ItemStack> outputs =
                row.blocks().stream()
                        .map(BuiltInRegistries.BLOCK::getValue)
                        .map(block -> new ItemStack(block.asItem()))
                        .filter(stack -> !stack.isEmpty())
                        .toList();
        layout.output(4, 4).background().items(outputs);
        layout.text(text("dimension", name("dimension", row.dimension())), 26, 3, 208, 11);
        layout.text(text("server_profile"), 26, 16, 208, 11);
        OreHeightProfile height = row.height();
        if (height.hasGraph()) {
            layout.text(text("origin_probability"), 4, 31, 232, 11);
        } else {
            layout.text(
                    text(height.shape() == OreHeightProfile.Shape.UNKNOWN ? "no_graph" : "spatial"),
                    GRAPH_X,
                    GRAPH_Y,
                    GRAPH_WIDTH,
                    GRAPH_HEIGHT);
        }
        if (height.shape() != OreHeightProfile.Shape.UNKNOWN) {
            layout.text(
                    text(
                            height.hasGraph() ? "origin_band" : "block_band",
                            height.minY(),
                            height.maxY()),
                    4,
                    82,
                    232,
                    11);
        }
        layout.text(
                row.attempts() >= 0
                        ? text("attempts", number(row.attempts()))
                        : text("attempts_unknown"),
                4,
                95,
                232,
                11);
        layout.text(
                row.veinSize() >= 0 ? text("size", row.veinSize()) : text("not_density"),
                4,
                108,
                232,
                11);
        MutableComponent details = Component.empty();
        for (Component condition : row.conditions()) {
            if (!details.getSiblings().isEmpty()) details.append("\n");
            details.append(condition);
        }
        details.append("\n").append(text("biomes", biomeNames(row)));
        layout.text(details, 4, 124, 232, 23);
    }

    private static Component biomeNames(OreGenerationProfile row) {
        MutableComponent names = Component.empty();
        for (Identifier biome : row.biomes()) {
            if (!names.getSiblings().isEmpty()) names.append(", ");
            names.append(name("biome", biome));
        }
        return names;
    }

    private static Component name(String kind, Identifier id) {
        if (id.getNamespace().equals("hbm") && id.getPath().startsWith("inline_" + kind + "/"))
            return text("unnamed_biome");
        if (kind.equals("dimension") && id.getNamespace().equals("minecraft")) {
            String key =
                    switch (id.getPath()) {
                        case "overworld" -> "flat_world_preset.minecraft.overworld";
                        case "the_nether" -> "advancements.nether.root.title";
                        case "the_end" -> "biome.minecraft.the_end";
                        default -> null;
                    };
            if (key != null) return Component.translatable(key);
        }
        return Component.translatableWithFallback(
                kind + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                id.toString());
    }

    @Override
    public void draw(OreGenerationProfile row, GuiGraphicsExtractor graphics) {
        OreHeightProfile height = row.height();
        if (!height.hasGraph()) return;
        int bottom = GRAPH_Y + GRAPH_HEIGHT;
        graphics.fill(GRAPH_X - 1, GRAPH_Y, GRAPH_X, bottom + 1, 0xFF555555);
        graphics.fill(GRAPH_X, bottom, GRAPH_X + GRAPH_WIDTH, bottom + 1, 0xFF555555);
        double max = height.peak();
        for (int x = 0; x < GRAPH_WIDTH; x++) {
            int y = heightAt(height, x);
            int pixels =
                    Math.max(1, (int) Math.round(height.probability(y) / max * (GRAPH_HEIGHT - 2)));
            graphics.fill(GRAPH_X + x, bottom - pixels, GRAPH_X + x + 1, bottom, 0xFF43874B);
        }
        var font = Minecraft.getInstance().font;
        graphics.text(font, "0", 15, bottom - font.lineHeight, 0xFF333333, false);
        Component peak = text("peak", number(max * 100));
        graphics.text(
                font,
                peak,
                GRAPH_X + GRAPH_WIDTH - font.width(peak),
                GRAPH_Y - font.lineHeight,
                0xFF333333,
                false);
    }

    @Override
    public void tooltip(
            OreGenerationProfile row, double mouseX, double mouseY, Consumer<Component> output) {
        if (!row.height().hasGraph()
                || mouseX < GRAPH_X
                || mouseX >= GRAPH_X + GRAPH_WIDTH
                || mouseY < GRAPH_Y
                || mouseY > GRAPH_Y + GRAPH_HEIGHT) return;
        int y = heightAt(row.height(), (int) (mouseX - GRAPH_X));
        output.accept(text("point", y, number(row.height().probability(y) * 100)));
        output.accept(text("not_density"));
    }

    private static int heightAt(OreHeightProfile height, int x) {
        return (int)
                (height.minY()
                        + Math.round(
                                ((long) height.maxY() - height.minY())
                                        * (x / (double) (GRAPH_WIDTH - 1))));
    }

    private static String number(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.4g", value);
    }

    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("ore.hbm." + key, args);
    }
}
