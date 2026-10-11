// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

public interface StructureGrounding {
    boolean modifiesGround(Structure structure, Structure.GenerationContext context);

    int[] groundAverages(Structure.GenerationContext context, List<StructurePiece> pieces);
}
