// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public final class InnerArmorModel {

    public static final OverridePoint<Predicate<ItemStack>> POINT =
            OverridePoint.of(Library.id("inner_armor_model"), stack -> false);

    private InnerArmorModel() {}
}
