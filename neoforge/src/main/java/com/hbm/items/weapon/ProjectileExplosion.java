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

package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.SlagBlock;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.MultiPointExplosionEntityProcessor;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class ProjectileExplosion {

    static final Supplier<BlockState> CRACKED_SLAG =
            () -> ModBlocks.BLOCK_SLAG.get().defaultBlockState().setValue(SlagBlock.CRACKED, true);

    private ProjectileExplosion() {}

    static Vec3 behindImpact(Entity projectile, HitResult hit) {
        Vec3 motion = projectile.getDeltaMovement();
        return hit.getLocation()
                .subtract(motion.lengthSqr() < 1.0e-8 ? Vec3.ZERO : motion.normalize());
    }

    static void explode(
            Entity projectile,
            HitResult hit,
            float size,
            float rangeMod,
            @Nullable Supplier<BlockState> debris) {
        Vec3 centre = behindImpact(projectile, hit);
        var blast = new ComposableExplosion(projectile.level(), centre.x, centre.y, centre.z, size);
        if (debris != null) {
            blast.blockSelector(new AttenuatedRayBlockSelector(48));
            blast.blockProcessor(
                    CellDestruction.noDrops().transform(new BlockMutatorDebris(debris)));
        }
        blast.entityProcessor(new MultiPointExplosionEntityProcessor(7.5D).range(rangeMod));
        blast.explode();
        projectile.discard();
    }
}
