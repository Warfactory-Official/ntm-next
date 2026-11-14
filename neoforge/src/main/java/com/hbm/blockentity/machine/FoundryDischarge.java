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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FoundryDischarge extends FoundryBaseBlockEntity implements SettingsHandler {
    public static final int STREAM_LIFETIME = 20;
    private static final long SETTINGS_UNITS = 0x1cL;
    private static final long STREAM_UNIT = 1L << 5;

    public @Nullable Material filter;
    public boolean invertFilter;
    public boolean invertRedstone;
    private boolean powered;
    private int streamColour;

    {
        hbm$syncSet$FoundryDischarge$streamColour(-1);
    }

    private float streamLength;
    public final List<CrucibleBlockEntity.PourStream> streams = new ArrayList<>();

    public FoundryDischarge(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FOUNDRY_OUTLET.get(), pos, state);
    }

    protected FoundryDischarge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        super.onGraphLoad(server);
        updatePower(
                ChunkPublication.signals(server, worldPosition).hasNeighborSignal(worldPosition));
    }

    public void refreshRedstone() {
        if (level instanceof ServerLevel server)
            updatePower(server.hasNeighborSignal(worldPosition));
    }

    private void updatePower(boolean next) {
        if (powered == next) return;
        powered = next;
        setChanged();
    }

    public boolean isClosed() {
        return invertRedstone ^ powered;
    }

    protected final boolean admitsSideAndMaterial(Direction side, Material material) {
        if (isClosed() || side != getBlockState().getValue(FoundryOutletBlock.FACING).getOpposite())
            return false;
        return filter == null || ((filter == material) != invertFilter);
    }

    protected int dropRange() {
        return 4;
    }

    protected final BlockHitResult trace(Level world, BlockPos cell) {
        return CrucibleUtil.traceDown(
                world,
                cell.getX() + 0.5D,
                cell.getY() - 0.125D,
                cell.getZ() + 0.5D,
                cell.getY() + 0.125D - dropRange());
    }

    @Override
    public boolean canAcceptFlow(
            Level world, BlockPos cell, Direction side, MaterialStack offered) {
        if (!admitsSideAndMaterial(side, offered.material)) return false;
        var hit = trace(world, cell);
        var receiver = CrucibleUtil.getPouringTarget(world, hit);
        return receiver != null
                && receiver.canAcceptPour(
                        world, hit.getBlockPos(), hit.getLocation(), Direction.UP, offered);
    }

    @Override
    public @Nullable MaterialStack acceptFlow(
            Level world, BlockPos cell, Direction side, MaterialStack offered) {
        var hit = trace(world, cell);
        var receiver = CrucibleUtil.getPouringTarget(world, hit);
        if (receiver == null) return offered;
        var remainder =
                receiver.acceptPour(
                        world, hit.getBlockPos(), hit.getLocation(), Direction.UP, offered);
        float length =
                Math.max(
                        1F,
                        worldPosition.getY()
                                - (float) (Math.ceil(hit.getBlockPos().getY() + 1D) - 0.875D));
        publishStream(offered.material, length);
        return remainder;
    }

    @Override
    public boolean canAcceptPour(
            Level world, BlockPos cell, Vec3 impact, Direction side, MaterialStack offered) {
        return false;
    }

    @Override
    public MaterialStack acceptPour(
            Level world, BlockPos cell, Vec3 impact, Direction side, MaterialStack offered) {
        return offered;
    }

    protected final void publishStream(Material material, float length) {
        hbm$syncSet$FoundryDischarge$streamColour(material.moltenColor());
        hbm$syncSet$FoundryDischarge$streamLength(length);
        markSyncEvent();
        networkPackNTTracking();
    }

    @Override
    public void tickServer() {
        super.tickServer();
        hbm$syncSet$FoundryDischarge$streamColour(-1);
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        int retained = 0;
        int count = streams.size();
        for (int index = 0; index < count; index++) {
            var stream = streams.get(index);
            if (now - stream.birth() >= STREAM_LIFETIME) continue;
            if (retained != index) streams.set(retained, stream);
            retained++;
        }
        while (streams.size() > retained) streams.removeLast();
    }

    @Override
    public CompoundTag copySettings(Level world, BlockPos cell) {
        var settings = new CompoundTag();
        settings.putBoolean("invert", invertRedstone);
        settings.putBoolean("invertFilter", invertFilter);
        if (filter != null) settings.putIntArray("matFilter", new int[] {filter.id()});
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level world, Player player, BlockPos cell) {
        boolean previousRedstone = invertRedstone;
        boolean previousFilterMode = invertFilter;
        Material previousFilter = filter;
        settings.getBoolean("invert")
                .ifPresent(value -> hbm$syncSet$FoundryDischarge$invertRedstone(value));
        settings.getBoolean("invertFilter")
                .ifPresent(value -> hbm$syncSet$FoundryDischarge$invertFilter(value));
        settings.getIntArray("matFilter")
                .ifPresent(
                        materials -> {
                            if (option >= 0 && option < materials.length)
                                hbm$syncSet$FoundryDischarge$filter(
                                        decodeMaterial(materials[option]));
                        });
        if (previousRedstone != invertRedstone
                || previousFilterMode != invertFilter
                || previousFilter != filter) {
            setChanged();
        }
    }

    @Override
    public String[] getSettingsOptions(Level world, BlockPos cell) {
        if (filter == null)
            return new String[] {"copytool.invertRedstone", "copytool.invertFilter"};
        return new String[] {
            "copytool.invertRedstone", "copytool.invertFilter", filter.translationKey()
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$FoundryDischarge$invertRedstone(input.getBooleanOr("invert", false));
        hbm$syncSet$FoundryDischarge$invertFilter(input.getBooleanOr("invertFilter", false));
        powered = input.getBooleanOr("redstone", false);
        hbm$syncSet$FoundryDischarge$filter(decodeMaterial(input.getIntOr("filter", -1)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("invert", invertRedstone);
        output.putBoolean("invertFilter", invertFilter);
        output.putBoolean("redstone", powered);
        output.putInt("filter", filter == null ? -1 : filter.id());
    }

    private static @Nullable Material decodeMaterial(int id) {
        if (id == -1) return null;
        var material = MaterialRegistry.byId(id);
        if (material == null) throw new IllegalArgumentException("Unknown foundry material: " + id);
        return material;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3cL;
    }

    @Override
    public long syncEventUnits() {
        return super.syncEventUnits() | STREAM_UNIT;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> output.writeInt(filter == null ? -1 : filter.id());
            case 3 -> output.writeBoolean(invertFilter);
            case 4 -> output.writeBoolean(invertRedstone);
            case 5 -> {
                output.writeInt(streamColour);
                output.writeFloat(streamLength);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> hbm$syncSet$FoundryDischarge$filter(decodeMaterial(input.readInt()));
            case 3 -> hbm$syncSet$FoundryDischarge$invertFilter(input.readBoolean());
            case 4 -> hbm$syncSet$FoundryDischarge$invertRedstone(input.readBoolean());
            case 5 -> {
                hbm$syncSet$FoundryDischarge$streamColour(input.readInt());
                hbm$syncSet$FoundryDischarge$streamLength(input.readFloat());
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & SETTINGS_UNITS) != 0) rebuildPlates();
        if ((units & STREAM_UNIT) != 0 && streamColour != -1) {
            streams.add(
                    new CrucibleBlockEntity.PourStream(
                            streamColour, false, streamLength, level.getGameTime()));
        }
    }

    @Override
    public void afterInitialSyncUnits() {
        rebuildPlates();
    }

    @Override
    public boolean afterRelayedSyncUnits(long units, boolean initial) {
        if (initial) afterInitialSyncUnits();
        else afterSyncUnits(units);
        return false;
    }

    private void rebuildPlates() {
        if (!level.isClientSide()) return;
        var state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }

    public final void hbm$syncSet$FoundryDischarge$filter(Material next) {
        if (this.filter != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.filter);
            this.filter = next;
            if (bound) bindSyncValue(next, 3, 1L << 2);
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$FoundryDischarge$invertFilter(boolean next) {
        if (this.invertFilter != next) {
            this.invertFilter = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$FoundryDischarge$invertRedstone(boolean next) {
        if (this.invertRedstone != next) {
            this.invertRedstone = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$FoundryDischarge$streamColour(int next) {
        if (this.streamColour != next) {
            this.streamColour = next;
            if ((STREAM_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, STREAM_UNIT);
        }
    }

    public final void hbm$syncSet$FoundryDischarge$streamLength(float next) {
        if (Float.floatToRawIntBits(this.streamLength) != Float.floatToRawIntBits(next)) {
            this.streamLength = next;
            if ((STREAM_UNIT) == 0L) syncChanged(3);
            else syncUnitsChanged(3, STREAM_UNIT);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(FoundryDischarge.class, "filter", 3, 1L << 2);
    }
}
