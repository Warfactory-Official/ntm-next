// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.TrackedStorage;
import com.tom.storagemod.block.entity.FilingCabinetBlockEntity;
import java.util.function.Consumer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FilingCabinetBlockEntity.class, remap = false)
public abstract class MixinFilingCabinetBlockEntity implements StoredItems, TrackedStorage {
    @Shadow
    public abstract Container getInv();

    @Override
    public void inventorySources(Consumer<Object> sources) {
        sources.accept(getInv());
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visitOwned(getInv());
    }
}
