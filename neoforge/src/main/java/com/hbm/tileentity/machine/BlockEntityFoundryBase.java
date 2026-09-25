// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityFoundryBase extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                ICrucibleAcceptor,
                ICopiable,
                SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public @Nullable NTMMaterial type;

    @SyncField(units = 1L << 1)
    public int amount;

    protected BlockEntityFoundryBase(BlockEntityType<?> be, BlockPos pos, BlockState state) {
        super(be, pos, state);
    }

    public void tickServer() {

        networkPackNTTracking();
    }

    public void tickClient() {}

    private void writeType(ByteBuf output) {
        output.writeInt(type == null ? -1 : type.id);
    }

    private void readType(ByteBuf input) {
        int id = input.readInt();
        type = id == -1 ? null : Mats.matById.get(id);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        if (level != null && !level.isClientSide() && this.amount > 0 && this.type != null) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            ItemScraps.create(new MaterialStack(this.type, this.amount))));
            this.amount = 0;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.type = Mats.matById.get(input.getIntOr("type", -1));
        this.amount = input.getIntOr("amount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("type", this.type == null ? -1 : this.type.id);
        output.putInt("amount", this.amount);
    }

    public abstract int getCapacity();

    public boolean standardCheck(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (this.type != null && this.type != stack.material && this.amount > 0) return false;
        return this.amount < this.getCapacity();
    }

    public @Nullable MaterialStack standardAdd(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.material;

        if (stack.amount + this.amount <= this.getCapacity()) {
            this.amount += stack.amount;
            return null;
        }

        int required = this.getCapacity() - this.amount;
        this.amount = this.getCapacity();

        stack.amount -= required;

        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return this.standardCheck(level, pos, side, stack);
    }

    @Override
    public MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return standardAdd(level, pos, side, stack);
    }

    @Override
    public boolean canAcceptPartialPour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        return this.standardCheck(level, pos, side, stack);
    }

    @Override
    public MaterialStack pour(
            Level level, BlockPos pos, Vec3 hit, Direction side, MaterialStack stack) {
        return standardAdd(level, pos, side, stack);
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (type != null) nbt.putIntArray("matFilter", new int[] {type.id});
        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {}

    private @Nullable BlockLookupCache<ICrucibleAcceptor>[] acceptorCache;

    @SuppressWarnings("unchecked")
    protected @Nullable ICrucibleAcceptor acceptorAt(Direction dir, BlockPos target) {
        if (!(level instanceof ServerLevel server))
            return NtmContracts.CRUCIBLE_ACCEPTOR.at(level, target);
        if (acceptorCache == null) acceptorCache = new BlockLookupCache[6];
        int i = dir.get3DDataValue();
        if (acceptorCache[i] == null)
            acceptorCache[i] = NtmContracts.CRUCIBLE_ACCEPTOR.cacheAt(server, target);
        return acceptorCache[i].find();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeType(output);
            case 1 -> output.writeInt(this.amount);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readType(input);
            case 1 -> this.amount = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
