// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import net.minecraft.world.item.ItemStack;

public interface IPARanged {

    void clickPrimary(ItemStack stack, LambdaContext ctx);

    void clickSecondary(ItemStack stack, LambdaContext ctx);
}
