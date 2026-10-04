// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.TrackedStorage;
import com.hbm.neoforge.inventory.AE2Inventories;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "appeng.blockentity.AEBaseBlockEntity", remap = false)
public abstract class MixinAEBaseBlockEntity implements TrackedStorage {
    @Override
    public void inventorySources(Consumer<Object> sources) {
        AE2Inventories.sources((BlockEntity) (Object) this, sources);
    }
}
