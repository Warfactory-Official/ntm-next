// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.packet.SyncField;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityFoundryOutlet extends BlockEntityFoundryBase {

    private boolean redstone;

    public final List<BlockEntityCrucible.PourStream> streams = new ArrayList<>();

    @SyncField(units = 1L << 2)
    public @Nullable NTMMaterial filter = null;

    @SyncField(units = 1L << 3)
    public boolean invertFilter = false;

    @SyncField(units = 1L << 4)
    public boolean invertRedstone = false;

    @SyncField(units = 1L << 5)
    private int pourColor = -1;

    @SyncField(units = 1L << 5)
    private float pourLen;

    public BlockEntityFoundryOutlet(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_OUTLET.get(), pos, state);
    }

    protected BlockEntityFoundryOutlet(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isClosed() {
        return invertRedstone ^ redstone;
    }

    public void refreshRedstone() {
        redstone = level.hasNeighborSignal(worldPosition);
    }

    @Override
    public void tickServer() {

        super.tickServer();
        this.pourColor = -1;
    }

    @Override
    public void tickClient() {
        long now = level.getGameTime();
        for (Iterator<BlockEntityCrucible.PourStream> it = streams.iterator(); it.hasNext(); ) {
            if (now - it.next().birth() >= 20) it.remove();
        }
    }

    protected double dropRange() {
        return 4;
    }

    protected @Nullable BlockHitResult clipDown(Level level, BlockPos pos) {
        BlockHitResult hit =
                CrucibleUtil.traceDown(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() - 0.125,
                        pos.getZ() + 0.5,
                        pos.getY() + 0.125 - dropRange());
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    protected boolean passesGates(Direction side, MaterialStack stack) {
        if (filter != null && (filter != stack.material ^ invertFilter)) return false;
        if (isClosed()) return false;
        return side == getBlockState().getValue(FoundryOutlet.FACING).getOpposite();
    }

    @Override
    public boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack pour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (!passesGates(side, stack)) return false;

        BlockHitResult hit = clipDown(level, pos);
        if (hit == null) return false;
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(level, hit);
        if (acc == null) return false;

        return acc.canAcceptPartialPour(
                level, hit.getBlockPos(), hit.getLocation(), Direction.UP, stack);
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        BlockHitResult hit = clipDown(level, pos);
        if (hit == null) return stack;
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(level, hit);
        if (acc == null) return stack;

        MaterialStack didPour =
                acc.pour(level, hit.getBlockPos(), hit.getLocation(), Direction.UP, stack);

        double hitY = hit.getBlockPos().getY() + 1;
        setPourEvent(
                stack.material.moltenColor,
                Math.max(1F, worldPosition.getY() - (float) (Math.ceil(hitY) - 0.875)));

        return didPour;
    }

    protected void setPourEvent(int color, float len) {
        markSyncEvent();
        this.pourColor = color;
        this.pourLen = len;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    private void writeFilter(ByteBuf output) {
        output.writeInt(filter == null ? -1 : filter.id);
    }

    private void readFilter(ByteBuf input) {
        int id = input.readInt();
        filter = id == -1 ? null : Mats.matById.get(id);
    }

    private void writePour(ByteBuf output) {
        output.writeInt(pourColor);
        output.writeFloat(pourLen);
    }

    private void readPour(ByteBuf input) {
        pourColor = input.readInt();
        pourLen = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 1L << 5) != 0 && level != null && level.isClientSide() && pourColor != -1) {
            streams.add(
                    new BlockEntityCrucible.PourStream(
                            pourColor, false, pourLen, level.getGameTime()));
        }
    }

    @Override
    public void afterInitialSyncUnits() {}

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("invert", this.invertRedstone);
        nbt.putBoolean("invertFilter", this.invertFilter);
        if (filter != null) nbt.putIntArray("matFilter", new int[] {filter.id});
        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (nbt.contains("invert"))
            this.invertRedstone = nbt.getBooleanOr("invert", this.invertRedstone);
        if (nbt.contains("invertFilter"))
            this.invertFilter = nbt.getBooleanOr("invertFilter", this.invertFilter);
        int[] ids = nbt.getIntArray("matFilter").orElse(null);
        if (ids != null && ids.length > 0 && index < ids.length) {
            this.filter = Mats.matById.get(ids[index]);
        }
        setChanged();
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        List<String> info = new ArrayList<>();
        info.add("copytool.invertRedstone");
        info.add("copytool.invertFilter");
        if (filter != null) info.add(filter.getUnlocalizedName());
        return info.toArray(new String[0]);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        redstone = input.getBooleanOr("redstone", false);
        this.invertRedstone = input.getBooleanOr("invert", false);
        this.invertFilter = input.getBooleanOr("invertFilter", false);
        this.filter = Mats.matById.get(input.getIntOr("filter", -1));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("invert", this.invertRedstone);
        output.putBoolean("invertFilter", this.invertFilter);
        output.putBoolean("redstone", redstone);
        output.putInt("filter", this.filter == null ? -1 : this.filter.id);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3cL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 2 -> writeFilter(output);
            case 3 -> output.writeBoolean(this.invertFilter);
            case 4 -> output.writeBoolean(this.invertRedstone);
            case 5 -> writePour(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 2 -> readFilter(input);
            case 3 -> this.invertFilter = input.readBoolean();
            case 4 -> this.invertRedstone = input.readBoolean();
            case 5 -> readPour(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public long syncEventUnits() {
        return super.syncEventUnits() | 1L << 5;
    }
}
