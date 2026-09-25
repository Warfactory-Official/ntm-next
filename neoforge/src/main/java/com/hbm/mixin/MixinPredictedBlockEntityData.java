// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class MixinPredictedBlockEntityData {

    @WrapOperation(
            method = "handleBlockEntityData",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Ljava/util/Optional;"))
    private Optional<BlockEntity> hbm$retainedEntity(
            ClientLevel level,
            BlockPos pos,
            BlockEntityType<?> type,
            Operation<Optional<BlockEntity>> original) {
        Optional<BlockEntity> current = original.call(level, pos, type);
        if (current.isPresent()) return current;
        BlockEntity retained = level.hbm$retainedBlockEntity(pos);
        return retained != null && retained.getType() == type
                ? Optional.of(retained)
                : Optional.empty();
    }
}
