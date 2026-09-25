// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.storage.ValueOutput;

public interface PersistentDrop {

    void writePersistent(DataComponentMap.Builder components);

    void readPersistent(DataComponentGetter components);

    String[] persistentKeys();

    void stripPersistent(ValueOutput output);
}
