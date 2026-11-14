/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.fabric.integration.bulk;

import com.hbm.world.bulk.BulkSnapshot;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import java.io.ByteArrayOutputStream;
import net.minecraft.core.BlockPos;

public final class AxiomBulkData {
    public static final short HEADER = -1;

    private AxiomBulkData() {}

    public static CompressedBlockEntity transform(CompressedBlockEntity data, IntMatrix matrix) {
        var tag = data.decompress();
        if (!tag.contains(BulkSnapshot.KEY)) return data;
        return CompressedBlockEntity.compress(
                BulkSnapshot.transform(
                        tag,
                        p ->
                                new BlockPos(
                                        matrix.transformX(p.getX(), p.getY(), p.getZ()),
                                        matrix.transformY(p.getX(), p.getY(), p.getZ()),
                                        matrix.transformZ(p.getX(), p.getY(), p.getZ()))),
                new ByteArrayOutputStream());
    }
}
