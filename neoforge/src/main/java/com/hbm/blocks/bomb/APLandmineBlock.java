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

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

public class APLandmineBlock extends LandmineBlock {
    public static final EnumProperty<Ground> GROUND = EnumProperty.create("ground", Ground.class);

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 1, 11);

    public APLandmineBlock(BlockBehaviour.Properties props) {
        super(props, 1.5D, 1D);
        registerDefaultState(defaultBlockState().setValue(GROUND, Ground.GRASS));
    }

    private static boolean covered(Level level, BlockPos pos) {
        int top =
                Math.min(
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1,
                        level.getMaxY());
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos(pos.getX(), top, pos.getZ());
        for (int y = top; y >= pos.getY() + 2; y--) {
            scan.setY(y);
            if (level.getBlockState(scan).getLightDampening() != 0) return true;
        }
        return false;
    }

    public static Ground groundFor(Level level, BlockPos pos) {
        if (covered(level, pos)) return Ground.STONE;
        Biome biome = level.getBiome(pos).value();
        if (biome.getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.SNOW)
            return Ground.SNOW;
        if (biome.getBaseTemperature() >= 1.5F
                && biome.getModifiedClimateSettings().downfall() <= 0.1F) {
            return Ground.DESERT;
        }
        return Ground.GRASS;
    }

    public static void refreshGround(Level level, BlockPos pos, BlockState state) {
        Ground ground = groundFor(level, pos);
        if (state.getValue(GROUND) == ground) return;
        level.setBlock(pos, state.setValue(GROUND, ground), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GROUND);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_AP_DAMAGE.get().floatValue();
        ComposableExplosion vnt =
                new ComposableExplosion(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3F);
        vnt.entityProcessor(new WeaponExplosionEntityProcessor(0.5, damage).piercing(5F, 0.2F));
        vnt.effects(new SmallExplosionEffect(5, 1F, 0.5F));
        vnt.explode();
    }

    public enum Ground implements StringRepresentable {
        GRASS,
        DESERT,
        SNOW,
        STONE;

        private final String id = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
