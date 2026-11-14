/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.capability.ForeignBridges;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.injected.BulkParticipant;
import com.hbm.interfaces.injected.ForeignBridgeHost;
import com.hbm.inventory.BlockItemContents;
import com.hbm.inventory.InventoryChanges;
import com.hbm.items.ModDataComponents;
import com.hbm.world.bulk.BulkRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements ForeignBridgeHost, BulkParticipant {
    @Unique private int hbm$bulkKind;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbm$bindBulk(CallbackInfo ignored) {
        hbm$bulkKind = BulkRegistry.bind(getClass());
    }

    @Override
    public int hbm$bulkKind() {
        return hbm$bulkKind;
    }

    @Unique public Object hbm$inventorySources;
    @Unique private @Nullable ForeignBridges hbm$bridges;

    @Override
    public ForeignBridges hbm$bridges() {
        ForeignBridges bridges = hbm$bridges;
        if (bridges == null) hbm$bridges = bridges = new ForeignBridges();
        return bridges;
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"))
    private void hbm$collectPackedInventory(DataComponentMap.Builder components, CallbackInfo ci) {
        BlockItemContents contents = BlockItemContents.capture((BlockEntity) (Object) this);
        if (contents != null) components.set(ModDataComponents.BLOCK_INVENTORY.get(), contents);
    }

    @Inject(method = "removeComponentsFromTag", at = @At("TAIL"))
    private void hbm$stripPackedInventory(ValueOutput output, CallbackInfo ci) {
        BlockItemContents.strip((BlockEntity) (Object) this, output);
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void hbm$restorePackedInventory(DataComponentGetter components, CallbackInfo ci) {
        BlockItemContents.apply((BlockEntity) (Object) this, components);
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void hbm$retire(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)) return;
        self.getBlockState().getBlock().hbm$blockEntityRemoved(level, self);
        if (UnstableFuses.isStorageOwner(self)) UnstableFuses.removed(level, self);
    }

    @Inject(
            method =
                    "setChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"))
    private static void hbm$storageChanged(
            Level world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(world instanceof ServerLevel level)) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        BlockEntity self = chunk.getBlockEntities().get(pos);
        if (self == null) return;
        if (UnstableFuses.isStorageOwner(self)) InventoryChanges.blockChanged(self);
    }
}
