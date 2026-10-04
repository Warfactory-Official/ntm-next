// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public record ConsumableTreatment(
        HolderSet<Item> targets,
        Trigger trigger,
        boolean blockWhenPotionSick,
        Optional<Holder<Item>> remainder,
        Optional<Holder<SoundEvent>> sound,
        List<TreatmentAction> actions) {
    public static final ResourceKey<Registry<ConsumableTreatment>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("consumable_treatment"));
    public static final Codec<ConsumableTreatment> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<ConsumableTreatment>mapCodec(
                                    i ->
                                            i.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.ITEM)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::targets),
                                                            Trigger.CODEC
                                                                    .fieldOf("trigger")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::trigger),
                                                            Codec.BOOL
                                                                    .optionalFieldOf(
                                                                            "block_when_potion_sick",
                                                                            false)
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::blockWhenPotionSick),
                                                            net.minecraft.resources
                                                                    .RegistryFixedCodec.create(
                                                                            Registries.ITEM)
                                                                    .optionalFieldOf("remainder")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::remainder),
                                                            SoundEvent.CODEC
                                                                    .optionalFieldOf("sound")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::sound),
                                                            TreatmentAction.CODEC
                                                                    .listOf()
                                                                    .fieldOf("actions")
                                                                    .forGetter(
                                                                            ConsumableTreatment
                                                                                    ::actions))
                                                    .apply(i, ConsumableTreatment::new)))
                    .codec()
                    .validate(
                            rule ->
                                    rule.trigger == Trigger.CONSUME && rule.remainder.isPresent()
                                            ? DataResult.error(
                                                    () ->
                                                            "consume treatments use the item's vanilla remainder component")
                                            : DataResult.success(rule));

    public enum Trigger implements net.minecraft.util.StringRepresentable {
        CONSUME,
        USE;
        public static final Codec<Trigger> CODEC =
                net.minecraft.util.StringRepresentable.fromEnum(Trigger::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
