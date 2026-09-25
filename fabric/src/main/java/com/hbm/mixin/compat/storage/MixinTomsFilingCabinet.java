// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.storage;

import com.hbm.interfaces.StoredItems;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.tom.storagemod.block.entity.FilingCabinetBlockEntity", remap = false)
public abstract class MixinTomsFilingCabinet implements StoredItems {
    @Shadow
    public abstract Container getInv();

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visitOwned(getInv());
    }
}
