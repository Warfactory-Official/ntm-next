// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.tool.ItemRebarPlacer;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.uninos.networkproviders.RebarGraph;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRebar extends BlockEntity
        implements Synced, GraphResident, IFluidHandlerMK2, SyncUnitSchema {

    public static final int FULL = 1_000;
    private static final int MAX_SPEED = 50;

    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static BlockEntityRebar[] members = new BlockEntityRebar[16];

    private @Nullable Block concrete;

    @SyncField(units = 1L)
    private int progress;

    public BlockEntityRebar(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REBAR.get(), pos, state);
    }

    public void setConcrete(Block concrete) {
        this.concrete = concrete;
        setChanged();
    }

    public int progress() {
        return progress;
    }

    public BlockState solidState() {
        return concrete != null && ItemRebarPlacer.isValidConcrete(concrete)
                ? concrete.defaultBlockState()
                : ModBlocks.CONCRETE_REBAR.get().defaultBlockState();
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return type == NTMFluids.LIQUID_CONCRETE && pressure == 0 ? 10_000L : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (type != NTMFluids.LIQUID_CONCRETE || !(level instanceof ServerLevel server))
            return amount;
        NodeNetwork<Unit> net = RebarGraph.get(server).networkAt(worldPosition.asLong());
        if (net == null) return amount;

        long[] layer = RebarGraph.lowestLayer(net);
        if (members.length < layer.length) members = new BlockEntityRebar[layer.length];
        int count = 0;
        long filled = 0;
        for (long cell : layer) {
            BlockEntityRebar member =
                    ChunkUtil.blockEntityIfLoaded(BlockEntityRebar.class, server, CURSOR.set(cell));
            if (member == null) continue;
            filled += member.progress;
            members[count++] = member;
        }
        if (count == 0) return amount;

        long accept =
                Math.min(Math.min(count * (long) FULL - filled, amount), MAX_SPEED * (long) count);
        int target = (int) Math.min((filled + accept) / count, FULL);
        for (int i = 0; i < count; i++) {
            BlockEntityRebar member = members[i];
            members[i] = null;
            if (member.progress >= target) continue;
            int delta = target - member.progress;
            if (delta > amount) continue;
            member.fill(server, target);
            amount -= delta;
        }
        return amount;
    }

    private void fill(ServerLevel server, int progress) {
        this.progress = progress;
        markChanged();
        networkPackNTTracking();
        if (progress >= FULL) server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        FluidTankNTM tank = new FluidTankNTM(NTMFluids.LIQUID_CONCRETE, FULL);
        tank.setFill(progress);
        return new FluidTankNTM[] {tank};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        int old = progress;
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        concrete = input.read("block", BuiltInRegistries.BLOCK.byNameCodec()).orElse(null);
        refreshFillMesh(old);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        if (concrete != null)
            output.store("block", BuiltInRegistries.BLOCK.byNameCodec(), concrete);
    }

    private void refreshFillMesh(int old) {
        if (old == progress || level == null || !level.isClientSide()) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(progress);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                int old = progress;
                progress = input.readInt();
                refreshFillMesh(old);
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
