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

package com.hbm.mixin;

import com.hbm.interfaces.RigidPistonStructure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class MixinPistonStructureResolver {

    @Shadow @Final private Level level;
    @Shadow @Final private List<BlockPos> toPush;
    @Shadow @Final private Direction pushDirection;

    @Shadow
    private static boolean isSticky(BlockState state) {
        throw new AssertionError();
    }

    @Shadow
    private boolean addBlockLine(BlockPos start, Direction direction) {
        throw new AssertionError();
    }

    @Shadow
    private boolean addBranchingBlocks(BlockPos fromPos) {
        throw new AssertionError();
    }

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void hbm$pullWholeMultiblocks(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        List<BlockPos> expanded = null;

        for (int i = 0; i < this.toPush.size(); i++) {
            BlockPos pos = this.toPush.get(i);
            BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof RigidPistonStructure structure) {
                BlockPos core = structure.rigidStructureCore(this.level, pos);
                if (core == null) {
                    cir.setReturnValue(false);
                    return;
                }
                if (expanded == null) expanded = new ArrayList<>(2);
                if (!expanded.contains(core)) {
                    expanded.add(core);
                    if (!hbm$addWholeStructure(structure, core)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
            if (isSticky(state) && !this.addBranchingBlocks(pos)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private boolean hbm$addWholeStructure(RigidPistonStructure structure, BlockPos core) {
        for (BlockPos block : structure.rigidStructureBlocks(this.level, core)) {
            if (this.toPush.contains(block)) continue;
            if (!structure.isSameMultiblock(this.level.getBlockState(block).getBlock())) continue;
            if (!this.addBlockLine(block, this.pushDirection)) return false;
            if (!this.toPush.contains(block)) return false;
        }
        return true;
    }
}
