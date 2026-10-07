// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public record BedrockOreConfig(
        boolean auto, List<Entry> entries, BlockState depthRock, RuleTest host)
        implements FeatureConfiguration {
    private static final RuleTest NATURAL_STONE = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);

    public static final Codec<BedrockOreConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            Codec.BOOL
                                                    .fieldOf("auto")
                                                    .forGetter(BedrockOreConfig::auto),
                                            Entry.CODEC
                                                    .listOf()
                                                    .fieldOf("entries")
                                                    .forGetter(BedrockOreConfig::entries),
                                            BlockState.CODEC
                                                    .fieldOf("depth_rock")
                                                    .forGetter(BedrockOreConfig::depthRock),
                                            RuleTest.CODEC
                                                    .optionalFieldOf("host", NATURAL_STONE)
                                                    .forGetter(BedrockOreConfig::host))
                                    .apply(inst, BedrockOreConfig::new));

    public BedrockOreConfig(boolean auto, List<Entry> entries, BlockState depthRock) {
        this(auto, entries, depthRock, NATURAL_STONE);
    }

    public record Entry(Item item, int count, int color, int tier, int weight) {
        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(
                        inst ->
                                inst.group(
                                                BuiltInRegistries.ITEM
                                                        .byNameCodec()
                                                        .fieldOf("item")
                                                        .forGetter(Entry::item),
                                                Codec.INT.fieldOf("count").forGetter(Entry::count),
                                                Codec.INT.fieldOf("color").forGetter(Entry::color),
                                                Codec.INT.fieldOf("tier").forGetter(Entry::tier),
                                                Codec.INT
                                                        .fieldOf("weight")
                                                        .forGetter(Entry::weight))
                                        .apply(inst, Entry::new));
    }
}
