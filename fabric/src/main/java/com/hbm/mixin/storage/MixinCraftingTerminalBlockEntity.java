// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.interfaces.StoredItems;
import com.hbm.inventory.TrackedStorage;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import java.util.function.Consumer;
import net.minecraft.world.inventory.CraftingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingTerminalBlockEntity.class, remap = false)
public abstract class MixinCraftingTerminalBlockEntity implements StoredItems, TrackedStorage {
    @Shadow
    public abstract CraftingContainer getCraftingInv();

    @Override
    public void inventorySources(Consumer<Object> sources) {
        sources.accept(getCraftingInv());
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        visitor.visitOwned(getCraftingInv());
    }
}
