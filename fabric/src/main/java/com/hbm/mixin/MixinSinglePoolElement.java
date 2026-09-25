// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.ImpactWorldgen;
import com.hbm.world.gen.nbt.ImpactVillageProcessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SinglePoolElement.class)
public abstract class MixinSinglePoolElement {

    @WrapOperation(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/pools/SinglePoolElement;getSettings(Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"))
    private StructurePlaceSettings hbm$impactVillageRuin(
            SinglePoolElement element,
            Rotation rotation,
            BoundingBox chunkBox,
            LiquidSettings liquidSettings,
            boolean keepJigsaws,
            Operation<StructurePlaceSettings> original) {
        StructurePlaceSettings settings =
                original.call(element, rotation, chunkBox, liquidSettings, keepJigsaws);
        if (ImpactWorldgen.VILLAGE_RUIN.isBound())
            settings.addProcessor(ImpactVillageProcessor.INSTANCE);
        return settings;
    }
}
