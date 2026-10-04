// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import com.hbm.inventory.SlotSource;
import java.util.Set;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

public final class AE2MenuInventories {
    private static final Set<String> MENUS =
            Set.of(
                    "appeng.menu.implementations.QuartzKnifeMenu",
                    "appeng.menu.implementations.WirelessAccessPointMenu");
    private static final ClassValue<Boolean> DECLARED =
            new ClassValue<>() {
                @Override
                protected Boolean computeValue(Class<?> type) {
                    return MENUS.contains(type.getName());
                }
            };

    private AE2MenuInventories() {}

    public static @Nullable SlotSource inventory(AbstractContainerMenu menu) {
        if (!DECLARED.get(menu.getClass())) return null;
        var input =
                (AppEngSlot) ((AEBaseMenu) menu).getSlots(SlotSemantics.MACHINE_INPUT).getFirst();
        return (SlotSource) input.getInventory();
    }
}
