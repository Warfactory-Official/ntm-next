// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class BlockEntityMachineSatLink extends BlockEntity
        implements Synced, FoldedCoreResident, IRORValueProvider, IRORInteractive, SyncUnitSchema {

    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROT = -15F;
    public static final float ACTIVE_LIFT = -45F;
    public static final float INACTIVE_ROT = 0F;
    public static final float INACTIVE_LIFT = -85F;
    private static final String[] ROR = {
        PREFIX_VALUE + "connected",
        PREFIX_VALUE + "freq",
        PREFIX_VALUE + "rx",
        PREFIX_VALUE + "type",
        PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
        PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"
    };

    @SyncField(units = 1L)
    public boolean connected;

    @SyncField(units = 2L)
    public int freq;

    @SyncField(units = 4L)
    public List<Component> info = List.of();

    public float rot = INACTIVE_ROT;
    public float prevRot = INACTIVE_ROT;
    public float lift = INACTIVE_LIFT;
    public float prevLift = INACTIVE_LIFT;

    public BlockEntityMachineSatLink(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATLINK.get(), pos, state);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityMachineSatLink be) {
        ServerLevel server = (ServerLevel) level;
        SatelliteSavedData data = SatelliteSavedData.get(server);
        be.connected = be.clearColumn(server) && data.isFreqTaken(be.freq);
        Satellite sat = be.connected ? data.getSatFromFreq(be.freq) : null;
        List<Component> nextInfo = sat == null ? List.of() : sat.getInfo(server);
        if (!be.info.equals(nextInfo)) be.info = nextInfo;
        be.networkPackNT(150);
    }

    public static void tickClient(
            Level level, BlockPos pos, BlockState state, BlockEntityMachineSatLink be) {
        be.prevRot = be.rot;
        be.prevLift = be.lift;
        float targetR = be.connected ? ACTIVE_ROT : INACTIVE_ROT;
        float targetL = be.connected ? ACTIVE_LIFT : INACTIVE_LIFT;
        be.rot =
                Math.abs(be.rot - targetR) <= SPEED
                        ? targetR
                        : be.rot + Math.copySign(SPEED, targetR - be.rot);
        be.lift =
                Math.abs(be.lift - targetL) <= SPEED
                        ? targetL
                        : be.lift + Math.copySign(SPEED, targetL - be.lift);
    }

    private boolean clearColumn(ServerLevel level) {

        int top = Math.min(worldPosition.getY() + 6, level.getMaxY());
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        for (int y = worldPosition.getY() + 1; y <= level.getMaxY(); y++) {
            BlockState state = level.getBlockState(cursor.setY(y));
            if (state.getLightDampening() == 0) continue;
            if (y <= top) {
                BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, cursor);
                if (owner != null && owner.pos().equals(worldPosition)) continue;
            }
            return false;
        }
        return true;
    }

    public void setFrequency(int frequency) {
        if (freq == frequency) return;
        freq = frequency;
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if (name.equals(PREFIX_VALUE + "connected")) return connected ? "TRUE" : "FALSE";
        if (name.equals(PREFIX_VALUE + "freq")) return Integer.toString(freq);
        if (name.equals(PREFIX_VALUE + "type") || name.equals(PREFIX_VALUE + "rx")) {
            Satellite sat = SatelliteSavedData.get((ServerLevel) level).getSatFromFreq(freq);
            if (sat == null) return "";
            return name.equals(PREFIX_VALUE + "type") ? sat.type().orbitName() : sat.tx;
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (name.equals(PREFIX_FUNCTION + "setfreq") && params.length == 1) {
            setFrequency(IRORInteractive.parseInt(params[0], 0, 100_000));
        }
        if (name.equals(PREFIX_FUNCTION + "tx")) {
            ServerLevel server = (ServerLevel) level;
            SatelliteSavedData data = SatelliteSavedData.get(server);
            Satellite sat = data.getSatFromFreq(freq);
            String[] command = String.join(PARAM_SEPARATOR, params).split(" ");
            if (sat != null) {
                sat.onCommand(server, command);
                data.setDirty();
            }
            SatelliteRayEvents.report(server, worldPosition, SatelliteRayEvents.RADIO_WAVES, 300);
            setChanged();
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        freq = input.getIntOr("freq", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("freq", freq);
    }

    @Override
    public long syncUnitMask() {
        return 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(connected);
            case 1 -> output.writeInt(freq);
            case 2 -> {
                output.writeInt(info.size());
                for (Component line : info) {
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(output, line);
                }
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> connected = input.readBoolean();
            case 1 -> freq = input.readInt();
            case 2 -> {
                int count = input.readInt();
                if (count < 0 || count > 16)
                    throw new DecoderException("Invalid satellite info count");
                Component[] lines = new Component[count];
                for (int i = 0; i < count; i++) {
                    lines[i] =
                            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(input);
                }
                info = List.of(lines);
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
