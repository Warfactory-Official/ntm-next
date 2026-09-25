// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.util.datafix.HbmDataFixers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleRegionStorage.class)
public abstract class MixinSimpleRegionStorage {

    @Shadow @Final private DataFixTypes dataFixType;

    @ModifyReturnValue(
            method =
                    "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"))
    private CompoundTag hbm$upgradeNtmData(CompoundTag chunkTag) {
        return dataFixType == DataFixTypes.CHUNK ? HbmDataFixers.upgradeChunk(chunkTag) : chunkTag;
    }
}
