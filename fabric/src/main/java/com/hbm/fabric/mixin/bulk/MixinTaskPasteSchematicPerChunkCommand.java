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

import com.hbm.fabric.integration.bulk.BulkPasteQueue;
import com.hbm.world.bulk.BulkAction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PasteNbtBehavior;
import fi.dy.masa.malilib.util.position.LayerRange;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TaskPasteSchematicPerChunkCommand.class, remap = false)
public abstract class MixinTaskPasteSchematicPerChunkCommand
        extends TaskPasteSchematicPerChunkBase {
    @Unique private final BulkPasteQueue hbm$bulk = new BulkPasteQueue();
    @Shadow @Final protected PasteNbtBehavior nbtBehavior;

    protected MixinTaskPasteSchematicPerChunkCommand(
            Collection<SchematicPlacement> placements, LayerRange range, boolean changedOnly) {
        super(placements, range, changedOnly);
    }

    @Shadow
    protected abstract boolean shouldSetBlock(BlockState schematic, BlockState existing);

    @WrapMethod(method = "execute")
    private boolean hbm$flush(ProfilerFiller profiler, Operation<Boolean> original) {
        boolean done = original.call(profiler);
        hbm$bulk.flush(done);
        return done;
    }

    @WrapMethod(method = "onStop")
    private void hbm$close(Operation<Void> original) {
        try {
            original.call();
        } finally {
            hbm$bulk.cancel();
        }
    }

    @WrapMethod(method = "pasteBlock")
    private void hbm$paste(
            BlockPos pos,
            LevelChunk schematic,
            ChunkAccess existing,
            boolean ignoreLimit,
            Operation<Void> original) {
        BlockState state = schematic.getBlockState(pos), old = existing.getBlockState(pos);
        if (!BulkAction.owns(state) && !BulkAction.owns(old)) {
            original.call(pos, schematic, existing, ignoreLimit);
            return;
        }
        if (shouldSetBlock(state, old))
            hbm$bulk.add(
                    schematicWorld, schematic, pos, state, nbtBehavior != PasteNbtBehavior.NONE);
    }

    @WrapMethod(method = "queueFillCommandForBox")
    private void hbm$fill(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState state,
            Operation<Void> original) {
        if (!BulkAction.owns(state)) {
            original.call(minX, minY, minZ, maxX, maxY, maxZ, state);
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    hbm$bulk.add(
                            schematicWorld,
                            schematicWorld.getChunk(x >> 4, z >> 4),
                            pos.set(x, y, z),
                            state,
                            nbtBehavior != PasteNbtBehavior.NONE);
    }
}
