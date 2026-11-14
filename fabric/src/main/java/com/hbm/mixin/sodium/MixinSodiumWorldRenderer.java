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

package com.hbm.mixin.sodium;

import com.hbm.client.render.MultiblockCrumbling;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class MixinSodiumWorldRenderer {
    @Shadow private ClientLevel level;

    @ModifyVariable(
            method =
                    "extractBlockEntities(Lnet/minecraft/client/Camera;FLit/unimi/dsi/fastutil/longs/Long2ObjectMap;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> hbm$wholeMachineProgress(
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> progress) {
        MultiblockCrumbling.collect(level);
        return MultiblockCrumbling.owners(level, progress);
    }
}
