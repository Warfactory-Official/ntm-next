// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record MissileStruct(
        @Nullable ItemCustomMissilePart warhead,
        @Nullable ItemCustomMissilePart fuselage,
        @Nullable ItemCustomMissilePart fins,
        @Nullable ItemCustomMissilePart thruster) {

    private static final Codec<ItemCustomMissilePart> PART =
            BuiltInRegistries.ITEM
                    .byNameCodec()
                    .comapFlatMap(
                            item ->
                                    item instanceof ItemCustomMissilePart part
                                            ? DataResult.success(part)
                                            : DataResult.error(() -> "not a missile part: " + item),
                            part -> part);

    public static final Codec<MissileStruct> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            PART.optionalFieldOf("warhead")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.warhead())),
                                            PART.optionalFieldOf("fuselage")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.fuselage())),
                                            PART.optionalFieldOf("fins")
                                                    .forGetter(s -> Optional.ofNullable(s.fins())),
                                            PART.optionalFieldOf("thruster")
                                                    .forGetter(
                                                            s -> Optional.ofNullable(s.thruster())))
                                    .apply(
                                            instance,
                                            (w, f, s, t) ->
                                                    new MissileStruct(
                                                            w.orElse(null),
                                                            f.orElse(null),
                                                            s.orElse(null),
                                                            t.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileStruct> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.warhead()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.fuselage()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.fins()),
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs::optional),
                    s -> Optional.ofNullable(s.thruster()),
                    (w, f, s, t) -> new MissileStruct(cast(w), cast(f), cast(s), cast(t)));

    public static final MissileStruct EMPTY =
            new MissileStruct((ItemCustomMissilePart) null, null, null, null);

    public MissileStruct(ItemStack w, ItemStack f, ItemStack s, ItemStack t) {
        this(
                ItemCustomMissilePart.part(w),
                ItemCustomMissilePart.part(f),
                ItemCustomMissilePart.part(s),
                ItemCustomMissilePart.part(t));
    }

    private static @Nullable ItemCustomMissilePart cast(Optional<? extends Item> item) {
        return item.orElse(null) instanceof ItemCustomMissilePart part ? part : null;
    }

    public MissileStruct sanitised() {
        return new MissileStruct(
                slot(this.warhead, PartType.WARHEAD),
                slot(this.fuselage, PartType.FUSELAGE),
                slot(this.fins, PartType.FINS),
                slot(this.thruster, PartType.THRUSTER));
    }

    private static @Nullable ItemCustomMissilePart slot(
            @Nullable ItemCustomMissilePart part, PartType expected) {
        return part != null && part.type == expected ? part : null;
    }

    public float height() {
        float height = 0F;
        if (this.warhead != null && this.warhead.type == PartType.WARHEAD)
            height += this.warhead.height;
        if (this.fuselage != null && this.fuselage.type == PartType.FUSELAGE)
            height += this.fuselage.height;
        if (this.thruster != null && this.thruster.type == PartType.THRUSTER)
            height += this.thruster.height;
        return height;
    }

    public boolean isComplete() {
        return this.warhead != null && this.fuselage != null && this.thruster != null;
    }
}
