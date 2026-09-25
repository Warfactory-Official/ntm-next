// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.client.model.SectionGeometryIndex;
import com.hbm.client.render.MultiblockOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface IClientLevelExtension extends IClientCoreHint {
    SectionGeometryIndex hbm$sectionGeometryIndex();

    MultiblockOutline hbm$multiblockOutline();

    @Nullable BlockEntity hbm$retainedBlockEntity(BlockPos pos);
}
