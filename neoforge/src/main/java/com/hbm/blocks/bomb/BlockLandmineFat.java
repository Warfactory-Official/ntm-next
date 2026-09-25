// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.main.Polaroid;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockLandmineFat extends BlockLandmine {

    private static final VoxelShape SHAPE = Block.box(5, 0, 4, 11, 6, 12);

    public BlockLandmineFat(BlockBehaviour.Properties props) {
        super(props, 2.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        float damage = ExplosionData.MINE_NUKE_DAMAGE.get().floatValue();
        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, 10);
        vnt.setBlockAllocator(new BlockAllocatorStandard(64));
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();

        XFactoryCatapult.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), 1.5F);
        if (level instanceof ServerLevel server) {
            SatelliteDetector.reportEvent(
                    server,
                    SatelliteDetector.DURATION_LOW,
                    SatelliteDetector.BurstIntensity.LOW,
                    x,
                    z);

            Services.NETWORK.sendToAllAround(
                    new MukePayload(
                            x,
                            y,
                            z,
                            false,
                            Polaroid.isBalefireDay() || level.getRandom().nextInt(100) == 0),
                    new TargetPoint(server, x, y, z, 250));
        }
        level.playSound(
                null,
                x,
                y,
                z,
                ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                SoundSource.BLOCKS,
                25.0F,
                0.9F);
    }
}
