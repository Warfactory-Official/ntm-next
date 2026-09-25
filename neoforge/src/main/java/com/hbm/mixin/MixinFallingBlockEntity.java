// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.StoredItems;
import com.hbm.items.special.CarriedBlockItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity implements StoredItems {
    @Shadow public @Nullable CompoundTag blockData;

    @Unique private @Nullable CompoundTag hbm$resolvedData;

    @Unique private @Nullable BlockEntity hbm$carriedInventory;

    @Override
    public void visitStoredItems(Visitor visitor) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (blockData == null
                || !self.getBlockState().hasBlockEntity()
                || !(self.level() instanceof ServerLevel server)) return;
        if (hbm$resolvedData != blockData) {
            hbm$carriedInventory =
                    CarriedBlockItems.load(
                            self.getBlockState(),
                            self.blockPosition(),
                            blockData,
                            server.registryAccess());
            hbm$resolvedData = blockData;
        }
        if (hbm$carriedInventory != null && visitor.visit(hbm$carriedInventory)) {
            blockData = hbm$carriedInventory.saveWithoutMetadata(server.registryAccess());
            hbm$resolvedData = blockData;
        }
    }
}
