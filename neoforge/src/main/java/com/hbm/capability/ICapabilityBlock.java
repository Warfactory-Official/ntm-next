// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.registration.RegistryHandle;
import net.minecraft.world.level.block.Block;

public interface ICapabilityBlock {

    MachineCaps caps();

    default void declareExtraCaps(RegistryHandle<? extends Block> self) {}
}
