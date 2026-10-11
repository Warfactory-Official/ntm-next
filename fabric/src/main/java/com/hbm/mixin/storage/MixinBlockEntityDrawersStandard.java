// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.storage;

import com.hbm.inventory.TrackedStorage;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockEntityDrawersStandard.class, remap = false)
public abstract class MixinBlockEntityDrawersStandard implements TrackedStorage {
    @Override
    public void inventorySources(Consumer<Object> sources) {
        Object group = __asm__(Object) {
            aload this;
            checkcast "com/jaquadro/minecraft/storagedrawers/block/tile/BlockEntityDrawersStandard";
            invokevirtual "com/jaquadro/minecraft/storagedrawers/block/tile/BlockEntityDrawersStandard" "getGroup" "()Lcom/jaquadro/minecraft/storagedrawers/api/storage/IDrawerGroup;";
        };
        int count = __asm__(int) {
            aload group;
            checkcast "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawerGroup";
            invokeinterface "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawerGroup" "getDrawerCount" "()I";
        };
        for (int slot = 0; slot < count; slot++) {
            Object drawer = __asm__(Object) {
                aload group;
                checkcast "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawerGroup";
                iload slot;
                invokeinterface "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawerGroup" "getDrawer" "(I)Lcom/jaquadro/minecraft/storagedrawers/api/storage/IDrawer;";
            };
            sources.accept(drawer);
        }
        Object upgrades = __asm__(Object) {
            aload this;
            checkcast "com/jaquadro/minecraft/storagedrawers/block/tile/BlockEntityDrawers";
            invokevirtual "com/jaquadro/minecraft/storagedrawers/block/tile/BlockEntityDrawers" "upgrades" "()Lcom/jaquadro/minecraft/storagedrawers/block/tile/tiledata/UpgradeData;";
        };
        sources.accept(upgrades);
    }
}
