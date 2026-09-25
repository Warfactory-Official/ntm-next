// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BedrockOreSample(
        double lightMetal,
        double heavyMetal,
        double rareEarth,
        double actinide,
        double nonMetal,
        double crystalline) {

    public static final BedrockOreSample EMPTY = new BedrockOreSample(0D, 0D, 0D, 0D, 0D, 0D);
    public static final Codec<BedrockOreSample> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.DOUBLE
                                                    .fieldOf("light")
                                                    .forGetter(BedrockOreSample::lightMetal),
                                            Codec.DOUBLE
                                                    .fieldOf("heavy")
                                                    .forGetter(BedrockOreSample::heavyMetal),
                                            Codec.DOUBLE
                                                    .fieldOf("rare")
                                                    .forGetter(BedrockOreSample::rareEarth),
                                            Codec.DOUBLE
                                                    .fieldOf("actinide")
                                                    .forGetter(BedrockOreSample::actinide),
                                            Codec.DOUBLE
                                                    .fieldOf("nonmetal")
                                                    .forGetter(BedrockOreSample::nonMetal),
                                            Codec.DOUBLE
                                                    .fieldOf("crystal")
                                                    .forGetter(BedrockOreSample::crystalline))
                                    .apply(instance, BedrockOreSample::new));
    public static final StreamCodec<ByteBuf, BedrockOreSample> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BedrockOreSample decode(ByteBuf buffer) {
                    return new BedrockOreSample(
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble());
                }

                @Override
                public void encode(ByteBuf buffer, BedrockOreSample sample) {
                    buffer.writeDouble(sample.lightMetal);
                    buffer.writeDouble(sample.heavyMetal);
                    buffer.writeDouble(sample.rareEarth);
                    buffer.writeDouble(sample.actinide);
                    buffer.writeDouble(sample.nonMetal);
                    buffer.writeDouble(sample.crystalline);
                }
            };

    public double amount(BedrockOreType type) {
        return switch (type) {
            case LIGHT_METAL -> lightMetal;
            case HEAVY_METAL -> heavyMetal;
            case RARE_EARTH -> rareEarth;
            case ACTINIDE -> actinide;
            case NON_METAL -> nonMetal;
            case CRYSTALLINE -> crystalline;
        };
    }
}
