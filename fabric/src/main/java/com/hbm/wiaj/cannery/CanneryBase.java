// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.wiaj.JarScript;
import net.minecraft.world.item.ItemStack;

public abstract class CanneryBase {
    public static final int[] colorCopper = {0xFFFDCA88, 0xFFD57C4F, 0xFFAB4223, 0xFF1A1F22};
    public static final int[] colorGold = {0xFFFFFDE0, 0xFFFAD64A, 0xFFDC9613, 0xFF1A1F22};
    public static final int[] colorBlue = {0xFFA5D9FF, 0xFF39ACFF, 0xFF1A6CA7, 0xFF1A1F22};
    public static final int[] colorGrey = {0xFFD1D1D1, 0xFF919191, 0xFF5D5D5D, 0xFF302E36};

    public abstract ItemStack getIcon();

    public abstract String getName();

    public abstract JarScript createScript();

    public CanneryBase[] seeAlso() {
        return new CanneryBase[0];
    }
}
