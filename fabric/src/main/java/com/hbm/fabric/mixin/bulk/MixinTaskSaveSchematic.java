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

package com.hbm.fabric.mixin.bulk;

import com.hbm.fabric.integration.bulk.BulkCopyJob;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TaskSaveSchematic.class, remap = false)
public abstract class MixinTaskSaveSchematic extends TaskProcessChunkBase {
    @Unique private final BulkCopyJob hbm$copy = new BulkCopyJob();
    @Shadow @Final protected boolean fromSchematicWorld;

    protected MixinTaskSaveSchematic(String name) {
        super(name);
    }

    @WrapMethod(method = "canProcessChunk")
    private boolean hbm$request(ChunkPos chunk, Operation<Boolean> original) {
        if (!original.call(chunk)) return false;
        return fromSchematicWorld || world instanceof ServerLevel || hbm$copy.ready(chunk);
    }

    @WrapMethod(method = "processChunk")
    private boolean hbm$capture(ChunkPos chunk, Operation<Boolean> original) {
        if (fromSchematicWorld || world instanceof ServerLevel) return original.call(chunk);
        return hbm$copy.capture(chunk, () -> original.call(chunk));
    }

    @WrapMethod(method = "onStop")
    private void hbm$close(Operation<Void> original) {
        try {
            original.call();
        } finally {
            hbm$copy.close();
        }
    }
}
