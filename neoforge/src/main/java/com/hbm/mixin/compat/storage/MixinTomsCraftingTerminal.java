// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.storage;

import com.hbm.interfaces.StoredItems;
import net.minecraft.world.inventory.CraftingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.tom.storagemod.block.entity.CraftingTerminalBlockEntity", remap = false)
public abstract class MixinTomsCraftingTerminal implements StoredItems {
    @Shadow
    public abstract CraftingContainer getCraftingInv();

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visitOwned(getCraftingInv());
    }
}
