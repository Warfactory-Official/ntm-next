// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityFusionKlystronCreative extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, AudioLoop, SyncUnitSchema {

    public static final float FAN_ACCELERATION = 0.125F;
    public static Consumer<BlockEntityFusionKlystronCreative> CLIENT_SOUND = be -> {};
    public float fan;
    public float prevFan;
    public float fanSpeed;

    @SyncField(units = 1L << 0)
    public boolean isConnected = false;

    public BlockEntityFusionKlystronCreative(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON_CREATIVE.get(), pos, state);
    }

    public void tickServer() {
        ServerLevel serverLevel = (ServerLevel) level;
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        FusionPorts.Port link = BlockEntityFusionKlystron.links(worldPosition, facing).get(0);
        isConnected =
                BlockEntityFusionKlystron.provideKyU(
                        serverLevel, link, FusionRecipes.INSTANCE.maxInput());

        networkPackNT(100);
    }

    public void tickClient() {
        CLIENT_SOUND.accept(this);

        if (isConnected) fanSpeed += FAN_ACCELERATION;
        else fanSpeed -= FAN_ACCELERATION;

        fanSpeed = Mth.clamp(fanSpeed, 0F, 5F);

        prevFan = fan;
        fan += fanSpeed;

        if (fan >= 360F) {
            fan -= 360F;
            prevFan -= 360F;
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = fanSpeed / 5F;
        return AudioSystem.getLoopedSound(
                ModSounds.FEL_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 2.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                15F,
                speed,
                20);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isConnected);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isConnected = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
