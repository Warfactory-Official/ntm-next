// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BedrockOreSample(BedrockOreSet set, double[] amounts) {

    public static final BedrockOreSample EMPTY =
            new BedrockOreSample(BedrockOreSet.NTM, new double[BedrockOreSet.NTM.kinds().size()]);
    public static final Codec<BedrockOreSample> CODEC =
            new MapCodec<BedrockOreSample>() {
                @Override
                public <T> Stream<T> keys(DynamicOps<T> ops) {
                    return Stream.concat(
                                    Stream.of("set"),
                                    BedrockOreSet.NTM.kinds().stream().map(BedrockOreKind::suffix))
                            .map(ops::createString);
                }

                @Override
                public <T> DataResult<BedrockOreSample> decode(
                        DynamicOps<T> ops, MapLike<T> input) {
                    T named = input.get("set");
                    DataResult<BedrockOreSet> set =
                            named == null
                                    ? DataResult.success(BedrockOreSet.NTM)
                                    : BedrockOreSet.SETS.codec().parse(ops, named);
                    return set.flatMap(
                            found -> {
                                double[] amounts = new double[found.kinds().size()];
                                for (int i = 0; i < amounts.length; i++) {
                                    String suffix = found.kinds().get(i).suffix();
                                    T value = input.get(suffix);
                                    if (value == null)
                                        return DataResult.error(
                                                () -> "bedrock ore sample lacks " + suffix);
                                    DataResult<Number> number = ops.getNumberValue(value);
                                    if (number.error().isPresent())
                                        return DataResult.error(() -> suffix + ": not a number");
                                    amounts[i] = number.getOrThrow().doubleValue();
                                }
                                return DataResult.success(new BedrockOreSample(found, amounts));
                            });
                }

                @Override
                public <T> RecordBuilder<T> encode(
                        BedrockOreSample sample, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                    if (sample.set != BedrockOreSet.NTM)
                        prefix.add("set", ops.createString(sample.set.id()));
                    for (int i = 0; i < sample.amounts.length; i++) {
                        prefix.add(
                                sample.set.kinds().get(i).suffix(),
                                ops.createDouble(sample.amounts[i]));
                    }
                    return prefix;
                }
            }.codec();
    public static final StreamCodec<ByteBuf, BedrockOreSample> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BedrockOreSample decode(ByteBuf buffer) {
                    BedrockOreSet set =
                            BedrockOreSet.SETS.byIndex(ByteBufCodecs.VAR_INT.decode(buffer));
                    double[] amounts = new double[set.kinds().size()];
                    for (int i = 0; i < amounts.length; i++) amounts[i] = buffer.readDouble();
                    return new BedrockOreSample(set, amounts);
                }

                @Override
                public void encode(ByteBuf buffer, BedrockOreSample sample) {
                    ByteBufCodecs.VAR_INT.encode(buffer, BedrockOreSet.SETS.index(sample.set));
                    for (double amount : sample.amounts) buffer.writeDouble(amount);
                }
            };

    public BedrockOreSample {
        if (amounts.length != set.kinds().size()) {
            throw new IllegalArgumentException(
                    set.id() + " holds " + set.kinds().size() + " types, not " + amounts.length);
        }
    }

    public double amount(BedrockOreKind kind) {
        int index = set.indexOf(kind);
        return index < 0 ? 0D : amounts[index];
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BedrockOreSample sample
                && sample.set == set
                && Arrays.equals(sample.amounts, amounts);
    }

    @Override
    public int hashCode() {
        return 31 * set.hashCode() + Arrays.hashCode(amounts);
    }

    @Override
    public String toString() {
        return "BedrockOreSample[" + set.id() + ", " + Arrays.toString(amounts) + "]";
    }
}
