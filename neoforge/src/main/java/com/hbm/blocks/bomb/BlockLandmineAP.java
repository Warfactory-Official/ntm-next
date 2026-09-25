// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.platform.Services;
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

public class BlockLandmineAP extends BlockLandmine {

    public static final EnumProperty<Ground> GROUND = EnumProperty.create("ground", Ground.class);

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 1, 11);

    public BlockLandmineAP(BlockBehaviour.Properties props) {
        super(props, 1.5D, 1D);
        registerDefaultState(defaultBlockState().setValue(GROUND, Ground.GRASS));
    }

    private static boolean covered(Level level, BlockPos pos) {
        int top =
                Math.min(
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1,
                        level.getMaxY() - 1);
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
        if (biome.getBaseTemperature() >= 1.5F && Services.PLATFORM.biomeDownfall(biome) <= 0.1F) {
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
        ExplosionVNT vnt =
                new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3F);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage).setupPiercing(5F, 0.2F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
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
