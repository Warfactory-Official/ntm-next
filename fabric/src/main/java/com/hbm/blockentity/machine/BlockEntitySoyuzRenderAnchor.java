// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.LaunchpadSoyuz;
import com.hbm.blocks.machine.LaunchpadSoyuzAnchorCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SoyuzRenderSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlockEntitySoyuzRenderAnchor extends BlockEntity
        implements Synced, SyncUnitSchema, SoyuzRenderSource {
    @SyncField(units = 1L)
    private final float[] positions = new float[8];

    private final float[] previous = new float[8];
    private final float[] targets = new float[8];

    @SyncField(units = 1L)
    private int skin = -1;

    @SyncField(units = 1L)
    private BlockEntityLaunchpadSoyuz.SoyuzStatus status =
            BlockEntityLaunchpadSoyuz.SoyuzStatus.ABSENT;

    @SyncField(units = 1L)
    private int blockLight;

    @SyncField(units = 1L)
    private int skyLight;

    private int turnProgress;
    private volatile boolean selected;
    private boolean reconciled;

    public BlockEntitySoyuzRenderAnchor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_SOYUZ_ANCHOR.get(), pos, state);
    }

    public void copyFrom(BlockEntityLaunchpadSoyuz core, int nextBlock, int nextSky) {
        reconciled = true;
        boolean changed = false;
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] != core.positions[i]) {
                positions[i] = core.positions[i];
                changed = true;
            }
        }
        if (skin != core.loadedType || status != core.soyuzStatus) {
            skin = core.loadedType;
            status = core.soyuzStatus;
            changed = true;
        }
        if (blockLight != nextBlock || skyLight != nextSky) {
            blockLight = nextBlock;
            skyLight = nextSky;
            changed = true;
        }
        if (changed) {
            setChanged();
            networkPackNTTracking();
        }
    }

    public void tickServer() {
        if (reconciled) return;
        var data = SoyuzRenderSavedData.getExisting((ServerLevel) level);
        if (data == null) return;
        var snapshot = data.snapshot(renderCorePos().asLong());
        if (snapshot == null) return;
        copyFrom(snapshot);
        reconciled = true;
    }

    private void copyFrom(SoyuzRenderSavedData.Snapshot snapshot) {
        boolean changed = false;
        for (int i = 0; i < positions.length; i++) {
            float value = snapshot.positions().get(i);
            if (positions[i] != value) {
                positions[i] = value;
                changed = true;
            }
        }
        var nextStatus = BlockEntityLaunchpadSoyuz.SoyuzStatus.values()[snapshot.status()];
        if (skin != snapshot.skin()
                || status != nextStatus
                || blockLight != snapshot.blockLight()
                || skyLight != snapshot.skyLight()) {
            skin = snapshot.skin();
            status = nextStatus;
            blockLight = snapshot.blockLight();
            skyLight = snapshot.skyLight();
            changed = true;
        }
        if (changed) {
            setChanged();
            networkPackNTTracking();
        }
    }

    public void tickClient() {
        for (int i = 0; i < positions.length; i++) {
            previous[i] = positions[i];
            if (turnProgress > 0) positions[i] += (targets[i] - positions[i]) / turnProgress;
            else positions[i] = targets[i];
        }
        if (turnProgress > 0) turnProgress--;
        selected = selectRenderSource();
    }

    private boolean selectRenderSource() {
        BlockPos core = renderCorePos();
        if (loadedEntity(core) instanceof BlockEntityLaunchpadSoyuz) return false;
        int segment = getBlockState().getValue(LaunchpadSoyuzAnchorCell.SEGMENT);
        var facing = getBlockState().getValue(BlockMultiblockCore.FACING);
        for (int earlier = 1; earlier < segment; earlier++) {
            BlockPos at =
                    core.offset(
                            LaunchpadSoyuz.anchorX(earlier, facing),
                            0,
                            LaunchpadSoyuz.anchorZ(earlier, facing));
            if (loadedEntity(at) instanceof BlockEntitySoyuzRenderAnchor) return false;
        }
        return true;
    }

    private BlockEntity loadedEntity(BlockPos pos) {
        var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    @Override
    public BlockPos renderCorePos() {
        return LaunchpadSoyuzAnchorCell.core(worldPosition, getBlockState());
    }

    @Override
    public float renderPosition(int index, float partialTick) {
        return previous[index] + (positions[index] - previous[index]) * partialTick;
    }

    @Override
    public int renderSkin() {
        return skin;
    }

    @Override
    public BlockEntityLaunchpadSoyuz.SoyuzStatus renderStatus() {
        return status;
    }

    @Override
    public boolean renderSelected() {
        return selected;
    }

    public int blockLight() {
        return blockLight;
    }

    public int skyLight() {
        return skyLight;
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        for (float position : positions) output.writeFloat(position);
        output.writeInt(skin);
        output.writeByte(status.ordinal());
        output.writeByte(blockLight);
        output.writeByte(skyLight);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        for (int i = 0; i < targets.length; i++) {
            float value = input.readFloat();
            if (targets[i] != value) {
                targets[i] = value;
                turnProgress = 2;
            }
        }
        skin = input.readInt();
        status = BlockEntityLaunchpadSoyuz.SoyuzStatus.values()[input.readUnsignedByte()];
        blockLight = input.readUnsignedByte();
        skyLight = input.readUnsignedByte();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < positions.length; i++) output.putFloat("p" + i, positions[i]);
        output.putInt("skin", skin);
        output.putInt("status", status.ordinal());
        output.putInt("block_light", blockLight);
        output.putInt("sky_light", skyLight);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < positions.length; i++) positions[i] = input.getFloatOr("p" + i, 0);
        skin = input.getIntOr("skin", -1);
        status = BlockEntityLaunchpadSoyuz.SoyuzStatus.values()[input.getIntOr("status", 0)];
        blockLight = input.getIntOr("block_light", 0);
        skyLight = input.getIntOr("sky_light", 0);
        System.arraycopy(positions, 0, previous, 0, positions.length);
        System.arraycopy(positions, 0, targets, 0, positions.length);
        reconciled = false;
    }
}
